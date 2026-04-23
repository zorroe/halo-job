package com.zorroe.cloud.job.executor;

import com.zorroe.cloud.job.core.anno.EnableHaloJobExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableHaloJobExecutor
@SpringBootApplication
public class HaloJobExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaloJobExecutorApplication.class, args);
    }

}
