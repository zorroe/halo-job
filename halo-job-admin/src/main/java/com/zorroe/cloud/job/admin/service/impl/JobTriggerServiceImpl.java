package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.JobExecutionLog;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.model.ExecutorDispatchTarget;
import com.zorroe.cloud.job.admin.model.JobTriggerSummary;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.admin.service.JobExecutionLogService;
import com.zorroe.cloud.job.admin.service.JobTriggerService;
import com.zorroe.cloud.job.core.common.BlockStrategyEnum;
import com.zorroe.cloud.job.core.common.Result;
import com.zorroe.cloud.job.core.model.TriggerJobRequest;
import com.zorroe.cloud.job.core.model.TriggerJobResponse;
import com.zorroe.cloud.job.core.model.TriggerRunStatusEnum;
import com.zorroe.cloud.job.core.model.TriggerTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class JobTriggerServiceImpl implements JobTriggerService {

    private static final ParameterizedTypeReference<Result<TriggerJobResponse>> EXECUTOR_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Resource
    private ExecutorInfoService executorInfoService;

    @Resource
    private JobExecutionLogService jobExecutionLogService;

    @Resource
    private RestTemplate restTemplate;

    @Override
    public JobTriggerSummary trigger(JobInfo jobInfo, TriggerTypeEnum triggerType) {
        List<ExecutorDispatchTarget> targets = executorInfoService.route(jobInfo);
        if (targets == null || targets.isEmpty()) {
            saveNoExecutorLog(jobInfo);
            return new JobTriggerSummary(false, 0, 0, 1, "没有可用的执行器");
        }

        int successCount = 0;
        List<String> failures = new ArrayList<>();

        for (ExecutorDispatchTarget target : targets) {
            TriggerJobResponse response = dispatchSingle(jobInfo, target, triggerType);
            if (response.isSuccess()) {
                successCount++;
            } else {
                failures.add(buildFailureMessage(target, response));
            }
        }

        int total = targets.size();
        int failCount = total - successCount;
        boolean success = failCount == 0;
        String message = success
                ? String.format("任务触发成功，共分发到 %d 个执行器", total)
                : String.format("任务触发完成，成功 %d/%d，失败详情：%s", successCount, total, String.join(" | ", failures));

        return new JobTriggerSummary(success, total, successCount, failCount, message);
    }

    private TriggerJobResponse dispatchSingle(JobInfo jobInfo, ExecutorDispatchTarget target, TriggerTypeEnum triggerType) {
        TriggerJobRequest request = buildRequest(jobInfo, target, triggerType);
        JobExecutionLog executionLog = createExecutionLog(jobInfo);
        long startTime = System.currentTimeMillis();
        executionLog.setStartTime(new Date(startTime));

        TriggerJobResponse response = null;
        try {
            response = dispatchWithRetry(jobInfo, request);
            executionLog.setStatus(response.isSuccess() ? 1 : 0);
            executionLog.setErrorMsg(response.isSuccess() ? null : buildFailureMessage(target, response));
            return response;
        } catch (Exception e) {
            response = TriggerJobResponse.failed(request, resolveMessage(e), System.currentTimeMillis() - startTime);
            executionLog.setStatus(0);
            executionLog.setErrorMsg(buildFailureMessage(target, response));
            return response;
        } finally {
            executionLog.setEndTime(new Date());
            executionLog.setExecutionTime(System.currentTimeMillis() - startTime);
            saveExecutionLog(executionLog);
        }
    }

    private TriggerJobResponse dispatchWithRetry(JobInfo jobInfo, TriggerJobRequest request) {
        int maxRetry = jobInfo.getRetryCount() == null ? 0 : Math.max(jobInfo.getRetryCount(), 0);
        TriggerJobResponse lastResponse = null;

        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return TriggerJobResponse.canceled(request, "重试等待时线程被中断", 0L);
                }
            }

            lastResponse = dispatchOnce(request);
            if (lastResponse.isSuccess()) {
                return lastResponse;
            }
            if (lastResponse.getStatus() != TriggerRunStatusEnum.FAILED) {
                return lastResponse;
            }
        }

        return lastResponse == null ? TriggerJobResponse.failed(request, "执行器返回为空", 0L) : lastResponse;
    }

    private TriggerJobResponse dispatchOnce(TriggerJobRequest request) {
        ResponseEntity<Result<TriggerJobResponse>> responseEntity = restTemplate.exchange(
                request.getExecutorAddress() + "/executor/run",
                HttpMethod.POST,
                new HttpEntity<>(request),
                EXECUTOR_RESPONSE_TYPE
        );

        Result<TriggerJobResponse> body = responseEntity.getBody();
        if (body == null) {
            return TriggerJobResponse.failed(request, "执行器响应为空", 0L);
        }

        TriggerJobResponse response = body.getData();
        if (response == null) {
            return TriggerJobResponse.failed(request, body.getMsg(), 0L);
        }

        if (response.getExecutorAddress() == null || response.getExecutorAddress().isBlank()) {
            response.setExecutorAddress(request.getExecutorAddress());
        }
        if (response.getJobId() == null) {
            response.setJobId(request.getJobId());
        }
        if (response.getJobName() == null || response.getJobName().isBlank()) {
            response.setJobName(request.getJobName());
        }
        if (response.getHandler() == null || response.getHandler().isBlank()) {
            response.setHandler(request.getHandler());
        }
        if (response.getShardIndex() == null) {
            response.setShardIndex(request.getShardIndex());
        }
        if (response.getShardTotal() == null) {
            response.setShardTotal(request.getShardTotal());
        }
        return response;
    }

    private TriggerJobRequest buildRequest(JobInfo jobInfo, ExecutorDispatchTarget target, TriggerTypeEnum triggerType) {
        TriggerJobRequest request = new TriggerJobRequest();
        request.setJobId(jobInfo.getId());
        request.setJobName(jobInfo.getJobName());
        request.setHandler(jobInfo.getExecutorHandler());
        request.setParam(jobInfo.getExecutorParam());
        request.setBlockStrategy(BlockStrategyEnum.getByCode(jobInfo.getBlockStrategy()).getCode());
        request.setShardIndex(target.getShardIndex());
        request.setShardTotal(target.getShardTotal());
        request.setExecutorAddress(target.getExecutorAddress());
        request.setTriggerType(triggerType.name());
        return request;
    }

    private JobExecutionLog createExecutionLog(JobInfo jobInfo) {
        JobExecutionLog executionLog = new JobExecutionLog();
        executionLog.setJobId(jobInfo.getId());
        executionLog.setJobName(jobInfo.getJobName());
        executionLog.setExecutorHandler(jobInfo.getExecutorHandler());
        executionLog.setExecutorParam(jobInfo.getExecutorParam());
        return executionLog;
    }

    private void saveNoExecutorLog(JobInfo jobInfo) {
        JobExecutionLog executionLog = createExecutionLog(jobInfo);
        Date now = new Date();
        executionLog.setStatus(0);
        executionLog.setErrorMsg("没有可用的执行器");
        executionLog.setStartTime(now);
        executionLog.setEndTime(now);
        executionLog.setExecutionTime(0L);
        saveExecutionLog(executionLog);
    }

    private void saveExecutionLog(JobExecutionLog executionLog) {
        try {
            jobExecutionLogService.insertLog(executionLog);
        } catch (Exception e) {
            log.error("保存任务执行日志失败", e);
        }
    }

    private String buildFailureMessage(ExecutorDispatchTarget target, TriggerJobResponse response) {
        String address = target == null ? "unknown" : target.getExecutorAddress();
        int shardIndex = target == null || target.getShardIndex() == null ? 0 : target.getShardIndex();
        int shardTotal = target == null || target.getShardTotal() == null ? 1 : target.getShardTotal();
        String message = response == null ? "未知错误" : response.getMessage();
        return String.format("executor=%s, shard=%d/%d, message=%s", address, shardIndex + 1, shardTotal, message);
    }

    private String resolveMessage(Throwable throwable) {
        if (throwable == null) {
            return "任务执行失败";
        }
        Throwable target = throwable;
        while (target.getCause() != null && target.getCause() != target) {
            target = target.getCause();
        }
        if (target.getMessage() != null && !target.getMessage().isBlank()) {
            return target.getMessage();
        }
        return target.getClass().getSimpleName();
    }
}
