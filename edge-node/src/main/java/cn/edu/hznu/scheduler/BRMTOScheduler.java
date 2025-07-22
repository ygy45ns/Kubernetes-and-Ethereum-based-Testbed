package cn.edu.hznu.scheduler;

import cn.edu.hznu.bean.EdgeNodeSystem;
import cn.edu.hznu.bean.TaskStatus;
import cn.edu.hznu.service.BlockchainService;
import cn.edu.hznu.service.TaskService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.*;

@Lazy
@Component
@Setter
@Slf4j
public class BRMTOScheduler implements IScheduler {
    @Value("${spring.application.name}")
    public String name;

    @Autowired
    private TaskService taskService;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int edgeNodeNumber;

    @Resource
    BlockchainService blockchainService;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Value("${edgeComputing.k}")
    private int k;

    @Autowired
    private RestTemplate restTemplate;


    @Override
    public String selectAction(Long taskId) throws Exception {
        String obj;
        if (taskService.getById(taskId).getStatus().equals(TaskStatus.EMPTY)) {
            obj = "edge-node-" + (edgeNodeNumber + 1);
            log.info("select " + obj);
            return obj;
        }
        // 本地处理优先
        if ((edgeNodeSystem.getExecutionQueue().getSize() + edgeNodeSystem.getExecutionQueue().getActiveSize()) <= edgeNodeSystem.getExecutionQueueThreshold()) {
            log.info("select " + name);
            return name;
        }
        // 候选节点列表
        List<BigInteger> reputationList = blockchainService.callGetReputations();
        float[] reputations = new float[reputationList.size()];
        for (int i = 0; i < reputationList.size(); i++) {
            reputations[i] = reputationList.get(i).floatValue() / 100f;
        }
        System.out.println("reputations:" + reputationList);
        List<Integer> sampledIndices = getRandomSample(edgeNodeNumber, edgeNodeNumber / 2);
        List<Integer> topIndices = findTopKIndices(reputations, k, sampledIndices);
        System.out.println("top k index:" + topIndices);

        var queue = new PriorityQueue<>(Comparator.comparingInt((Map<String, Object> o) -> (int) o.get("queue")));

        for (Integer topIndex : topIndices) {
            String edgeName = String.format("edge-node-%d", topIndex + 1);
            var info = new HashMap<String, Object>();
            var url = String.format("http://%s/edgeNode/queue", edgeName);
            var queueSize = restTemplate.getForObject(url, Integer.class);
            info.put("queue", queueSize);
            info.put("edgeName", edgeName);
            queue.add(info);
        }
        obj = (String) Objects.requireNonNull(queue.poll()).get("edgeName");
        log.info("select " + obj);
        return obj;
    }

    public List<Integer> findTopKIndices(float[] reputations, int k, List<Integer> candidates) {
        candidates.sort((i1, i2) -> Float.compare(reputations[i2], reputations[i1]));
        return candidates.subList(0, Math.min(k, candidates.size()));
    }


    public List<Integer> getRandomSample(int total, int sampleSize) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < total; i++) indices.add(i);
        Collections.shuffle(indices);
        return indices.subList(0, Math.min(sampleSize, total));
    }
}