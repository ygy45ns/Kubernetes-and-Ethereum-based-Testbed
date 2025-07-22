package cn.edu.hznu.service;

import cn.edu.hznu.agent.Buffer;
import cn.edu.hznu.bean.*;
import cn.edu.hznu.util.ArrayUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

@Service
@Slf4j
public class TransitionService implements InitializingBean {

    @Value("${edgeComputing.maxTaskRate}")
    private double maxTaskRate;

    @Value("${edgeComputing.maxCpuCore}")
    private int maxCpuCore;

    @Value("${edgeComputing.maxTaskSize}")
    private long maxTaskSize;

    @Value("${edgeComputing.maxTaskComplexity}")
    private long maxTaskComplexity;

    @Value("${edgeComputing.maxTransmissionRate}")
    private double maxTransmissionRate;

    @Value("${edgeComputing.maxTransmissionFailureRate}")
    private double maxTransmissionFailureRate;

    @Value("${edgeComputing.maxExecutionFailureRate}")
    private double maxExecutionFailureRate;

    @Value("${edgeComputing.deadline}")
    private int deadline;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int agentNumber;

    @Value("${edgeComputing.episodeLimit}")
    private int episodeLimit;

    @Value("${rl.state-shape:0}")
    private int stateShape;

    @Value("${rl.action-shape:0}")
    private int actionShape;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LinkService linkService;

    @Value("${rl.buffer-size:64}")
    private int bufferSize;

    @Value("${rl.online-buffer-size:64}")
    private int onlineBufferSize;


    @Value("${spring.application.name}")
    private String name;

    @Setter
    private int bufferIndex = 0;

    @Lazy
    @Autowired(required = false)
    private Buffer buffer;

    @Getter
    private float[][] allObs;


    @Getter
    private Long[] allTaskID;

    @Override
    public void afterPropertiesSet() {
        allObs = new float[bufferSize + 1][stateShape];
        allTaskID = new Long[bufferSize + 1];
    }

    public ArrayList<Float> getPartState(Long taskId) {
        // lack reputation
        var task = taskService.getById(taskId);
        var edgeNodeInfos = edgeNodeService.list();
        String sourceName = task.getSource(); // edge-node-x
        var linkInfos = linkService.list(new QueryWrapper<Link>().eq("source", sourceName));
        var sourceNode = edgeNodeInfos.stream().filter(node -> node.getName().equals(sourceName)).findFirst().orElseThrow(() -> new RuntimeException("Node not found: " + sourceName));

        var obsList = new ArrayList<Float>();
        // task: 2
        obsList.add(Float.valueOf(task.getTaskSize()) / (float) (maxTaskSize * StoreConstants.Byte.value * StoreConstants.Kilo.value));
        obsList.add(task.getTaskComplexity() / (float) maxTaskComplexity);

        // edge node: 2
        obsList.add((float) (sourceNode.getTaskRate() / maxTaskRate));
        obsList.add((float) (sourceNode.getCpuNum() / maxCpuCore));

        // link: 10
        for (Link link : linkInfos) {
            obsList.add((float) (link.getTransmissionRate() / (maxTransmissionRate * Constants.Mega.value * Constants.Byte.value)));
        }
        return obsList;
    }

    public float[] getState(Long taskId, float[] reputations, boolean training) {
        // with reputation mechanism
        var obsList = getPartState(taskId);
        // add reputation: 10
        for (float reputation : reputations) {
            obsList.add(reputation);
        }
        // add one-hot: 10
        int selfIndex = Integer.parseInt(name.substring(10)) - 1;
        for (int i = 0; i < agentNumber; i++) {
            if (i == selfIndex) {
                obsList.add(1.0f);
            } else {
                obsList.add(0.0f);
            }
        }
        var obs = ArrayUtils.toFloatArray(obsList);
        if (training) {
            allObs[bufferIndex] = obs;
            allTaskID[bufferIndex] = taskId;
            bufferIndex++;
        }
        return obs;
    }

