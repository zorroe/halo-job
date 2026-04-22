package com.zorroe.cloud.job.core.anno;

import com.zorroe.cloud.job.core.config.ExecutorAutoConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ExecutorAutoConfig.class) // 这里扫 core 包
public @interface EnableHaloJobExecutor {

}
