package cn.edu.hznu.agent.sac;

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
import lombok.Getter;
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

@Component
@Slf4j
@ConditionalOnProperty(name = "rl.name", havingValue = "SAC")
public class SACAgent implements InitializingBean, Agent {

    @Value("${rl.action-shape}")
    private int actionShape;

    @Value("${rl.alpha}")
    private float alpha;

    @Value("${rl.gamma}")
    private float gamma;

    @Value("${rl.use-soft-update}")
    private boolean useSoftUpdate;

    @Value("${rl.tau}")
    private float tau;

    @Value("${rl.use-adaptive-alpha}")
    private boolean useAdaptiveAlpha;

    @Value("${rl.use-normalized-reward}")
    private boolean useNormalizedReward;

    @Autowired
    private Random schedulerRandom;

    @Autowired
    private Model q1Model;
    @Autowired
    private Model q2Model;

    @Autowired
    private Model targetQ1Model;
    @Autowired
    private Model targetQ2Model;

    @Getter
    @Autowired
    private Model actorModel;

    @Autowired
    private Model criticModel;

    @Autowired
    private Buffer buffer;

    @Autowired
    private NDManager manager;

    private float targetEntropy;

    @Autowired
    private Model alphaModel;

    @Value("${spring.application.name}")
    String name;

    @Value("${rl.use-cql}")
    private boolean useCql;

    @Value("${rl.cql-weight}")
    private float cqlWeight;

    @Value("${rl.use-addition-critic}")
    private boolean useAdditionCritic;

    @Value("${rl.target-entropy-coef}")
    private float targetEntropyCoef;

    @Value("${edgeComputing.episodeLimit}")
    private int episodeLimit;

    @Value("${rl.epoch}")
    private int epoch;

    @Value("${rl.clip}")
    private float clip;

    @Value("${rl.use-entropy}")
    private boolean useEntropy;

    @Value("${rl.entropy-coef}")
    private float entropyCoef;

    @Value("${rl.use-gae}")
    private boolean useGae;

    @Value("${rl.gae-lambda}")
    private float gaeLambda;

    @Autowired
    private TrainingConfig trainingConfig;

    @Override
    public void afterPropertiesSet() {
        targetEntropy = -(float) Math.log(1.0 / actionShape) * targetEntropyCoef;
    }

    public int selectAction(float[] state, int[] availAction, boolean training) {
        int action = 0;
        var subManager = manager.newSubManager();
        try (subManager) {
            var predictor = actorModel.newPredictor(new NoopTranslator());
            try (predictor) {
                try {
                    var out = predictor.predict(new NDList(subManager.create(state))).singletonOrThrow();
                    var bool = subManager.create(availAction).eq(0);
                    out.set(bool, -1e5f);
                    var prob = out.softmax(-1);
                    if (training) {
                        action = DJLUtils.sampleMultinomial(schedulerRandom, prob);
                    } else {
                        action = prob.argMax().toType(DataType.INT32, false).getInt();
                    }
                } catch (TranslateException e) {
                    log.error("predict error: {}", e.getMessage());
                }
            }
        }
        return action;
    }


