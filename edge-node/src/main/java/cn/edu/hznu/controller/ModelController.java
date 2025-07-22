package cn.edu.hznu.controller;

import cn.edu.hznu.agent.Agent;
import cn.edu.hznu.service.EdgeNodeSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Slf4j
@Lazy
public class ModelController {

    @Autowired
    private Agent agent;

    @Value("${spring.application.name}")
    public String name;

    @Resource
    EdgeNodeSystemService edgeNodeSystemService;

    @GetMapping("/loadAgent")
    public String loadAgent(@RequestParam String flag) {
        agent.loadModel(flag, Integer.parseInt(name.substring(10)));
        return "success";
    }

    @GetMapping("/initAgent")
    public String initAgent(@RequestParam String flag) {
        agent.saveModel(flag, Integer.parseInt(name.substring(10)));
        return "success";
    }

    @GetMapping("/saveModel")
    public String saveModel(@RequestParam String flag) {
        agent.saveModel(flag, Integer.parseInt(name.substring(10)));
        return "success";
    }

    @GetMapping("/train")
    public String train() {
        agent.train(edgeNodeSystemService.isCollecting());
        return "success";
    }
}