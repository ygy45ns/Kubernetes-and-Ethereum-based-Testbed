package cn.edu.hznu.runner;


import cn.edu.hznu.service.*;
import cn.edu.hznu.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import cn.edu.hznu.util.ArrayUtils;

import java.util.ArrayList;

/**
 * DTTO
 */
@Component
@Slf4j
@Lazy
public class DTTOTrainingRunner implements IRunner {

    @Value("${edgeComputing.episodeNumber}")
    private int episodeNumber;

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
        log.info("run DTTO!");
        log.info("========================");
        var flag = DateTimeUtils.getFlag() + "_" + rlName;
        // remove data
        linkService.remove(null);
        edgeNodeService.remove(null);
        taskService.remove(null);
        // init edge node, link and agent model
        runnerService.init();
        runnerService.initAgent(flag);

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
            // training
            runnerService.train();
            taskService.remove(null);

//            runnerService.reset();
        }
        runnerService.saveModel(flag);

        // save
        var x = ArrayUtils.toDoubleArray(episodes);
        plotService.saveSuccessRates(x, ArrayUtils.toDoubleArray(successRates), flag);
//        plotService.saveSuccessRatesStds(x, ArrayUtils.toDoubleArray(stdList), flag);
        plotService.saveRewards(rewardsList, flag);
    }
}
