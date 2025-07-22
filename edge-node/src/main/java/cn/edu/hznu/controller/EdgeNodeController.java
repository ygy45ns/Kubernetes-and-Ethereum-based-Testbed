package cn.edu.hznu.controller;

import cn.edu.hznu.bean.EdgeNode;
import cn.edu.hznu.bean.RatcVo;
import cn.edu.hznu.bean.Task;
import cn.edu.hznu.service.EdgeNodeSystemService;
import cn.edu.hznu.thread.ExecutionRunnable;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping(value = "/edgeNode", method = {RequestMethod.GET, RequestMethod.POST})
@CommonsLog
public class EdgeNodeController {

    @Resource
    private EdgeNodeSystemService edgeNodeSystemService;

    @PostMapping("/task")
    public String receiveEdgeNodeTask(@RequestBody Task task) throws Exception {
        // 表示从边缘节点接收到的任务
        log.info("receive task from edge node: " + task.getSource());
        // 任务处理
        edgeNodeSystemService.processEdgeNodeTask(task);
        return "success";
    }

    @GetMapping("/queue")
    public Integer queue() {
        // 获取执行队列的大小
        return edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getSize();
    }

    @GetMapping("/info")
    public EdgeNode info() {
        // 获取EdgeNode实例，表示边缘节点的具体信息（如节点状态、名称等）
        return edgeNodeSystemService.getEdgeNodeSystem().getEdgeNode();
    }

    @GetMapping("/waitingTime")
    public Long waitingTime() {
        var queue = edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getExecutor().getQueue();
        long waitingTime = 0;
        for (Runnable runnable : queue) {
            var r = (ExecutionRunnable) runnable;
            waitingTime += r.getTask().getExecutionTime();
        }
        return waitingTime;
    }

    @GetMapping("/ratc")
    public RatcVo ratc() {
        var queue = edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getExecutor().getQueue();
        long waitingTime = 0;
        for (Runnable runnable : queue) {
            var r = (ExecutionRunnable) runnable;
            waitingTime += r.getTask().getExecutionTime();
        }
        var res = new RatcVo();
        res.setWaitingTime(waitingTime);
        var edgeNode = edgeNodeSystemService.getEdgeNodeSystem().getEdgeNode();
        res.setExecutionFailureRate(edgeNode.getExecutionFailureRate());
        res.setCapacity(edgeNode.getCapacity());
        res.setEdgeId(edgeNode.getName());
        return res;
    }

    @GetMapping("/available")
    public Integer avail() {
//        查询队列是否可用
        int queueSize = edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getSize();
        if (queueSize < edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueueThreshold()) {
            return 1;
        } else {
            return 0;
        }
    }
}