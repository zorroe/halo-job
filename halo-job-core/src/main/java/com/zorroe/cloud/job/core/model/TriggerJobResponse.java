package com.zorroe.cloud.job.core.model;

import lombok.Data;

@Data
public class TriggerJobResponse {

    private boolean success;

    private TriggerRunStatusEnum status;

    private String message;

    private String executorAddress;

    private Long jobId;

    private String jobName;

    private String handler;

    private Integer shardIndex;

    private Integer shardTotal;

    private Long costTime;

    public static TriggerJobResponse success(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.SUCCESS, true, message, costTime);
    }

    public static TriggerJobResponse failed(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.FAILED, false, message, costTime);
    }

    public static TriggerJobResponse discarded(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.DISCARDED, false, message, costTime);
    }

    public static TriggerJobResponse canceled(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.CANCELED, false, message, costTime);
    }

    private static TriggerJobResponse build(
            TriggerJobRequest request,
            TriggerRunStatusEnum status,
            boolean success,
            String message,
            long costTime
    ) {
        TriggerJobResponse response = new TriggerJobResponse();
        response.setSuccess(success);
        response.setStatus(status);
        response.setMessage(message == null || message.isBlank() ? status.name() : message);
        response.setCostTime(costTime);
        if (request != null) {
            response.setExecutorAddress(request.getExecutorAddress());
            response.setJobId(request.getJobId());
            response.setJobName(request.getJobName());
            response.setHandler(request.getHandler());
            response.setShardIndex(request.getShardIndex() == null ? 0 : request.getShardIndex());
            response.setShardTotal(request.getShardTotal() == null || request.getShardTotal() <= 0 ? 1 : request.getShardTotal());
        } else {
            response.setShardIndex(0);
            response.setShardTotal(1);
        }
        return response;
    }
}
