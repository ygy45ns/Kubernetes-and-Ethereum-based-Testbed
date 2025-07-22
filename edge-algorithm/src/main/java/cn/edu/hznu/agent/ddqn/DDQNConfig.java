package cn.edu.hznu.agent.ddqn;

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.TrainingConfig;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.PolynomialDecayTracker;
import ai.djl.training.tracker.Tracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "rl.name", havingValue = "DDQN")
public class DDQNConfig {
    @Value("${rl.learning-rate}")
    private float learningRate;

    @Value("${rl.hidden-shape}")
    private int hiddenShape;

    @Value("${rl.action-shape}")
    private int actionShape;

    @Value("${rl.state-shape}")
    private int stateShape;

    @Value("${edgeComputing.episodeNumber}")
    private int episodeNumber;

    @Value("${rl.batch-size}")
    private int batchSize;


    @Bean
    public Tracker polynomialDecayTracker() { // deprecated
        int trainNum = episodeNumber;
        return PolynomialDecayTracker.builder()
                .setBaseValue(0.001f)
                .setEndLearningRate(0.0001f)
                .setDecaySteps(trainNum)
                .build();
    }

    @Bean
    public Optimizer optimizer(Tracker polynomialDecayTracker) {
        var adam = Optimizer.adam();
        adam.optLearningRateTracker(Tracker.fixed(learningRate));
        return adam.build();
    }

    public Block createNetwork(NDManager manager, int outputDim) {
        var block = new SequentialBlock();
        block.add(Linear.builder().setUnits(hiddenShape).build());
        block.add(Activation::relu);
        block.add(Linear.builder().setUnits(hiddenShape).build());
        block.add(Activation::relu);
        block.add(Linear.builder().setUnits(outputDim).build());
        block.initialize(manager, DataType.FLOAT32, new Shape(batchSize, stateShape));
        return block;
    }

    @Bean
    public Model qModelOnline(NDManager manager) {
        Model model = Model.newInstance("ddqn-online");
        model.setBlock(createNetwork(manager, actionShape));
        return model;
    }

    @Bean
    public Model qModelTarget(NDManager manager) {
        var model = Model.newInstance("ddqn-target");
        model.setBlock(createNetwork(manager, actionShape));
        return model;
    }

    @Bean
    public Loss loss() {
        return new Loss("null") {
            @Override
            public NDArray evaluate(NDList ndList, NDList ndList1) {
                return null;
            }
        };
    }

    @Bean
    public TrainingConfig trainingConfig(Optimizer optimizer, Loss loss) {
        return new DefaultTrainingConfig(loss)
                .optOptimizer(optimizer);
    }
}
