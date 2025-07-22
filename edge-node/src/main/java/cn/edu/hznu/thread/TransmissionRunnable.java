package cn.edu.hznu.thread;

import cn.edu.hznu.bean.EdgeNodeSystem;
import cn.edu.hznu.bean.Link;
import cn.edu.hznu.bean.Task;
import cn.edu.hznu.bean.TaskStatus;
import cn.edu.hznu.service.TaskService;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@Scope("prototype")
@Setter
@RefreshScope
@CommonsLog
public class TransmissionRunnable implements Runnable {
    // prototype
    private Task task;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Autowired
    private Random reliabilityRandom;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TaskService taskService;

    @Override
    public void run() {
        task.setBeginTransmissionTime(LocalDateTime.now());
        task.setTransmissionWaitingTime(Duration.between(task.getArrivalTime(), task.getBeginTransmissionTime()).toMillis());
        try {
            Thread.sleep(task.getTransmissionTime()); // 线程睡眠 task.getTransmissionTime() 毫秒，模拟任务的传输时间
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        task.setEndTransmissionTime(LocalDateTime.now());

        // 如果随机值大于reliability，任务传输失败
        String destination = task.getDestination();
        Link link = edgeNodeSystem.getLinkMap().get(destination);
        Double transmissionFailureRate = link.getTransmissionFailureRate();
        double reliability = Math.exp(-task.getTransmissionTime() / 1000.0 * transmissionFailureRate);
        if (reliabilityRandom.nextDouble() > reliability) {
            task.setStatus(TaskStatus.TRANSMISSION_FAILURE);
            // fixed null type exception
            task.setExecutionWaitingTime(0L);
            task.setExecutionTime(0L);
            taskService.updateById(task);
        } else {
            // 传输成功，发送到目标节点
            String url = String.format("http://%s/edgeNode/task", destination);
            restTemplate.postForObject(url, task, String.class);
        }
    }
}
