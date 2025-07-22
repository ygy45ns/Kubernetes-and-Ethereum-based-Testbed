package cn.edu.hznu.queue;

import cn.edu.hznu.bean.Task;
import cn.edu.hznu.thread.TransmissionRunnable;
import cn.edu.hznu.utils.SpringBeanUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TransmissionQueue {
    // 基于线程池的传输队列，用于节点的任务传输管理
    // 只允许 1 个线程并行传输任务，即使任务堆积，线程数也不会增加，所有任务都必须排队传输
    ThreadPoolExecutor thread = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    public void add(Task task) {
        TransmissionRunnable runnable = SpringBeanUtils.applicationContext.getBean(TransmissionRunnable.class);
        runnable.setTask(task);
        thread.execute(runnable);
    }
}
