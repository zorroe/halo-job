package com.zorroe.cloud.job.admin.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExecutorDispatchTarget {

    private String executorAddress;

    private Integer shardIndex;

    private Integer shardTotal;
}
