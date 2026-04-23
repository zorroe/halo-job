package com.zorroe.cloud.job.core.model;

import lombok.Data;

@Data
public class TriggerJobRequest {

    private Long jobId;

    private String jobName;

    private String handler;

    private String param;

    private Integer blockStrategy;

    private Integer shardIndex = 0;

    private Integer shardTotal = 1;

    private String executorAddress;

    private String triggerType;
}
