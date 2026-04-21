package com.zorroe.cloud.job.core.component;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobMethodRegistry {
    private static final Map<String, JobMethodHolder> JOB_HANDLER_MAP = new ConcurrentHashMap<>();

    public static void register(String handler, Object bean, Method method) {
        JOB_HANDLER_MAP.put(handler, new JobMethodHolder(bean, method));
    }

    public static JobMethodHolder get(String handler) {
        return JOB_HANDLER_MAP.get(handler);
    }

    public static class JobMethodHolder {
        private final Object bean;
        private final Method method;

        public JobMethodHolder(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }

        // getter
        public Object getBean() { return bean; }
        public Method getMethod() { return method; }
    }
}