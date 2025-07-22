package cn.edu.hznu.agent.ddqn;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.nn.Parameter;
import ai.djl.training.TrainingConfig;
import ai.djl.translate.NoopTranslator;
import ai.djl.translate.TranslateException;
import cn.edu.hznu.agent.Agent;
import cn.edu.hznu.agent.Buffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@ConditionalOnProperty(name = "rl.name", havingValue = "DDQN")
public class DDQNAgent implements Agent, InitializingBean {
    @Autowired
    private Random schedulerRandom;

    @Autowired
    private NDManager manager;

    @Autowired
    private Buffer buffer;


    @Value("${rl.gamma}")
    private float gamma;

    @Value("${spring.application.name}")
    String name;

    @Autowired
    private Model qModelOnline;

    @Autowired
    private Model qModelTarget;

    @Autowired
    private TrainingConfig trainingConfig;

    private final Lock lock = new ReentrantLock();

    @Value("${rl.epsilon-start}")
    private float epsilonStart;

    @Value("${rl.epsilon-max}")
    private float epsilonMax;

    @Value("${rl.epsilon-increment}")
    private float epsilonIncrement;

    private float epsilon;

    private long trainStep = 0;

    @Value("${rl.replace-target-iter}")
    private int replace_target_iter = 100;
    @Value("${edgeComputing.edgeNodeNumber}")
    private int agentNumber;

    @Override
    public void afterPropertiesSet() {
        epsilon = epsilonStart;
    }

    public int selectAction(float[] state, int[] availAction, boolean training) {
        int action;
        try {
            lock.lock();
            var subManager = manager.newSubManager();
            try (subManager) {
                if (training) {
                    var val = schedulerRandom.nextFloat();
                    if (val < epsilon) {
                        var predictor = qModelOnline.newPredictor(new NoopTranslator());
                        NDArray qValues = predictor.predict(new NDList(subManager.create(state))).singletonOrThrow();
                        NDArray argMaxIndex = qValues.argMax();
                        action = argMaxIndex.toType(DataType.INT32, false).getInt();
                        return action;
                    } else {
                        action = schedulerRandom.nextInt(agentNumber);
                        return action;
                    }
                } else {
                    var predictor = qModelOnline.newPredictor(new NoopTranslator());
                    NDArray qValues = predictor.predict(new NDList(subManager.create(state))).singletonOrThrow();
                    System.out.println(qValues);
                    NDArray argMaxIndex = qValues.argMax();
                    action = argMaxIndex.toType(DataType.INT32, false).getInt();
                    return action;
                }
            } catch (TranslateException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }
    }

    public void train(boolean isOffline) {
        try {
            lock.lock();
            try (var trainer = qModelOnline.newTrainer(trainingConfig)) {
                var subManager = trainer.getManager();
                try (subManager) {
                    var batch = buffer.sample(subManager);
                    NDArray states = batch.get(0);
                    NDArray actions = batch.get(1);
                    NDArray availActions = batch.get(2);
                    NDArray rewards = batch.get(3);
                    NDArray nextStates = batch.get(4);

                    NDArray qPred = trainer.forward(new NDList(states)).singletonOrThrow();
                    NDArray qPredTaken = qPred.gather(actions.reshape(-1, 1), 1);

                    NDArray nextActions;
                    try (var predictorOnline = qModelOnline.newPredictor(new NoopTranslator())) {
                        nextActions = predictorOnline.predict(new NDList(nextStates))
                                .singletonOrThrow()
                                .argMax(1);
                    }


                    NDArray nextQValues;
                    try (var predictorTarget = qModelTarget.newPredictor(new NoopTranslator())) {
                        NDArray targetQValues = predictorTarget.predict(new NDList(nextStates)).singletonOrThrow();
                        nextQValues = targetQValues.get(nextActions);
                    }


                    NDArray targetQ = rewards.add(nextQValues.mul(gamma));

                    NDArray loss;
                    try (var gc = trainer.newGradientCollector()) {
                        loss = qPredTaken.sub(targetQ.stopGradient()).pow(2).mean();
                        gc.backward(loss);
                    }

                    System.out.printf("Step: %d | Loss: %.4f | ε: %.3f%n",
                            trainStep, loss.getFloat(), epsilon);

                    trainer.step();
                    trainStep++;

                    // 更新 epsilon
                    epsilon = Math.min(epsilon + epsilonIncrement, epsilonMax);

                    // 替换 target 网络参数
                    if (trainStep % replace_target_iter == 0) {
                        List<Parameter> sourceParams = qModelOnline.getBlock().getParameters().values();
                        List<Parameter> targetParams = qModelTarget.getBlock().getParameters().values();

                        for (int i = 0; i < sourceParams.size(); i++) {
                            NDArray srcArray = sourceParams.get(i).getArray();
                            NDArray tgtArray = targetParams.get(i).getArray();
                            if (srcArray == null || tgtArray == null) continue;

                            Device srcDevice = srcArray.getDevice();
                            Device tgtDevice = tgtArray.getDevice();

                            if (!srcDevice.equals(tgtDevice)) {
                                try (NDArray temp = tgtArray.toDevice(srcDevice, false)) {
                                    srcArray.copyTo(temp);
                                }
                            } else {
                                srcArray.copyTo(tgtArray);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("DDQN train error", e);
        } finally {
            lock.unlock();
        }
    }


    public void saveModel(String path, int id) {
        String basePath = "results/model/" + path + "/";
        var onlinePath = basePath + "online_" + id + ".param";
        var targetPath = basePath + "target_" + id + ".param";
        try {
            Files.createDirectories(Paths.get(onlinePath).getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        DJLUtils.saveModel(Paths.get(onlinePath), qModelOnline.getBlock());
//        DJLUtils.saveModel(Paths.get(targetPath), qModelTarget.getBlock());
    }

    public void loadModel(String path, int id) {
        String basePath = "results/model/" + path + "/";
        var onlinePath = basePath + "online_" + id + ".param";
        var targetPath = basePath + "target_" + id + ".param";
//        DJLUtils.loadModel(Paths.get(onlinePath), qModelOnline.getBlock(), manager);
//        DJLUtils.loadModel(Paths.get(targetPath), qModelTarget.getBlock(), manager);
    }

    @Override
    public Model getActorModel() {
        return null;
    }

}
