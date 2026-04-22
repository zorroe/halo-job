package com.zorroe.cloud.job.core.config;

import com.zorroe.cloud.job.core.component.ExecutorAutoRegister;
import com.zorroe.cloud.job.core.component.JobAnnotationScanner;
import com.zorroe.cloud.job.core.controller.ExecutorController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ExecutorAutoConfig {

    // 1. 注册任务扫描器
    @Bean
    public JobAnnotationScanner jobAnnotationScanner() {
        return new JobAnnotationScanner();
    }

    // 2. 注册执行器自动注册+心跳
    @Bean
    public ExecutorAutoRegister executorAutoRegister() {
        return new ExecutorAutoRegister();
    }

    // 3. 注册核心 Controller（/executor/run）
    @Bean
    public ExecutorController executorController() {
        return new ExecutorController();
    }
}