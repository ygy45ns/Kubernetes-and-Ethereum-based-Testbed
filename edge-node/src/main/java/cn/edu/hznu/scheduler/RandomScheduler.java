package cn.edu.hznu.scheduler;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Random;

@Lazy
@Component
@Setter
public class RandomScheduler implements IScheduler {
    @Autowired
    private Random schedulerRandom;

    @Value("${edgeComputing.edgeNodeNumber}")
    private int edgeNodeNumber;

    @Override
    public String selectAction(Long taskId) {
        var services = new ArrayList<String>(); // 创建 services 列表，存储所有边缘节点名称
        for (int i = 1; i <= edgeNodeNumber; i++) {
            services.add(String.format("edge-node-%d", i));
        }
        int index = schedulerRandom.nextInt(services.size()); // 生成随机索引，选择目标服务器
        return services.get(index);
    }
}
