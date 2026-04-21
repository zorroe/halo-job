package com.zorroe.cloud.job.admin.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ExecutorInfo {
    private Long id;
    private String executorName;
    private String executorAddress;
    private Date heartbeatTime;
    private Integer status;
    private Date createTime;
}
