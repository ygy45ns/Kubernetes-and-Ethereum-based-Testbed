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

import javax.annotation.Resource;

@Service
@Slf4j
@RefreshScope
@Lazy
public class DDQNScheduler implements IScheduler {

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
    private int agentNumber;
    @Value("${rl.n-step}")
    private int nStep;

    private long step = 0;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Override
    public String selectAction(Long taskId) {
        if (edgeNodeSystemService.isTraining()) {
            step++;
            if (edgeNodeSystemService.isStartLearn() && (step % nStep == 0)) {
                agent.train(edgeNodeSystemService.isCollecting());
            }
        }
        Task task = taskService.getById(taskId);
        var state = transitionService.getState(taskId, edgeNodeSystemService.isTraining());

        if (task.getStatus().equals(TaskStatus.EMPTY)) {
            log.info("select edge-node-{}", (agentNumber + 1));
            return String.format("edge-node-%d", (agentNumber + 1));
        }

        int[] availAction = parseAvailAction(task.getAvailAction());
        int i = agent.selectAction(state, availAction, edgeNodeSystemService.isTraining()) + 1;
        log.info("drl select edge-node-{}", i);
        return String.format("edge-node-%d", i);
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