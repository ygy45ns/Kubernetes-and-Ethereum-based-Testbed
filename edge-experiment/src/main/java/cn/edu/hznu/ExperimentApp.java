package cn.edu.hznu;

import cn.edu.hznu.runner.*;
import cn.edu.hznu.util.SpringBeanUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExperimentApp {
    public static void main(String[] args) {
        var context = SpringApplication.run(ExperimentApp.class, args);
        SpringBeanUtils.setApplicationContext(context);
        var env = context.getEnvironment();
        Boolean isTest = env.getProperty("edgeComputing.isTest", Boolean.class);
        var runnerType = env.getProperty("edgeComputing.runner", String.class);
        assert runnerType != null;
        IRunner runner;
        if (Boolean.TRUE.equals(isTest)) { // test mode
            runner = context.getBean(TestingRunner.class);
        } else {
            runner = switch (runnerType) {
                case "DTTO" -> context.getBean(DTTOTrainingRunner.class);
                case "BRMTO" -> context.getBean(BRMTORunner.class);
                case "LOCAL" -> context.getBean(LOCALRunner.class);
                case "SAC" -> context.getBean(SACTrainingRunner.class);
                case "DDQN" -> context.getBean(DDQNTrainingRunner.class);
                default -> throw new RuntimeException("error in runner type.");
            };
        }

        // waiting for other components start.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        runner.run();
    }
}