package cn.edu.hznu.util;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import ai.djl.nn.Block;
import ai.djl.nn.ParameterList;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

@Slf4j
public class DJLUtils {
    public static int sampleMultinomial(Random random, NDArray prob) {
//        int value = 0;
//        long size = prob.size();
//        float rnd = random.nextFloat();
//        for (int i = 0; i < size; i++) {
//            float cut = prob.getFloat(value);
//            if (rnd > cut) {
//                value++;
//            } else {
//                return value;
//            }
//            rnd -= cut;
//        }
//        throw new IllegalArgumentException("Invalid multinomial distribution");
        // 上述代码是不鲁棒的，因为动作选择考虑了mask动作的功能，因此非空任务的最后一个动作选择概率为0
        // 假如生成的rnd较大，即使每次减去一个概率，到最后rnd=0，该方法也不会有返回
        // 改进如下：
        float rnd = random.nextFloat();
        float cumulative = 0f;
        for (int i = 0; i < prob.size(); i++) {
            cumulative += prob.getFloat(i);
            if (rnd < cumulative) {
                return i;
            }
        }
        return prob.argMax().toType(DataType.INT32, false).getInt(); // fallback
    }

    public static void softUpdate(Block network, Block targetNetwork, float tau) {
        var parameters = network.getParameters();
        var targetParameters = targetNetwork.getParameters();
        for (String key : parameters.keys()) {
            var array = parameters.get(key).getArray();
            var targetArray = targetParameters.get(key).getArray();
            var mixArray = targetArray.mul(1.0f - tau).add(array.mul(tau));
            targetParameters.get(key).getArray().set(mixArray.toFloatArray());
        }
    }

    public static void hardUpdate(Block network, Block targetNetwork) {
        var parameters = network.getParameters();
        var targetParameters = targetNetwork.getParameters();
        for (String key : parameters.keys()) {
            targetParameters.get(key).getArray().set(parameters.get(key).getArray().toFloatArray());
        }
    }


//    public static void saveModel(Path path, Block actorBlock) {
//        try {
//            Files.createDirectories(path.getParent());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        try (var fileOutputStream = new FileOutputStream(path.toFile());
//             var dataOutputStream = new DataOutputStream(fileOutputStream)) {
//            actorBlock.saveParameters(dataOutputStream);
//        } catch (IOException e) {
//            log.error("save model error");
//            throw new RuntimeException(e);
//        }
//    }

//    public static void loadModel(Path path, Block actorBlock, NDManager manager) {
//        try (var fileInputStream = new FileInputStream(path.toFile());
//             var dataInputStream = new DataInputStream(fileInputStream)) {
//            actorBlock.loadParameters(manager, dataInputStream);
//            ParameterList parameters = actorBlock.getParameters();
//            List<String> keys = parameters.keys();
//            for (String key : keys) {
//                var data = parameters.get(key).getArray();
//                data.setRequiresGradient(true);
//            }
//        } catch (IOException e) {
//            log.error("read file error");
//            throw new RuntimeException(e);
//        } catch (MalformedModelException e) {
//            log.error("load model error");
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static void loadModel(Path path, Model model, NDManager manager) {
//        try (DataInputStream dis = new DataInputStream(new FileInputStream(path.toFile()))) {
//            model.getBlock().loadParameters(manager, dis);
//            ParameterList paramList = model.getBlock().getParameters();
//            StringBuilder sb = new StringBuilder("Loaded Model Params:\n");
//            for (String key : paramList.keys()) {
//                NDArray param = paramList.get(key).getArray();
//                sb.append(String.format("%s: mean=%.6f\n", key, param.mean().getFloat()));
//            }
//            System.out.println("load sb is " + sb);
//
//        } catch (IOException | MalformedModelException e) {
//            throw new RuntimeException("Failed to load model from " + path, e);
//        }
//    }

    public static void loadModel(String basePath, Model model, String modelName) {
        try {
            Path modelDir = Paths.get(basePath);
            model.load(modelDir, modelName);
            System.out.println("Model loaded from " + modelDir.resolve(modelName + "-model.json"));
        } catch (IOException | MalformedModelException e) {
            throw new RuntimeException("Failed to load model", e);
        }
    }

    public static void saveModel(String basePath, Model model, String modelName) {
        try {
            Path modelDir = Paths.get(basePath);
            Files.createDirectories(modelDir);
            model.save(modelDir, modelName);
            System.out.println("Model saved to " + modelDir.resolve(modelName + "-model.json"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save model", e);
        }
    }


    public static void loadStreamModel(InputStream inputStream, Block actorBlock, NDManager manager) {
        try (var dataInputStream = new DataInputStream(inputStream)) {
            log.info("load stream model.");
            actorBlock.loadParameters(manager, dataInputStream);
            ParameterList parameters = actorBlock.getParameters();
            List<String> keys = parameters.keys();
            for (String key : keys) {
                var data = parameters.get(key).getArray();
                data.setRequiresGradient(true);
            }
        } catch (IOException e) {
            log.error("IOException");
            throw new RuntimeException(e);
        } catch (MalformedModelException e) {
            log.error("MalformedModelException");
            throw new RuntimeException(e);
        }
    }

    public static NDArray getReturn(NDArray rewards, NDArray nextStateValues, float gamma, int episodeLimit) {
        var res = rewards.zerosLike();
        for (long i = episodeLimit - 1; i >= 0; i--) {
            NDArray nextReturn;
            if (i == episodeLimit - 1) {
                nextReturn = nextStateValues.get(episodeLimit - 1);
            } else {
                nextReturn = res.get(i + 1);
            }
            var currentReturn = rewards.get(i).add(nextReturn.mul(gamma));
            res.set(new NDIndex(i), currentReturn);
        }
        return res;
    }

    public static NDArray getReturn(NDArray rewards, float gamma, int episodeLimit) {
        var res = rewards.zerosLike();
        for (long i = episodeLimit - 1; i >= 0; i--) {
            NDArray nextReturn;
            if (i == episodeLimit - 1) {
                nextReturn = rewards.get(i);
            } else {
                nextReturn = res.get(i + 1);
            }
            var currentReturn = rewards.get(i).add(nextReturn.mul(gamma));
            res.set(new NDIndex(i), currentReturn);
        }
        return res;
    }


    public static NDArray getGae(NDArray rewards, NDArray stateValues, NDArray nextStateValues, NDManager manager, float gamma, int episodeLimit, float gaeLambda) {
        var advantages = rewards.zerosLike();
        var deltas = rewards.add(nextStateValues.mul(gamma)).sub(stateValues);
        var nextAdvantage = manager.create(0.0f);
        for (long i = episodeLimit - 1; i >= 0; i--) {
            var advantage = deltas.get(i).add(nextAdvantage.mul(gamma).mul(gaeLambda));
            advantages.set(new NDIndex(i), advantage);
            nextAdvantage = advantage;
        }
        return advantages;
    }
}
