package cn.edu.hznu.scheduler;


import cn.edu.hznu.agent.Agent;
import cn.edu.hznu.bean.EdgeNodeSystem;
import cn.edu.hznu.bean.Task;
import cn.edu.hznu.bean.TaskStatus;
import cn.edu.hznu.service.EdgeNodeSystemService;
import cn.edu.hznu.service.TaskService;
import cn.edu.hznu.service.TransitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import cn.edu.hznu.bean.RatcVo;

import javax.annotation.Resource;
import java.util.*;

import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RefreshScope
@Lazy
public class SACScheduler implements IScheduler {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private Agent agent;

    @Autowired
    private TransitionService transitionService;

    @Autowired
    private TaskService taskService;

    @Value("${spring.application.name}")
    public String name;

    @Resource
    private EdgeNodeSystemService edgeNodeSystemService;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int edgeNodeNumber;

    @Autowired
    private Random schedulerRandom;
    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Override
    public String selectAction(Long taskId) {

        Task task = taskService.getById(taskId);
        var state = transitionService.getState(taskId, edgeNodeSystemService.isTraining());

        if (edgeNodeSystemService.isCollecting()) {
            if (task.getStatus() == TaskStatus.EMPTY) {
                log.info("select edge-node-{}", (edgeNodeNumber + 1));
                return String.format("edge-node-%d", (edgeNodeNumber + 1));
            }
            var edgeNodeIds = new ArrayList<Integer>();
            for (int i = 1; i <= edgeNodeNumber; i++) {
                edgeNodeIds.add(i);
            }
            var selectedNodes = new HashSet<Integer>();
            while (selectedNodes.size() < 2) {
                selectedNodes.add(edgeNodeIds.get(schedulerRandom.nextInt(edgeNodeIds.size())));
            }

            var selectEdgeNodeInfo = new ArrayList<RatcVo>();
            for (Integer edgeNodeId : selectedNodes) {
                var url1 = String.format("http://edge-node-%s/edgeNode/ratc", edgeNodeId);
                var ratcVo = restTemplate.getForObject(url1, RatcVo.class);
                selectEdgeNodeInfo.add(ratcVo);
            }

            String bestNode = null;
            long minTotalTime = Integer.MAX_VALUE;

            for (RatcVo ratcVo : selectEdgeNodeInfo) {
                long executionTime = (int) ((double) task.getCpuCycle() / ratcVo.getCapacity() * 1000);
                long totalTime = executionTime + ratcVo.getWaitingTime();
                ratcVo.setTotalTime(totalTime);
                if (totalTime < minTotalTime) {
                    minTotalTime = totalTime;
                    bestNode = ratcVo.getEdgeId();
                }
            }
            log.info("select {}", bestNode);
            return bestNode;
        } else {
            int[] availAction = parseAvailAction(task.getAvailAction());
            int i = agent.selectAction(state, availAction, edgeNodeSystemService.isTraining()) + 1;
            log.info("drl select action {}", i);
            return String.format("edge-node-%d", i);
        }
    }

    public static int[] parseAvailAction(String availActionStr) {
        String[] parts = availActionStr.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }
}