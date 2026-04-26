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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RestController
public class ExecutorController {

    private final ExecutorJobRunner executorJobRunner;

    public ExecutorController(ExecutorJobRunner executorJobRunner) {
        this.executorJobRunner = executorJobRunner;
    }

    /**
     * 接收新版 POST 调度请求，并返回结构化执行结果。
     *
     * @param request 调度中心下发的任务请求
     * @return 执行结果
     */
    @PostMapping("/executor/run")
    public Result<TriggerJobResponse> run(@RequestBody TriggerJobRequest request) {
        return Result.success(execute(request));
    }

    /**
     * 校验并标准化请求后，把任务交给运行器按阻塞策略执行。
     *
     * @param request 原始触发请求
     * @return 执行结果
     */
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

    /**
     * 补齐默认字段，保证后续执行阶段能够按统一规则处理请求。
     *
     * @param request 原始触发请求
     * @return 标准化后的请求
     */
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

    /**
     * 绑定任务上下文并反射调用实际任务方法，把异常转换为统一执行结果。
     *
     * @param holder 任务方法持有对象
     * @param request 标准化后的请求
     * @return 执行结果
     */
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

    /**
     * 按 Halo-Job 约定的签名规则反射调用任务方法。
     *
     * @param method 目标方法
     * @param bean 目标 Bean
     * @param param 字符串参数
     * @param context 任务上下文
     * @return 方法返回值
     * @throws Exception 反射调用异常
     */
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

    /**
     * 从异常链中提取更适合返回给调度中心的错误文案。
     *
     * @param throwable 原始异常
     * @return 错误文案
     */
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
