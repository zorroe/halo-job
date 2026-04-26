package com.zorroe.cloud.job.core.component;

import com.zorroe.cloud.job.core.anno.HaloJob;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JobAnnotationScanner implements ApplicationContextAware {

    /**
     * 容器启动完成后扫描所有 Bean，把标注了 {@link HaloJob} 的方法注册到任务仓库中。
     *
     * @param applicationContext Spring 上下文
     * @throws BeansException Spring 容器异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        for (Object bean : applicationContext.getBeansOfType(Object.class).values()) {
            Class<?> clazz = bean.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                HaloJob anno = method.getAnnotation(HaloJob.class);
                if (anno != null) {
                    String handler = anno.value();
                    JobMethodRegistry.register(
                            handler,
                            anno.desc(),
                            buildMethodSignature(method),
                            bean,
                            method
                    );
                    System.out.println("【Halo-Job-Core】注册任务：handler = " + handler);
                }
            }
        }
    }

    private String buildMethodSignature(Method method) {
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return method.getReturnType().getSimpleName() + " " + method.getName() + "(" + params + ")";
    }
}