    public void train(boolean isOffline) {
        if (isOffline) {// offline
            var subManager = manager.newSubManager();
            try (subManager) {
                NDList list = buffer.sample(subManager);
                var states = list.get(0);
                var actions = list.get(1);
                var availActions = list.get(2);
                var rewards = list.get(3);
                var nextStates = list.get(4);

                if (useNormalizedReward) {
                    var mean = rewards.mean();
                    var std = rewards.sub(mean).pow(2).mean().sqrt().add(1e-5f);
                    rewards = rewards.sub(mean).div(std);
                }

                var q1Trainer = q1Model.newTrainer(trainingConfig);
                var q2Trainer = q2Model.newTrainer(trainingConfig);
                var actorTrainer = actorModel.newTrainer(trainingConfig);
                var criticTrainer = criticModel.newTrainer(trainingConfig);
                var targetQ1Predictor = targetQ1Model.newPredictor(new NoopTranslator());
                var targetQ2Predictor = targetQ2Model.newPredictor(new NoopTranslator());
                var alphaTrainer = alphaModel.newTrainer(trainingConfig);

                NDArray alphaValue;
                if (useAdaptiveAlpha) {
                    alphaValue = alphaModel.getBlock().getParameters().get("alpha").getArray().duplicate().exp();
                } else {
                    alphaValue = subManager.create(alpha);
                }

                try (q1Trainer; q2Trainer; actorTrainer; criticTrainer; targetQ1Predictor; targetQ2Predictor; alphaTrainer) {
                    var actorOut = actorTrainer.evaluate(new NDList(nextStates)).singletonOrThrow();
                    var nextLogProbabilities = actorOut.logSoftmax(-1);

                    NDArray nextTargetQ1, nextTargetQ2;
                    try {
                        nextTargetQ1 = targetQ1Predictor.predict(new NDList(nextStates)).singletonOrThrow();
                        nextTargetQ2 = targetQ2Predictor.predict(new NDList(nextStates)).singletonOrThrow();
                    } catch (TranslateException e) {
                        throw new RuntimeException(e);
                    }

                    var targetQ = nextLogProbabilities.exp().mul(NDArrays.minimum(nextTargetQ1, nextTargetQ2).sub(nextLogProbabilities.mul(alphaValue)));
                    var avgTargetQ = targetQ.sum(new int[]{-1}, true);
                    var target = rewards.add(avgTargetQ.mul(gamma)).stopGradient();

                    var q1Value = q1Trainer.evaluate(new NDList(states)).singletonOrThrow();
                    var q2Value = q2Trainer.evaluate(new NDList(states)).singletonOrThrow();
                    var lobProbabilityValue = actorTrainer.evaluate(new NDList(states)).singletonOrThrow().logSoftmax(-1);

                    if (useAdaptiveAlpha) {
                        var alphaGradientCollector = alphaTrainer.newGradientCollector();
                        try (alphaGradientCollector) {
                            var entropy = lobProbabilityValue.exp().mul(lobProbabilityValue).sum(new int[]{-1}).mean().neg();
                            var logAlpha = alphaModel.getBlock().getParameters().get("alpha").getArray();
                            var loss = logAlpha.mul(entropy.sub(targetEntropy));
                            alphaGradientCollector.backward(loss);
                            alphaTrainer.step();
                        }
                    }

                    var actorGradientCollector = actorTrainer.newGradientCollector();
                    try (actorGradientCollector) {
                        var qMin = NDArrays.minimum(q1Value, q2Value);
                        var lobProbabilities = actorTrainer.forward(new NDList(states)).singletonOrThrow().logSoftmax(-1);
                        var loss = lobProbabilities.exp().mul(qMin.sub(lobProbabilities.mul(alphaValue))).sum(new int[]{-1}).mean().neg();
                        actorGradientCollector.backward(loss);
                        actorTrainer.step();
                    }

                    var q1GradientCollector = q1Trainer.newGradientCollector();
                    try (q1GradientCollector) {
                        var q1 = q1Trainer.forward(new NDList(states)).singletonOrThrow();
                        var q1Action = q1.gather(actions, -1);
                        var loss1 = q1Action.sub(target).pow(2).mean();
                        if (useCql) {
                            var cqlLoss1 = (q1.exp().sum().log().mean()).sub(q1Action.mean());
                            loss1.add(cqlLoss1.mul(cqlWeight));
                        }
                        q1GradientCollector.backward(loss1);
                        q1Trainer.step();
                    }

                    var q2GradientCollector = q2Trainer.newGradientCollector();
                    try (q2GradientCollector) {
                        var q2 = q2Trainer.forward(new NDList(states)).singletonOrThrow();
                        var q2Action = q2.gather(actions, -1);
                        var loss2 = q2Action.sub(target).pow(2).mean();
                        if (useCql) {
                            var cqlLoss2 = (q2.exp().sum().log().mean()).sub(q2Action.mean());
                            loss2.add(cqlLoss2.mul(cqlWeight));
                        }
                        q2GradientCollector.backward(loss2);
                        q2Trainer.step();
                    }

                    if (useAdditionCritic) {
                        var criticGradientCollector = criticTrainer.newGradientCollector();
                        try (criticGradientCollector) {
                            var value = criticTrainer.evaluate(new NDList(states)).singletonOrThrow();
                            var loss = value.sub(target).pow(2).mean();
                            criticGradientCollector.backward(loss);
                            criticTrainer.step();
                        }
                    }
                }
                if (useSoftUpdate) {
                    DJLUtils.softUpdate(q1Model.getBlock(), targetQ1Model.getBlock(), tau);
                    DJLUtils.softUpdate(q2Model.getBlock(), targetQ2Model.getBlock(), tau);
                }
            }
        } else { // online
            var criticTrainer = criticModel.newTrainer(trainingConfig);
            var actorTrainer = actorModel.newTrainer(trainingConfig);
            var subManager = actorTrainer.getManager();
            try (subManager) {
                var list = buffer.sampleAll(subManager);
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

                    NDArray targets = advantages.add(stateValuesForAdv);

                    var out = actorTrainer.forward(new NDList(states)).singletonOrThrow();
                    var mask = availActions.eq(0);
                    out.set(mask, -1e5f);
                    var logProb = out.logSoftmax(-1);
                    var oldLogProbTaken = logProb.gather(actions, -1).stopGradient();

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
        }
    }


    public void saveModel(String path, int id) {
        String basePath = "results/model/" + path + "/";
        var actorPath = basePath + "actor_" + id + ".param";
        var criticPath = basePath + "critic_" + id + ".param";
        try {
            Files.createDirectories(Paths.get(actorPath).getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        DJLUtils.saveModel(Paths.get(actorPath), actorModel.getBlock());
//        DJLUtils.saveModel(Paths.get(criticPath), criticModel.getBlock());
    }

    public void loadModel(String path, int id) {
        String basePath = "results/model/" + path + "/";
        var actorPath = basePath + "actor_" + id + ".param";
        var criticPath = basePath + "critic_" + id + ".param";
//        DJLUtils.loadModel(Paths.get(actorPath), actorModel, manager);
//        DJLUtils.loadModel(Paths.get(criticPath), criticModel, manager);
    }

}
