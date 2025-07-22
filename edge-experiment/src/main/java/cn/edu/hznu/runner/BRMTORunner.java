package cn.edu.hznu.runner;


import cn.edu.hznu.service.*;
import cn.edu.hznu.util.ArrayUtils;
import cn.edu.hznu.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * BRMTO
 */
@Component
@Slf4j
@Lazy
public class BRMTORunner implements IRunner {

    @Value("${edgeComputing.episodeNumber}")
    private int episodeNumber;

    @Value("${edgeComputing.name}")
    private String name;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int agentNumber;


    @Autowired
    private TaskService taskService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private PlotService plotService;

    @Autowired
    private RunnerService runnerService;

    @Value("${edgeComputing.flag}")
    private String flag;

    @Value("${edgeComputing.testFrequency}")
    private int testFrequency;

    public void run() {
        log.info("========================");
        log.info("run BRMTO!");
        log.info("========================");
        var flag = DateTimeUtils.getFlag() + "_" + name;
        // remove data
        linkService.remove(null);
        edgeNodeService.remove(null);
        taskService.remove(null);

        // init edge node and link
        runnerService.init();

        var episodes = new ArrayList<Double>();
        var successRates = new ArrayList<Double>();
        var rewardsList = new ArrayList<Double>();
        for (int currentEpisode = 1; currentEpisode <= episodeNumber; currentEpisode++) {

            // test performance
//            if (currentEpisode % testFrequency == 0) {
//                log.info("start to test.");
//                var successRate = runnerService.test(flag);
//                episodes.add((double) currentEpisode);
//                successRates.add(successRate);
//                log.info("end test.");
//            }

            // run episode
            runnerService.run();
            var rewards = taskService.getReward();
            var successRate = taskService.getSuccessRate();
            episodes.add((double) currentEpisode);
            successRates.add(successRate);
            log.info("running episode {}, rewards {}", currentEpisode, rewards);
            log.info("running episode {}, success rate {}", currentEpisode, successRate);
            rewardsList.add(rewards);

            taskService.remove(null);
//            runnerService.reset();
        }
        // save
        plotService.saveSuccessRates(ArrayUtils.toDoubleArray(episodes), ArrayUtils.toDoubleArray(successRates), flag);
        plotService.saveRewards(rewardsList, flag);
    }
}
