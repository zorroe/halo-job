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

    /**
     * 构造一次执行成功的响应结果。
     *
     * @param request 原始请求
     * @param message 响应消息
     * @param costTime 执行耗时
     * @return 成功响应
     */
    public static TriggerJobResponse success(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.SUCCESS, true, message, costTime);
    }

    /**
     * 构造一次执行失败的响应结果。
     *
     * @param request 原始请求
     * @param message 响应消息
     * @param costTime 执行耗时
     * @return 失败响应
     */
    public static TriggerJobResponse failed(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.FAILED, false, message, costTime);
    }

    /**
     * 构造一次因阻塞策略被丢弃的响应结果。
     *
     * @param request 原始请求
     * @param message 响应消息
     * @param costTime 执行耗时
     * @return 丢弃响应
     */
    public static TriggerJobResponse discarded(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.DISCARDED, false, message, costTime);
    }

    /**
     * 构造一次执行被取消的响应结果。
     *
     * @param request 原始请求
     * @param message 响应消息
     * @param costTime 执行耗时
     * @return 取消响应
     */
    public static TriggerJobResponse canceled(TriggerJobRequest request, String message, long costTime) {
        return build(request, TriggerRunStatusEnum.CANCELED, false, message, costTime);
    }

    /**
     * 统一填充执行结果中的公共字段。
     *
     * @param request 原始请求
     * @param status 执行状态
     * @param success 是否成功
     * @param message 响应消息
     * @param costTime 执行耗时
     * @return 标准响应对象
     */
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
