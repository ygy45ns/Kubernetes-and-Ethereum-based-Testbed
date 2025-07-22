package cn.edu.hznu.controller;

import cn.edu.hznu.service.BlockchainService;
import cn.edu.hznu.service.EdgeNodeSystemService;
import cn.edu.hznu.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.List;

@Lazy
@RestController
@RequestMapping(value = "/cmd", method = {RequestMethod.GET, RequestMethod.POST})
public class CmdController {

    @Value("${spring.application.name}")
    public String name;
    @Resource
    EdgeNodeSystemService edgeNodeSystemService;

    @Resource
    BlockchainService blockchainService;

    @Autowired
    private TransitionService transitionService;

    @GetMapping("/init")
    public String init() {
        edgeNodeSystemService.init();
        return "success";
    }

    @GetMapping("/initMaliciousNodes")
    public String initMaliciousNodes(@RequestParam List<String> maliciousList) {
        System.out.println("malicious list: " + maliciousList);
        edgeNodeSystemService.setMaliciousList(maliciousList);
        return "success";
    }

    @GetMapping("/initReputation")
    public String initReputation() throws Exception {
        blockchainService.initAccount();
        return blockchainService.callInitializeNode();
    }

    @GetMapping("/getReputations")
    public String getReputations() throws Exception {
        List<BigInteger> bigIntegers = blockchainService.callGetReputations();
        return bigIntegers.toString();
    }

    @GetMapping("/addData")
    public String addData() {
        transitionService.addData();
        return "success";
    }

    @GetMapping("/changeToTest")
    public String changeToTest() {
        edgeNodeSystemService.setTraining(false);// non-training
        return "success";
    }

    @GetMapping("/startLearn")
    public String startLearn() {
        edgeNodeSystemService.setStartLearn(true);
        return "success";
    }

    @GetMapping("/changeToTrain")
    public String changeToTrain() {
        edgeNodeSystemService.setTraining(true);// training
        return "success";
    }

    @GetMapping("/collectOfflineDataMode")
    public String collectOfflineDataMode() {
        edgeNodeSystemService.setCollecting(true);
        return "success";
    }

    @GetMapping("/onlineTrainMode")
    public String onlineTrainMode() {
        edgeNodeSystemService.setCollecting(false);
        transitionService.changeToOnlineTrainMode();
        return "success";
    }

    @GetMapping("/doMaliciousBehavior")
    public void doMaliciousBehavior(@RequestParam String address, BigInteger Wd) throws Exception {
        blockchainService.callUpdateReputation(address, "", false, Wd);
    }

    @GetMapping("/doHonestBehavior")
    public void doHonestBehavior(@RequestParam String address, BigInteger Wd) throws Exception {
        blockchainService.callUpdateReputation(address, "", true, Wd);
    }
}
