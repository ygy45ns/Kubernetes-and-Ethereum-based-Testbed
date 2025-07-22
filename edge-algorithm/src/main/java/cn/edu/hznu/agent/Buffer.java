package cn.edu.hznu.agent;


import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@Component
@Lazy
@Slf4j
@Setter
public class Buffer implements InitializingBean {

    @Value("${rl.state-shape}")
    private int stateShape;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int agentNumber;

    @Value("${rl.action-shape}")
    private int actionShape;

    @Value("${rl.buffer-size}")
    private int bufferSize;

    @Value("${rl.batch-size}")
    private int batchSize;

    @Getter
    private float[][] states;
    @Getter
    private int[][] actions;
    @Getter
    private float[][] rewards;
    @Getter
    private int[][] availActions;
    @Getter
    private float[][] nextStates;

    private int index = 0;

    private int size = 0;

    @Autowired
    private Random bufferRandom;

    @Override
    public void afterPropertiesSet() {
        log.info("buffer-size {} ", bufferSize);
        states = new float[bufferSize][stateShape];
        actions = new int[bufferSize][1];
        availActions = new int[bufferSize][actionShape];
        rewards = new float[bufferSize][1];
        nextStates = new float[bufferSize][stateShape];
    }

    public void insert(float[] state, int[] action, int[] availAction, float[] reward, float[] nextState) {
        states[index] = state;
        actions[index] = action;
        availActions[index] = availAction;
        rewards[index] = reward;
        nextStates[index] = nextState;
        index = (index + 1) % bufferSize;
        size = Math.min(size + 1, bufferSize);
    }

    public NDList sample(NDManager manager) {
        var list = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        Collections.shuffle(list, bufferRandom);
        var batch = new int[batchSize];
        for (int i = 0; i < batchSize; i++) {
            batch[i] = list.get(i);
        }
        var ndStates = manager.zeros(new Shape(batchSize, stateShape), DataType.FLOAT32);
        var ndActions = manager.zeros(new Shape(batchSize, 1), DataType.INT32);
        var ndAvailActions = manager.zeros(new Shape(batchSize, actionShape), DataType.INT32);
        var ndRewards = manager.zeros(new Shape(batchSize, 1), DataType.FLOAT32);
        var ndNextStates = manager.zeros(new Shape(batchSize, stateShape), DataType.FLOAT32);

        for (int i = 0; i < batchSize; i++) {
            var ndIndex = new NDIndex(i);
            ndStates.set(ndIndex, manager.create(states[batch[i]]));
            ndActions.set(ndIndex, manager.create(actions[batch[i]]));
            ndAvailActions.set(ndIndex, manager.create(availActions[batch[i]]));
            ndRewards.set(ndIndex, manager.create(rewards[batch[i]]));
            ndNextStates.set(ndIndex, manager.create(nextStates[batch[i]]));
        }
        return new NDList(ndStates, ndActions, ndAvailActions, ndRewards, ndNextStates);
    }

    public NDList sampleAll(NDManager manager) {
        var ndStates = manager.zeros(new Shape(bufferSize, stateShape), DataType.FLOAT32);
        var ndActions = manager.zeros(new Shape(bufferSize, 1), DataType.INT32);
        var ndAvailActions = manager.zeros(new Shape(bufferSize, actionShape), DataType.INT32);
        var ndRewards = manager.zeros(new Shape(bufferSize, 1), DataType.FLOAT32);
        var ndNextStates = manager.zeros(new Shape(bufferSize, stateShape), DataType.FLOAT32);
        for (int i = 0; i < bufferSize; i++) {
            var ndIndex = new NDIndex(i);
            ndStates.set(ndIndex, manager.create(states[i]));
            ndActions.set(ndIndex, manager.create(actions[i]));
            ndAvailActions.set(ndIndex, manager.create(availActions[i]));
            ndRewards.set(ndIndex, manager.create(rewards[i]));
            ndNextStates.set(ndIndex, manager.create(nextStates[i]));
        }
        return new NDList(ndStates, ndActions, ndAvailActions, ndRewards, ndNextStates);
    }
}
