package com.zorroe.cloud.job.core.controller;

import com.zorroe.cloud.job.core.common.BlockStrategyEnum;
import com.zorroe.cloud.job.core.common.Result;
import com.zorroe.cloud.job.core.component.ExecutorJobRunner;
import com.zorroe.cloud.job.core.component.JobMethodRegistry;
import com.zorroe.cloud.job.core.context.HaloJobContext;
import com.zorroe.cloud.job.core.context.HaloJobContextHolder;
import com.zorroe.cloud.job.core.model.TriggerJobRequest;
import com.zorroe.cloud.job.core.model.TriggerJobResponse;
import com.zorroe.cloud.job.core.model.TriggerTypeEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RestController
public class ExecutorController {

    private final ExecutorJobRunner executorJobRunner;

    public ExecutorController(ExecutorJobRunner executorJobRunner) {
        this.executorJobRunner = executorJobRunner;
    }

    @PostMapping("/executor/run")
    public Result<TriggerJobResponse> run(@RequestBody TriggerJobRequest request) {
        return Result.success(execute(request));
    }

    @GetMapping("/executor/run")
    public Result<TriggerJobResponse> runLegacy(
            @RequestParam String handler,
            @RequestParam(required = false) String param
    ) {
        TriggerJobRequest request = new TriggerJobRequest();
        request.setHandler(handler);
        request.setParam(param);
        request.setJobName(handler);
        request.setBlockStrategy(BlockStrategyEnum.QUEUE_WAIT.getCode());
        request.setTriggerType(TriggerTypeEnum.MANUAL.name());
        return Result.success(execute(request));
    }

    private TriggerJobResponse execute(TriggerJobRequest request) {
        TriggerJobRequest normalizedRequest = normalizeRequest(request);
        if (normalizedRequest.getHandler() == null || normalizedRequest.getHandler().isBlank()) {
            return TriggerJobResponse.failed(normalizedRequest, "handler 不能为空", 0L);
        }

        JobMethodRegistry.JobMethodHolder holder = JobMethodRegistry.get(normalizedRequest.getHandler());
        if (holder == null) {
            return TriggerJobResponse.failed(normalizedRequest, "handler not found: " + normalizedRequest.getHandler(), 0L);
        }

        return executorJobRunner.run(normalizedRequest, () -> invokeHandler(holder, normalizedRequest));
    }

    private TriggerJobRequest normalizeRequest(TriggerJobRequest request) {
        TriggerJobRequest normalized = request == null ? new TriggerJobRequest() : request;
        normalized.setBlockStrategy(BlockStrategyEnum.getByCode(normalized.getBlockStrategy()).getCode());
        if (normalized.getShardIndex() == null) {
            normalized.setShardIndex(0);
        }
        if (normalized.getShardTotal() == null || normalized.getShardTotal() <= 0) {
            normalized.setShardTotal(1);
        }
        if (normalized.getJobName() == null || normalized.getJobName().isBlank()) {
            normalized.setJobName(normalized.getHandler());
        }
        if (normalized.getTriggerType() == null || normalized.getTriggerType().isBlank()) {
            normalized.setTriggerType(TriggerTypeEnum.MANUAL.name());
        }
        return normalized;
    }

    private TriggerJobResponse invokeHandler(JobMethodRegistry.JobMethodHolder holder, TriggerJobRequest request) {
        long startTime = System.currentTimeMillis();
        HaloJobContext context = HaloJobContext.fromRequest(request);
        HaloJobContextHolder.set(context);

        try {
            Method method = holder.getMethod();
            method.setAccessible(true);
            Object result = invokeMethod(method, holder.getBean(), request.getParam(), context);
            String message = result == null ? "success" : String.valueOf(result);
            return TriggerJobResponse.success(request, message, System.currentTimeMillis() - startTime);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                return TriggerJobResponse.canceled(request, resolveMessage(target), System.currentTimeMillis() - startTime);
            }
            return TriggerJobResponse.failed(request, resolveMessage(target), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                return TriggerJobResponse.canceled(request, resolveMessage(e), System.currentTimeMillis() - startTime);
            }
            return TriggerJobResponse.failed(request, resolveMessage(e), System.currentTimeMillis() - startTime);
        } finally {
            HaloJobContextHolder.clear();
        }
    }

    private Object invokeMethod(Method method, Object bean, String param, HaloJobContext context) throws Exception {
        Class<?>[] types = method.getParameterTypes();
        if (types.length == 0) {
            return method.invoke(bean);
        }
        if (types.length == 1 && types[0] == String.class) {
            return method.invoke(bean, param);
        }
        if (types.length == 1 && types[0] == HaloJobContext.class) {
            return method.invoke(bean, context);
        }
        if (types.length == 2 && types[0] == String.class && types[1] == HaloJobContext.class) {
            return method.invoke(bean, param, context);
        }
        throw new IllegalStateException("unsupported halo job method signature: " + method);
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
