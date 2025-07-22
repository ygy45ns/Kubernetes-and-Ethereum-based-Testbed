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
 * SAC
 */
@Component
@Slf4j
@Lazy
public class SACTrainingRunner implements IRunner {

    @Value("${edgeComputing.episodeNumber}")
    private int episodeNumber;

    @Value("${rl.collect-offline-data-episode-number}")
    private int collectRound;

    @Value("${rl.offline-data-train-number}")
    private int offlineTrainNumber;

    @Value("${rl.name}")
    private String rlName;

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
        log.info("run SAC!");
        log.info("========================");
        var flag = DateTimeUtils.getFlag() + "_" + rlName;
        // remove data
        linkService.remove(null);
        edgeNodeService.remove(null);
        taskService.remove(null);
        // init edge node, link and agent model
        runnerService.init();
        runnerService.initAgent(flag);

        // collect offline data
        runnerService.collectOfflineDataMode();
        for (int i = 1; i <= collectRound; i++) {
            runnerService.run();
            var successRate = taskService.getSuccessRate();
            log.info("collecting round {}, success rate {}", i, successRate);
            runnerService.addData();
            taskService.remove(null);
//            runnerService.reset();
        }
        // offline train
        for (int i = 0; i < offlineTrainNumber; i++) {
            runnerService.train();
        }

        // online train
        runnerService.onlineTrainMode();
        var episodes = new ArrayList<Double>();
        var successRates = new ArrayList<Double>();
        var rewardsList = new ArrayList<Double>();
        var stdList = new ArrayList<Double>();
        for (int currentEpisode = 1; currentEpisode <= episodeNumber; currentEpisode++) {

            // test performance
//            if ((currentEpisode - 1) % testFrequency == 0) {
//                log.info("start to test.");
//                runnerService.test(flag);
//                episodes.add((double) currentEpisode);
//                successRates.add(runnerService.getSuccessRateAvg());
//                stdList.add(runnerService.getSuccessRateStd());
//                log.info("end test.");
//            }

            // run episode
            runnerService.run();
            var rewards = taskService.getReward();
            var successRate = taskService.getSuccessRate();
            episodes.add((double) currentEpisode);
            successRates.add(successRate);
            rewardsList.add(rewards);
            log.info("training episode {}, rewards {}", currentEpisode, rewards);
            log.info("training episode {}, success rate {}", currentEpisode, successRate);
            // process data
            runnerService.addData();
            runnerService.train();
            taskService.remove(null);
//            runnerService.reset();
        }
        runnerService.saveModel(flag);

        // save
        var x = ArrayUtils.toDoubleArray(episodes);
        plotService.saveRewards(rewardsList, flag);
        plotService.saveSuccessRates(x, ArrayUtils.toDoubleArray(successRates), flag);
//        plotService.saveSuccessRatesStds(x, ArrayUtils.toDoubleArray(stdList), flag);
    }
}
