package cn.edu.hznu.scheduler;

import cn.edu.hznu.bean.TaskStatus;
import cn.edu.hznu.service.TaskService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@Setter
@Slf4j
public class LOCALScheduler implements IScheduler {
    @Value("${spring.application.name}")
    public String name;

    @Autowired
    private TaskService taskService;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int edgeNodeNumber;

    @Override
    public String selectAction(Long taskId) {
        String obj = null;
        if (taskService.getById(taskId).getStatus().equals(TaskStatus.EMPTY)) {
            obj = "edge-node-" + (edgeNodeNumber + 1);
        } else {
            obj = name;
        }
        log.info("select " + obj);
        return obj;
    }
}