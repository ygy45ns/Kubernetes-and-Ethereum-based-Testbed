package cn.edu.hznu.queue;

import cn.edu.hznu.bean.Task;
import cn.edu.hznu.thread.ExecutionRunnable;
import cn.edu.hznu.utils.SpringBeanUtils;
import lombok.Getter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutionQueue {
    // 基于线程池的执行队列，用于节点的任务执行管理
    // 只允许 1 个线程并行执行任务，即使任务堆积，线程数也不会增加，所有任务都必须排队执行
    @Getter
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    public void add(Task task) {
        // ExecutionRunnable 是任务执行类，封装了任务的执行逻辑
        ExecutionRunnable runnable = SpringBeanUtils.applicationContext.getBean(ExecutionRunnable.class);
        runnable.setTask(task);
        executor.execute(runnable); // 将任务放入队列，等待执行
    }

    public int getSize() {
        return executor.getQueue().size();
    }

    public int getActiveSize() {
        if (executor.getQueue().isEmpty()) {
            return 0;
        } else {
            return 1;
        }
    }
}
