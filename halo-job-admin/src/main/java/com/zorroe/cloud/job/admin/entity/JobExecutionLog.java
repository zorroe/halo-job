package com.zorroe.cloud.job.admin.entity;

import lombok.Data;

import java.util.Date;

@Data
public class JobExecutionLog {
    private Long id;
    private Long jobId;
    private String jobName;
    private String executorHandler;
    private String executorParam;
    private Integer status; // 1-成功，0-失败
    private String errorMsg;
    private Long executionTime;
    private Date startTime;
    private Date endTime;
    private Date createTime;
}