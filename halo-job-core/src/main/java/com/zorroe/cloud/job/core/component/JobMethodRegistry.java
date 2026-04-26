package com.zorroe.cloud.job.core.component;

import com.zorroe.cloud.job.core.model.ExecutorHandlerDefinition;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobMethodRegistry {
    private static final Map<String, JobMethodHolder> JOB_HANDLER_MAP = new ConcurrentHashMap<>();

    /**
     * 注册任务 handler 与具体 Bean 方法的映射关系。
     *
     * @param handler 任务处理器名称
     * @param description 任务描述
     * @param methodSignature 方法签名摘要
     * @param bean 方法所属 Bean
     * @param method 任务处理方法
     */
    public static void register(String handler, String description, String methodSignature, Object bean, Method method) {
        JOB_HANDLER_MAP.put(handler, new JobMethodHolder(handler, description, methodSignature, bean, method));
    }

    /**
     * 根据 handler 获取已注册的方法封装。
     *
     * @param handler 任务处理器名称
     * @return 方法持有对象，不存在时返回 {@code null}
     */
    public static JobMethodHolder get(String handler) {
        return JOB_HANDLER_MAP.get(handler);
    }

    /**
     * 返回当前执行器已注册的所有 handler 元数据，供自动注册时上报给调度中心。
     *
     * @return handler 元数据列表
     */
    public static List<ExecutorHandlerDefinition> listHandlerDefinitions() {
        return JOB_HANDLER_MAP.values().stream()
                .sorted(Comparator.comparing(JobMethodHolder::getHandlerName))
                .map(holder -> new ExecutorHandlerDefinition(
                        holder.getHandlerName(),
                        holder.getDescription(),
                        holder.getMethodSignature()
                ))
                .toList();
    }

    public static class JobMethodHolder {
        private final String handlerName;
        private final String description;
        private final String methodSignature;
        private final Object bean;
        private final Method method;

        /**
         * 封装任务方法与对应 Bean，便于执行阶段统一反射调用。
         *
         * @param handlerName handler 名称
         * @param description 任务描述
         * @param methodSignature 方法签名摘要
         * @param bean Spring Bean 实例
         * @param method 任务处理方法
         */
        public JobMethodHolder(String handlerName, String description, String methodSignature, Object bean, Method method) {
            this.handlerName = handlerName;
            this.description = description;
            this.methodSignature = methodSignature;
            this.bean = bean;
            this.method = method;
        }

        public String getHandlerName() {
            return handlerName;
        }

        public String getDescription() {
            return description;
        }

        public String getMethodSignature() {
            return methodSignature;
        }

        /**
         * 获取任务方法所属的 Bean 实例。
         *
         * @return Bean 实例
         */
        public Object getBean() { return bean; }

        /**
         * 获取实际需要调用的方法对象。
         *
         * @return 方法对象
         */
        public Method getMethod() {
            return method;
        }
    }
}
