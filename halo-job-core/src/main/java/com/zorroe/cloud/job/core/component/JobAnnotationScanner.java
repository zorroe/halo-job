package com.zorroe.cloud.job.core.component;

import com.zorroe.cloud.job.core.anno.HaloJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

public class JobAnnotationScanner implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        for (Object bean : applicationContext.getBeansOfType(Object.class).values()) {
            Class<?> clazz = bean.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                HaloJob anno = method.getAnnotation(HaloJob.class);
                if (anno != null) {
                    String handler = anno.value();
                    JobMethodRegistry.register(handler, bean, method);
                    System.out.println("【Halo-Job-Core】注册任务：handler = " + handler);
                }
            }
        }
    }
}