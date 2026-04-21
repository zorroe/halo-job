package com.zorroe.cloud.job.core.anno;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HaloJob {

    // 任务名称（handler）
    String value();

    // 任务描述
    String desc() default "";
}
