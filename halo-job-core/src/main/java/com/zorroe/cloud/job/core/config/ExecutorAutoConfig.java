package com.zorroe.cloud.job.core.config;

import com.zorroe.cloud.job.core.component.ExecutorAutoRegister;
import com.zorroe.cloud.job.core.component.ExecutorJobRunner;
import com.zorroe.cloud.job.core.component.JobAnnotationScanner;
import com.zorroe.cloud.job.core.controller.ExecutorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorAutoConfig {

    /**
     * 注册任务注解扫描器，在执行器启动时发现并登记任务方法。
     *
     * @return 任务注解扫描器
     */
    @Bean
    public JobAnnotationScanner jobAnnotationScanner() {
        return new JobAnnotationScanner();
    }

    /**
     * 注册执行器自动注册组件，负责向调度中心上报自身信息。
     *
     * @return 自动注册组件
     */
    @Bean
    public ExecutorAutoRegister executorAutoRegister() {
        return new ExecutorAutoRegister();
    }

    /**
     * 注册执行器任务运行器，负责串行化和阻塞策略控制。
     *
     * @return 任务运行器
     */
    @Bean
    public ExecutorJobRunner executorJobRunner() {
        return new ExecutorJobRunner();
    }

    /**
     * 暴露执行器控制器，接收调度中心下发的触发请求。
     *
     * @param executorJobRunner 任务运行器
     * @return 执行器控制器
     */
    @Bean
    public ExecutorController executorController(ExecutorJobRunner executorJobRunner) {
        return new ExecutorController(executorJobRunner);
    }
}
