package com.zorroe.cloud.job.core.anno;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HaloJob {

    /**
     * 定义任务处理器名称，调度中心通过该名称路由到具体方法。
     *
     * @return 任务 handler 标识
     */
    String value();

    /**
     * 为任务补充说明信息，便于后续管理端展示和维护。
     *
     * @return 任务描述
     */
    String desc() default "";
}
