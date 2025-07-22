package cn.edu.hznu.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.edu.hznu.service.EdgeConfigService;
import cn.edu.hznu.service.EdgeNodeService;
import cn.edu.hznu.service.UserService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@CommonsLog
public class EdgeNodeController {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EdgeConfigService edgeConfigService;

    @Autowired
    private UserService userService;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int edgeNodeNumber;

    @Value("${edgeComputing.maliciousProportion}")
    private double maliciousProportion;

    @GetMapping("/generate")
    public String generate() {
        log.info("generate edge node configurations.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            edgeConfigService.generateEdgeLink(name);
        }
        return "success\n";
    }

    @GetMapping("/changeToTest")
    public String changeToTest() {
        log.info("change to test mode.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            String url = String.format("http://%s/cmd/changeToTest", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/changeToTrain")
    public String changeToTrain() {
        log.info("change to train mode.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            String url = String.format("http://%s/cmd/changeToTrain", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/startLearn")
    public String startLearn() {
        log.info("DDQN start learn.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            String url = String.format("http://%s/cmd/startLearn", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/init")
    public String init() {
        log.info("init edge node configurations.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("init " + name + " configuration");
            String url = String.format("http://%s/cmd/init", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/initMaliciousNodes")
    public String initMaliciousNodes() {
        log.info("init malicious edge node.");
        int maliciousNodesNum = (int) (edgeNodeNumber * maliciousProportion);
        List<String> allNodeNames = new ArrayList<>();
        for (int i = 1; i <= edgeNodeNumber; i++) {
            allNodeNames.add("edge-node-" + i);
        }
        Collections.shuffle(allNodeNames);
        List<String> maliciousNodeNames = allNodeNames.subList(0, maliciousNodesNum);
        String maliciousNodeStr = String.join(",", maliciousNodeNames);
        log.info("malicious nodes: " + maliciousNodeStr);
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("init " + name + " malicious node info");
            String url = String.format("http://%s/cmd/initMaliciousNodes?maliciousList=%s", name, maliciousNodeStr);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }


    @GetMapping("/initReputation")
    public String initReputation() {
        log.info("init edge node reputations.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("init " + name + " reputation");
            String url = String.format("http://%s/cmd/initReputation", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/addData")
    public String addData() {
        log.info("collect edge nodes training data.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("collect " + name + " training data");
            String url = String.format("http://%s/cmd/addData", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/collectOfflineDataMode")
    public String collectOfflineDataMode() {
        log.info("change to collect offline data mode.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            String url = String.format("http://%s/cmd/collectOfflineDataMode", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/onlineTrainMode")
    public String onlineTrainMode() {
        log.info("change to online train mode.");
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            String url = String.format("http://%s/cmd/onlineTrainMode", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/initAgent")
    public String initAgent(@RequestParam String flag) {
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("init agent for " + name);
            String url = String.format("http://%s/initAgent?flag=" + flag, name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/loadAgent")
    public String loadAgent(@RequestParam String flag) {
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("load agent for " + name);
            String url = String.format("http://%s/loadAgent?flag=" + flag, name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/saveModel")
    public String saveModel(@RequestParam String flag) {
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("save model for " + name);
            String url = String.format("http://%s/saveModel?flag=" + flag, name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/train")
    public String train() {
        for (int i = 1; i <= edgeNodeNumber; i++) {
            String name = String.format("edge-node-%d", i);
            log.info("train model for " + name);
            String url = String.format("http://%s/train", name);
            restTemplate.getForObject(url, String.class);
        }
        return "success\n";
    }

    @GetMapping("/stop")
    public String stop() {
        log.info("stop experiment.");
        userService.stop();
        return "success\n";
    }

    @GetMapping("/restart")
    public String restart() {
        log.info("restart experiment.");
//        edgeConfigService.resetTaskRate();
        userService.restart();
        return "success\n";
    }
}
