package cn.edu.hznu.controller;

import cn.edu.hznu.bean.Task;
import cn.edu.hznu.service.EdgeNodeSystemService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/user", method = {RequestMethod.GET, RequestMethod.POST})
@CommonsLog
public class UserController {
    @Resource
    EdgeNodeSystemService edgeNodeSystemService;

    @PostMapping("/task")
    public String receiveUserTask(@RequestBody Task task) throws Exception {
        // 接收处理用户任务
        log.info("==============================");
        log.info("receive task from user");
        task.setArrivalTime(LocalDateTime.now());
        edgeNodeSystemService.processUserTask(task);
        return "success";
    }
}
