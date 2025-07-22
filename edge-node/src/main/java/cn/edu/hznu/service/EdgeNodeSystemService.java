package cn.edu.hznu.service;

import cn.edu.hznu.bean.*;
import cn.edu.hznu.queue.ExecutionQueue;
import cn.edu.hznu.queue.TransmissionQueue;
import cn.edu.hznu.scheduler.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

@Setter
@Getter
@Service
@Slf4j
@RefreshScope
public class EdgeNodeSystemService {

    @Value("${spring.application.name}")
    private String name;

    @Value("${edgeComputing.seed}")
    private Integer seed;

    @Value("${edgeComputing.cpuCapacity}")
    private Integer cpuCapacity;

    // refresh
    @Value("${edgeComputing.scheduler}")
    private String scheduler;

    @Value("${edgeComputing.minCpuCore}")
    private int minCpuCore;

    @Value("${edgeComputing.maxTaskSize}")
    private int maxTaskSize;

    @Value("${edgeComputing.maxTaskComplexity}")
    private int maxTaskComplexity;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Lazy
    @Autowired
    private DTTOScheduler DTTOScheduler;

    @Lazy
    @Autowired
    private SACScheduler SACScheduler;

    @Lazy
    @Autowired
    private DDQNScheduler DDQNScheduler;

    @Lazy
    @Autowired
    private BRMTOScheduler BRMTOScheduler;

    @Lazy
    @Autowired
    private LOCALScheduler LOCALScheduler;

    private boolean training = true;

    private boolean collecting = false;

    private boolean startLearn = false;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private LinkService linkService;

    @Value("${edgeComputing.queueCoef}")
    private float queueCoef;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int agentNumber;

    private List<String> maliciousList;

    @Resource
    BlockchainService blockchainService;

    @Autowired
    private TaskService taskService;


    @Async
    public void processUserTask(Task task) throws Exception {
        String service = switch (scheduler) {
            case "DTTO" -> DTTOScheduler.selectAction(task.getId());
            case "BRMTO" -> BRMTOScheduler.selectAction(task.getId());
            case "LOCAL" -> LOCALScheduler.selectAction(task.getId());
            case "SAC" -> SACScheduler.selectAction(task.getId());
            case "DDQN" -> DDQNScheduler.selectAction(task.getId());
            default -> throw new RuntimeException("no scheduler");
        };
        if (service.equals(name)) { // local process
            // 直接处理任务，不需要传输。
            task.setBeginExecutionTime(LocalDateTime.now());
            task.setEndTransmissionTime(LocalDateTime.now());
            task.setTransmissionTime(0L);
            task.setTransmissionWaitingTime(0L);
            task.setDestination(name);
            processEdgeNodeTask(task);
        } else if (service.equals("edge-node-" + (agentNumber + 1))) {
            // empty
            task.setExecutionTime(0L);
            task.setExecutionWaitingTime(0L);
            task.setTransmissionTime(0L);
            task.setTransmissionWaitingTime(0L);
        } else { // other node process
            Link link = edgeNodeSystem.getLinkMap().get(service);
            Double transmissionTime = task.getTaskSize() / link.getTransmissionRate() * 1000;
            task.setTransmissionTime(transmissionTime.longValue());
            task.setDestination(service);
            edgeNodeSystem.getTransmissionQueueMap().get(service).add(task);
        }
    }

    public void processEdgeNodeTask(Task task) throws Exception {
        if ((!Objects.equals(task.getSource(), task.getDestination())) && (maliciousList.contains(task.getDestination()))) { // 恶意节点从别的节点接收到任务
            Random random = new Random();
            if (random.nextDouble() < 1.1) {
                task.setStatus(TaskStatus.MALICIOUS_FAILURE);
                task.setExecutionWaitingTime(0L);
                task.setTransmissionTime(0L);
                task.setTransmissionWaitingTime(0L);
                BigInteger Wd = BigInteger.valueOf(100 * (task.getCpuCycle() / StoreConstants.Kilo.value / StoreConstants.Byte.value) / ((long) maxTaskSize * maxTaskComplexity));
                taskService.updateById(task);
                String address = blockchainService.getAddress();
                String url = String.format("http://%s/cmd/doMaliciousBehavior?address=" + address + "&Wd=" + Wd, task.getSource());
                restTemplate.getForObject(url, String.class);
                return;
            }
        }
        Double executionTime = task.getCpuCycle().doubleValue() / edgeNodeSystem.getEdgeNode().getCapacity().doubleValue() * 1000;
        task.setExecutionTime(executionTime.longValue());
        edgeNodeSystem.getExecutionQueue().add(task);
    }

    public void init() {
        // 初始化边缘节点配置、任务队列、链路信息等
        var edgeNodeConfig = edgeNodeService.getOne(new QueryWrapper<EdgeNode>().eq("name", name));
        var id = Integer.parseInt(name.split("-")[2]);
        EdgeNode edgeNode = new EdgeNode();
        edgeNode.setId(id);
        edgeNode.setName(name);
        edgeNode.setCpuNum(edgeNodeConfig.getCpuNum());
        edgeNode.setExecutionFailureRate(edgeNodeConfig.getExecutionFailureRate());
        edgeNode.setTaskRate(edgeNodeConfig.getTaskRate());
        edgeNode.setCapacity(edgeNodeConfig.getCpuNum() * Constants.Giga.value * cpuCapacity);
        edgeNodeSystem.setEdgeNode(edgeNode);
        edgeNodeSystem.setExecutionQueue(new ExecutionQueue());

        var transmissionQueueMap = new HashMap<String, TransmissionQueue>();
        var linkMap = new HashMap<String, Link>();
        var links = linkService.list(new QueryWrapper<Link>().eq("source", name));
        for (Link link : links) {
            transmissionQueueMap.put(link.getDestination(), new TransmissionQueue());
            linkMap.put(link.getDestination(), link);
        }
        edgeNodeSystem.setTransmissionQueueMap(transmissionQueueMap);
        edgeNodeSystem.setLinkMap(linkMap);
        // availAction
        float threshold = (float) Math.floor((float) edgeNode.getCpuNum() / minCpuCore * queueCoef);
        edgeNodeSystem.setExecutionQueueThreshold(threshold);
        log.info("load edge nodes and links configuration completed");
        log.info("{} edge config: {}", name, edgeNode);
        log.info("{} link config：{}", name, links);
    }

}
