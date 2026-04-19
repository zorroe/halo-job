package com.zorroe.cloud.job.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zorroe.cloud.job.admin.mapper")
@SpringBootApplication
public class HaloJobAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaloJobAdminApplication.class, args);
    }

}
