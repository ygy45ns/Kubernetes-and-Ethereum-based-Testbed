package cn.edu.hznu.runner;


import cn.edu.hznu.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@Lazy
public class TestingRunner implements IRunner {

    @Autowired
    private TaskService taskService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private RunnerService runnerService;

    @Value("${rl.test-flag:0}")
    private String testFlag;

    @Value("${edgeComputing.runner}")
    private String runner;


    public void run() {
        log.info("========================");
        log.info("run {} test!", runner);
        log.info("========================");
        // remove data
        linkService.remove(null);
        edgeNodeService.remove(null);
        taskService.remove(null);

        // init edge node, link and agent model
        runnerService.init();

        // load model parameters
        switch (runner) {
            case "DTTO" -> runnerService.loadAgent(testFlag);
            case "SAC" -> runnerService.loadAgent(testFlag);
            case "DDQN" -> runnerService.loadAgent(testFlag);
        }

        // test performance
        log.info("start to test.");
        runnerService.test();
        log.info("end test.");


        taskService.remove(null);
    }
}
