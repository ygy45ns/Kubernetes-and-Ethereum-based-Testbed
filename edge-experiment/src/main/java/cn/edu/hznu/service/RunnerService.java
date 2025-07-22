package cn.edu.hznu.service;


import cn.edu.hznu.bean.Task;
import cn.edu.hznu.util.MathUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;


@Service
@Slf4j
public class RunnerService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TaskService taskService;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int agentNumber;

    @Value("${edgeComputing.episodeLimit}")
    private int episodeLimit;

    @Value("${edgeComputing.episodeLimit}")
    private int deadline;

    @Value("${edgeComputing.timeSlot}")
    private int timeSlot;

    // heuristic-test
    @Value("${rl.state-shape:0}")
    private int stateShape;

    @Value("${edgeComputing.testNumber}")
    private int testNumber;

    @Getter
    private double successRateAvg;

    @Getter
    private double successRateStd;

    private final List<String> overStatuses = Arrays.asList(
            "SUCCESS",
            "EXECUTION_FAILURE",
            "TRANSMISSION_FAILURE",
            "DROP",
            "EMPTY",
            "MALICIOUS_FAILURE"
    );

    public void init() {
        var controllerUrl = "http://edge-controller";
        var generateMessage = restTemplate.getForObject(controllerUrl + "/generate", String.class);
        log.info("generate edge nodes configuration: {}", generateMessage);
        var initMessage = restTemplate.getForObject(controllerUrl + "/init", String.class);
        log.info("init edge nodes: {}", initMessage);
        var initMaliciousNodes = restTemplate.getForObject(controllerUrl + "/initMaliciousNodes", String.class);
        log.info("init malicious edge nodes: {}", initMaliciousNodes);
        var initReputationMessage = restTemplate.getForObject(controllerUrl + "/initReputation", String.class);
        log.info("init edge nodes reputations: {}", initReputationMessage);
    }

    public void reset() {
        var controllerUrl = "http://edge-controller";
        var initMaliciousNodes = restTemplate.getForObject(controllerUrl + "/initMaliciousNodes", String.class);
        log.info("reset malicious edge nodes: {}", initMaliciousNodes);
        var initReputationMessage = restTemplate.getForObject(controllerUrl + "/initReputation", String.class);
        log.info("reset edge nodes reputations: {}", initReputationMessage);
    }


    public void run() {
        var controllerUrl = "http://edge-controller";
        var restartMessage = restTemplate.getForObject(controllerUrl + "/restart", String.class);
        log.info("restart experiment : {}", restartMessage);
        while (true) {
            try {
                Thread.sleep(timeSlot);
            } catch (InterruptedException e) {
                log.error("{}", e.getMessage());
            }
            var count = taskService.count(new QueryWrapper<Task>().in("status", overStatuses));
            if (count >= ((long) (episodeLimit + 1) * agentNumber)) {
                break;
            }
        }
        var stopMessage = restTemplate.getForObject(controllerUrl + "/stop", String.class);
        log.info("stop experiment: {}", stopMessage);
    }


    public void collectOfflineDataMode() {
        var controllerUrl = "http://edge-controller";
        String res = restTemplate.getForObject(controllerUrl + "/collectOfflineDataMode", String.class);
        log.info("change to collect offline data mode: {}", res);
    }

    public void onlineTrainMode() {
        var controllerUrl = "http://edge-controller";
        String res = restTemplate.getForObject(controllerUrl + "/onlineTrainMode", String.class);
        log.info("change to online train mode: {}", res);
    }

    public void startLearn() {
        var controllerUrl = "http://edge-controller";
        String res = restTemplate.getForObject(controllerUrl + "/startLearn", String.class);
        log.info("DDQN start learn: {}", res);
    }


    public void test() {
        var list = new ArrayList<Double>();
        var controllerUrl = "http://edge-controller";
        restTemplate.getForObject(controllerUrl + "/changeToTest", String.class);
        for (int i = 1; i <= testNumber; i++) {
            run();
            var successRate = taskService.getSuccessRate();
            log.info("test {}, success rate : {}", i, successRate);
            list.add(successRate);
            taskService.remove(null);
//            reset();
        }
        successRateAvg = MathUtils.avg(list);
        successRateStd = MathUtils.std(list);
        log.info("the results are as follow.");
        log.info("avg success rate: {}", String.format("%.3f", successRateAvg));
        log.info("std success rate: {}", String.format("%.3f", successRateStd));
        restTemplate.getForObject(controllerUrl + "/changeToTrain", String.class);
    }

    public void addData() {
        var controllerUrl = "http://edge-controller";
        var addMessage = restTemplate.getForObject(controllerUrl + "/addData", String.class);
        log.info("add data: {}", addMessage);
    }


    public void initAgent(String flag) {
        var controllerUrl = "http://edge-controller";
        var message = restTemplate.getForObject(controllerUrl + "/initAgent?flag=" + flag, String.class);
        log.info("init agents: {}", message);
    }

    public void loadAgent(String testFlag) {
        var controllerUrl = "http://edge-controller";
        var message = restTemplate.getForObject(controllerUrl + "/loadAgent?flag=" + testFlag, String.class);
        log.info("load agents: {}", message);
    }

    public void saveModel(String flag) {
        var controllerUrl = "http://edge-controller";
        var message = restTemplate.getForObject(controllerUrl + "/saveModel?flag=" + flag, String.class);
        log.info("save models: {}", message);
    }

    public void train() {
        var controllerUrl = "http://edge-controller";
        var message = restTemplate.getForObject(controllerUrl + "/train", String.class);
        log.info("train models: {}", message);
    }
}
