package com.zorroe.cloud.job.core.context;

import com.zorroe.cloud.job.core.model.TriggerJobRequest;
import lombok.Data;

@Data
public class HaloJobContext {

    private Long jobId;

    private String jobName;

    private String handler;

    private String param;

    private Integer shardIndex;

    private Integer shardTotal;

    private String executorAddress;

    private String triggerType;

    public static HaloJobContext fromRequest(TriggerJobRequest request) {
        HaloJobContext context = new HaloJobContext();
        if (request == null) {
            context.setShardIndex(0);
            context.setShardTotal(1);
            return context;
        }
        context.setJobId(request.getJobId());
        context.setJobName(request.getJobName());
        context.setHandler(request.getHandler());
        context.setParam(request.getParam());
        context.setShardIndex(request.getShardIndex() == null ? 0 : request.getShardIndex());
        context.setShardTotal(request.getShardTotal() == null || request.getShardTotal() <= 0 ? 1 : request.getShardTotal());
        context.setExecutorAddress(request.getExecutorAddress());
        context.setTriggerType(request.getTriggerType());
        return context;
    }
}
