package com.zorroe.cloud.job.core.anno;

import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ComponentScan("com.zorroe.cloud.job.core") // 这里扫 core 包
public @interface EnableHaloJobExecutor {

}
