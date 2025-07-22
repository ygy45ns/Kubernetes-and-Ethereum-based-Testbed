package cn.edu.hznu.service;

import cn.edu.hznu.bean.Task;
import cn.edu.hznu.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TaskService extends ServiceImpl<TaskMapper, Task> {

    @Autowired
    private TaskMapper taskMapper;


    public double getReward() {
        Long successTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "SUCCESS"));
        Long emptyTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "EMPTY"));
        Long exeFailedTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "EXECUTION_FAILURE"));
        Long transFailedTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "TRANSMISSION_FAILURE"));
        Long maliciousFailedTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "MALICIOUS_FAILURE"));
        Long dropTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "DROP"));
        return 1.0f * (Double.valueOf(successTasks)) + (0.5f * Double.valueOf(emptyTasks)) + (-1.0f * (double) (exeFailedTasks + transFailedTasks + dropTasks + maliciousFailedTasks));
    }

    public double getSuccessRate() {
        List<String> completedStatuses = Arrays.asList(
                "SUCCESS",
                "EXECUTION_FAILURE",
                "TRANSMISSION_FAILURE",
                "DROP",
                "MALICIOUS_FAILURE"
        );
        Long successTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "SUCCESS"));
        Long totalTasks = taskMapper.selectCount(
                new QueryWrapper<Task>().in("status", completedStatuses)
        );
        return Double.valueOf(successTasks) / Double.valueOf(totalTasks);
    }
}
