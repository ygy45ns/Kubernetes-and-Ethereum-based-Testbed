package cn.edu.hznu.agent.dtto;

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.training.TrainingConfig;
import ai.djl.translate.NoopTranslator;
import ai.djl.translate.TranslateException;
import cn.edu.hznu.agent.Agent;
import cn.edu.hznu.agent.Buffer;
import cn.edu.hznu.util.DJLUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@ConditionalOnProperty(name = "rl.name", havingValue = "DTTO")
public class DTTOAgent implements Agent, InitializingBean {
    @Autowired
    private Random schedulerRandom;

    @Autowired
    private NDManager manager;

    @Autowired
    private Buffer buffer;

    @Value("${rl.use-normalized-reward}")
    private boolean useNormalizedReward;

    @Value("${rl.epoch}")
    private int epoch;

    @Value("${rl.clip}")
    private float clip;

    @Value("${rl.use-entropy}")
    private boolean useEntropy;

    @Value("${rl.entropy-coef}")
    private float entropyCoef;

    @Value("${rl.gamma}")
    private float gamma;

    @Value("${edgeComputing.episodeLimit}")
    private int episodeLimit;

    @Value("${rl.use-gae}")
    private boolean useGae;

    @Value("${rl.gae-lambda}")
    private float gaeLambda;

    @Value("${spring.application.name}")
    String name;

    @Autowired
    private Model actorModel;

    @Autowired
    private Model criticModel;

    @Autowired
    private TrainingConfig trainingConfig;

    private final Lock lock = new ReentrantLock();

    @Override
    public void afterPropertiesSet() {
    }

    public int selectAction(float[] state, int[] availAction, boolean training) {
        int action;
        try {
            lock.lock();
            var subManager = manager.newSubManager();
            try (subManager) {
                var predictor = actorModel.newPredictor(new NoopTranslator());
                try (predictor) {
                    NDArray out = null;
                    try {
                        out = predictor.predict(new NDList(subManager.create(state))).singletonOrThrow();
                    } catch (TranslateException e) {
                        log.error("predict error: {}", e.getMessage());
                    }
                    var bool = subManager.create(availAction).eq(0);
                    assert out != null;
                    out.set(bool, -1e5f);
                    var prob = out.softmax(-1);
                    log.info("prob: {}", prob);
                    if (training) {
                        action = DJLUtils.sampleMultinomial(schedulerRandom, prob);
                    } else {
                        action = prob.argMax().toType(DataType.INT32, false).getInt();
                    }
//                    action = DJLUtils.sampleMultinomial(schedulerRandom, prob);
                }
            }
        } finally {
            lock.unlock();
        }
        return action;
    }

    public void train(boolean isOffline) {
        try {
            lock.lock();
            var criticTrainer = criticModel.newTrainer(trainingConfig);
            var actorTrainer = actorModel.newTrainer(trainingConfig);
            var subManager = actorTrainer.getManager();
            try (subManager) {
                var list = buffer.sampleAll(subManager);
//                var list = buffer.sample(subManager);
                var states = list.get(0);
                var actions = list.get(1);
                var availActions = list.get(2);
                var rewards = list.get(3);
                var nextStates = list.get(4);

                // Trick: normalized reward
                if (useNormalizedReward) {
                    var mean = rewards.mean();
                    var std = rewards.sub(mean).pow(2).mean().sqrt().add(1e-5f);
                    rewards = rewards.sub(mean).div(std);
                }

                try (criticTrainer; actorTrainer) {
                    var stateValues = criticTrainer.forward(new NDList(states)).singletonOrThrow();
                    var nextStateValues = criticTrainer.forward(new NDList(nextStates)).singletonOrThrow();

                    // 使用stopGradient，用于GAE或return计算，不参与训练
                    NDArray stateValuesForAdv = stateValues.stopGradient();
                    NDArray nextStateValuesForAdv = nextStateValues.stopGradient();

                    // gae
                    NDArray advantages;
                    if (useGae) {
                        advantages = DJLUtils.getGae(rewards, stateValuesForAdv, nextStateValuesForAdv, subManager, gamma, episodeLimit, gaeLambda);
                    } else {
                        NDArray returns = DJLUtils.getReturn(rewards, nextStateValuesForAdv, gamma, episodeLimit);
                        advantages = returns.sub(stateValuesForAdv);
                    }

                    NDArray targets = advantages.add(stateValuesForAdv); // targets也用stopGradient的结果

                    var out = actorTrainer.forward(new NDList(states)).singletonOrThrow();
                    var mask = availActions.eq(0);
                    out.set(mask, -1e5f);
                    var logProb = out.logSoftmax(-1);
                    var oldLogProbTaken = logProb.gather(actions, -1).stopGradient();  // 明确冻结旧策略

                    // 多epoch训练
                    for (int i = 0; i < epoch; i++) {
                        var gradientCollector = actorTrainer.newGradientCollector();
                        try (gradientCollector) {
                            var logits = actorTrainer.forward(new NDList(states)).singletonOrThrow();
                            logits.set(mask, -1e5f);
                            var probs = logits.softmax(-1);
                            var logProbs = logits.logSoftmax(-1);
                            var logProbTaken = logProbs.gather(actions, -1);

                            NDArray adv = advantages.stopGradient();

                            // PPO
                            var ratios = logProbTaken.sub(oldLogProbTaken).exp();
                            var surr1 = ratios.mul(adv);
                            var surr2 = ratios.clip(1 - clip, 1 + clip).mul(adv);
                            var loss = NDArrays.minimum(surr1, surr2).mean().neg();

                            if (useEntropy) {
                                var entropy = probs.mul(logProbs).neg();
                                var entropyLoss = entropy.mean().mul(entropyCoef).neg();
                                loss = loss.add(entropyLoss);
                            }
                            gradientCollector.backward(loss);
                            actorTrainer.step();
                        }
                        var criticGradientCollector = criticTrainer.newGradientCollector();
                        try (criticGradientCollector) {
                            var newValues = criticTrainer.forward(new NDList(states)).singletonOrThrow();
                            var criticLoss = newValues.sub(targets).pow(2).mean().mul(0.5f);
                            criticGradientCollector.backward(criticLoss);
                            criticTrainer.step();
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void saveModel(String path, int id) {
        String baseDir = Paths.get("results", "model", path).toString();
        String modelName = "actor_" + id;
        try {
            Files.createDirectories(Paths.get(baseDir));
            DJLUtils.saveModel(baseDir, actorModel, modelName);

            modelName = "critic_" + id;
            DJLUtils.saveModel(baseDir, criticModel, modelName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void loadModel(String path, int id) {
        String baseDir = Paths.get("results", "model", path).toString();
        String modelName = "actor_" + id;
        DJLUtils.loadModel(baseDir, actorModel, modelName);

        modelName = "critic_" + id;
        DJLUtils.loadModel(baseDir, criticModel, modelName);
    }


    @Override
    public Model getActorModel() {
        return null;
    }
}
