package com.zorroe.cloud.job.admin.entity;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 任务实体类（对应job_info表）
 */
@Data
@ToString
public class JobInfo {

    private Long id;

    private String jobName;            // 任务名称

    private String executorHandler;    // 执行器处理器（执行器要执行的任务名）

    private String executorParam;      // 执行参数

    private Integer jobStatus;         // 任务状态

    private String cronExpression;

    private Long nextExecuteTime;     // 下次执行时间

    private String remark;

    private Integer routeStrategy;

    private Integer blockStrategy;

    private Integer retryCount;

    private Date createTime;

    private Date updateTime;
}