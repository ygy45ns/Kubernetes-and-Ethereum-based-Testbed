package cn.edu.hznu.bean;

import lombok.Data;

@Data
public class Link {
    private Integer id;
    private String source;
    private String destination;
    private Double transmissionRate;
    private Double transmissionFailureRate;
}