    public float[] getState(Long taskId, boolean training) {
        // without reputation mechanism
        var obsList = getPartState(taskId);
        var obs = ArrayUtils.toFloatArray(obsList);
        if (training) {
            allObs[bufferIndex] = obs;
            allTaskID[bufferIndex] = taskId;
            bufferIndex++;
        }
        return obs;
    }

    public void addData() {
        bufferIndex = 0;
        var states = new float[episodeLimit][stateShape];
        var actions = new int[episodeLimit][1];
        var availActions = new int[episodeLimit][1];
        var rewards = new float[episodeLimit][1];
        var nextStates = new float[episodeLimit][stateShape];
        for (int i = 1; i <= episodeLimit; i++) {
            Long taskID = allTaskID[i - 1];
            states[i - 1] = allObs[i - 1];
            var action = getAction(taskID);
            actions[i - 1] = new int[]{action};
            nextStates[i - 1] = allObs[i - 1 + 1];
            var availAction = getAvailAction(taskID);
            availActions[i - 1] = availAction;
            float reward = getReward(taskID);
            rewards[i - 1] = new float[]{reward};
//            System.out.println("state is " + Arrays.toString(states[i - 1]));
//            System.out.println("action is " + Arrays.toString(actions[i - 1]));
//            System.out.println("nextState is " + Arrays.toString(nextStates[i - 1]));
//            System.out.println("availActions is " + Arrays.toString(availActions[i - 1]));
//            System.out.println("reward is " + Arrays.toString(rewards[i - 1]));
        }
        for (int i = 1; i <= episodeLimit; i++) {
            buffer.insert(states[i - 1], actions[i - 1], availActions[i - 1], rewards[i - 1], nextStates[i - 1]);
        }
    }

    public void changeToOnlineTrainMode() {
        buffer.setBufferSize(onlineBufferSize);
        buffer.setIndex(0);
    }


    public int getAction(Long taskId) {
        var task = taskService.getById(taskId);
        int action;

        if (task.getStatus().equals(TaskStatus.EMPTY)) {
            action = agentNumber;
        } else {
            action = Integer.parseInt(task.getDestination().substring(10)) - 1;
        }
        return action;
    }

    public int[] getAvailAction(Long taskId) {
        var task = taskService.getById(taskId);
        return Arrays.stream(task.getAvailAction().split(",")).mapToInt(Integer::parseInt).toArray();
    }


    public float getReward(Long taskId) {
        var task = taskService.getById(taskId);
        float reward;
        if (task.getStatus().equals(TaskStatus.EMPTY)) {
            reward = 0.5f;
        } else if (task.getStatus().equals(TaskStatus.MALICIOUS_FAILURE) || task.getStatus().equals(TaskStatus.EXECUTION_FAILURE) || task.getStatus().equals(TaskStatus.TRANSMISSION_FAILURE) || task.getStatus().equals(TaskStatus.DROP)) {
            reward = -1.0f;
        } else if (task.getStatus().equals(TaskStatus.SUCCESS)) {
            reward = 1.0f;
        } else {
            throw new RuntimeException("task status error");
        }
        return reward;
    }

    public float getTrustEnabledReward(Long taskId, float[] reputations) {
        var task = taskService.getById(taskId);
        float reward;
        if (task.getStatus().equals(TaskStatus.EMPTY)) {
            reward = 0.5f;
        } else if (task.getStatus().equals(TaskStatus.MALICIOUS_FAILURE) || task.getStatus().equals(TaskStatus.EXECUTION_FAILURE) || task.getStatus().equals(TaskStatus.TRANSMISSION_FAILURE) || task.getStatus().equals(TaskStatus.DROP)) {
            reward = -1.0f;
        } else if (task.getStatus().equals(TaskStatus.SUCCESS)) {
            reward = 1.0f + reputations[Integer.parseInt(task.getDestination().substring(10)) - 1];
        } else {
            throw new RuntimeException("task status error");
        }
        return reward;
    }
}

