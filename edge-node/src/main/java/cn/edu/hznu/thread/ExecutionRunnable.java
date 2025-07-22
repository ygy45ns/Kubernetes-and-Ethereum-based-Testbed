package cn.edu.hznu.thread;

import cn.edu.hznu.bean.EdgeNodeSystem;
import cn.edu.hznu.bean.StoreConstants;
import cn.edu.hznu.bean.Task;
import cn.edu.hznu.bean.TaskStatus;
import cn.edu.hznu.service.BlockchainService;
import cn.edu.hznu.service.EdgeNodeSystemService;
import cn.edu.hznu.service.TaskService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;

@Component
@Setter
@Scope("prototype")
@RefreshScope
public class ExecutionRunnable implements Runnable {

    // prototype
    @Getter
    private Task task;

    @Autowired
    private TaskService taskService;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Autowired
    private Random reliabilityRandom;

    @Resource
    BlockchainService blockchainService;

    @Resource
    EdgeNodeSystemService edgeNodeSystemService;

    @Value("${edgeComputing.maxTaskSize}")
    private int maxTaskSize;

    @Value("${edgeComputing.maxTaskComplexity}")
    private int maxTaskComplexity;

    @Autowired
    private RestTemplate restTemplate;

    public void updateNegativeReputation() {
        if (!Objects.equals(task.getSource(), task.getDestination())) {
            BigInteger Wd = BigInteger.valueOf(100 * (task.getCpuCycle() / StoreConstants.Kilo.value / StoreConstants.Byte.value) / ((long) maxTaskSize * maxTaskComplexity));
            String address = blockchainService.getAddress();
            String url = String.format("http://%s/cmd/doMaliciousBehavior?address=" + address + "&Wd=" + Wd, task.getSource());
            restTemplate.getForObject(url, String.class);
        }
    }

    public void updatePositiveReputation() {
        if (!Objects.equals(task.getSource(), task.getDestination())) {
            BigInteger Wd = BigInteger.valueOf(100 * (task.getCpuCycle() / StoreConstants.Kilo.value / StoreConstants.Byte.value) / ((long) maxTaskSize * maxTaskComplexity));
            String address = blockchainService.getAddress();
            String url = String.format("http://%s/cmd/doHonestBehavior?address=" + address + "&Wd=" + Wd, task.getSource());
            restTemplate.getForObject(url, String.class);
        }
    }

    @Override
    public void run() {
        task.setBeginExecutionTime(LocalDateTime.now());
        task.setExecutionWaitingTime(Duration.between(task.getEndTransmissionTime(), task.getBeginExecutionTime()).toMillis());
        long estimatedTotalTime = task.getTransmissionWaitingTime() + task.getTransmissionTime() + task.getExecutionWaitingTime() + task.getExecutionTime();
        if (estimatedTotalTime > task.getDeadline()) {
            task.setStatus(TaskStatus.DROP);
            taskService.updateById(task);
            updateNegativeReputation();
            return;
        }

        try {
            Thread.sleep(task.getExecutionTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        task.setEndExecutionTime(LocalDateTime.now());

        double reliability = Math.exp(-task.getExecutionTime() / 1000.0 * edgeNodeSystem.getEdgeNode().getExecutionFailureRate());
        if (reliabilityRandom.nextDouble() > reliability) {
            task.setStatus(TaskStatus.EXECUTION_FAILURE);
            updateNegativeReputation();
        } else {
            task.setStatus(TaskStatus.SUCCESS);
            updatePositiveReputation();

        }
        taskService.updateById(task);
    }
}
