package com.zorroe.cloud.job.core.config;

import com.zorroe.cloud.job.core.component.ExecutorAutoRegister;
import com.zorroe.cloud.job.core.component.ExecutorJobRunner;
import com.zorroe.cloud.job.core.component.JobAnnotationScanner;
import com.zorroe.cloud.job.core.controller.ExecutorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorAutoConfig {

    @Bean
    public JobAnnotationScanner jobAnnotationScanner() {
        return new JobAnnotationScanner();
    }

    @Bean
    public ExecutorAutoRegister executorAutoRegister() {
        return new ExecutorAutoRegister();
    }

    @Bean
    public ExecutorJobRunner executorJobRunner() {
        return new ExecutorJobRunner();
    }

    @Bean
    public ExecutorController executorController(ExecutorJobRunner executorJobRunner) {
        return new ExecutorController(executorJobRunner);
    }
}
