package cn.edu.hznu.scheduler;


import cn.edu.hznu.agent.Agent;
import cn.edu.hznu.bean.EdgeNodeSystem;
import cn.edu.hznu.bean.Task;
import cn.edu.hznu.bean.TaskStatus;
import cn.edu.hznu.service.BlockchainService;
import cn.edu.hznu.service.EdgeNodeSystemService;
import cn.edu.hznu.service.TaskService;
import cn.edu.hznu.service.TransitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RefreshScope
@Lazy
public class DTTOScheduler implements IScheduler {

    @Autowired(required = false)
    private Agent agent;

    @Autowired
    private TransitionService transitionService;

    @Autowired
    private TaskService taskService;

    @Value("${spring.application.name}")
    public String name;

    @Resource
    BlockchainService blockchainService;

    @Resource
    private EdgeNodeSystemService edgeNodeSystemService;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int edgeNodeNumber;

    @Override
    public String selectAction(Long taskId) throws Exception {
//        String obj;
//        if (taskService.getById(taskId).getStatus().equals(TaskStatus.EMPTY)) {
//            obj = "edge-node-" + (edgeNodeNumber + 1);
//            log.info("select " + obj);
//            return obj;
//        }
        Task task = taskService.getById(taskId);
        int[] availAction = parseAvailAction(task.getAvailAction());
        List<BigInteger> reputationList = blockchainService.callGetReputations();
        float[] reputations = new float[reputationList.size()];
        for (int i = 0; i < reputationList.size(); i++) {
            reputations[i] = reputationList.get(i).floatValue() / 100f;
        }
        System.out.println("reputation: " + Arrays.toString(reputations));
        var state = transitionService.getState(taskId, reputations, edgeNodeSystemService.isTraining());

        if (edgeNodeSystemService.isTraining()) {
            int i = agent.selectAction(state, availAction, true) + 1;
            log.info("drl select edge-node-{}", i);
            return String.format("edge-node-%d", i);

        } else {
            if (taskService.getById(taskId).getStatus().equals(TaskStatus.EMPTY)) {
                String obj = "edge-node-" + (edgeNodeNumber + 1);
                log.info("select " + obj);
                return obj;
            }
            int i = agent.selectAction(state, availAction, false) + 1;
            log.info("drl select edge-node-{}", i);
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