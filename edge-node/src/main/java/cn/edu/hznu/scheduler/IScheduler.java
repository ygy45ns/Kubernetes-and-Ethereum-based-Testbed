package cn.edu.hznu.scheduler;

public interface IScheduler {
    String selectAction(Long taskId) throws Exception;
}
