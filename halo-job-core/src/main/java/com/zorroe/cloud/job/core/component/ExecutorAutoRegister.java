package com.zorroe.cloud.job.core.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ExecutorAutoRegister implements CommandLineRunner {

    @Value("${executor.admin:http://localhost:8080}")
    private String adminAddress;

    @Value("${server.port}")
    private int port;

    @Value("${executor.name:default-executor}")
    private String name;

    @Value("${executor.address:}")
    private String configuredAddress;

    private String address;
    private final RestTemplate template = new RestTemplate();

    /**
     * 应用启动后计算执行器地址并完成首次注册，同时开启心跳续约任务。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        address = StringUtils.hasText(configuredAddress)
                ? configuredAddress
                : "http://localhost:" + port;
        register();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::beat, 5, 5, TimeUnit.SECONDS
        );
    }

    /**
     * 向调度中心登记当前执行器，便于后续任务路由。
     */
    private void register() {
        try {
            template.postForObject(
                    adminAddress + "/executor/api/register?name={name}&address={address}",
                    null,
                    String.class,
                    name,
                    address
            );
        } catch (Exception ignored) {
        }
    }

    /**
     * 定时上报心跳，刷新执行器在线状态。
     */
    private void beat() {
        try {
            template.postForObject(
                    adminAddress + "/executor/api/beat?name={name}&address={address}",
                    null,
                    String.class,
                    name,
                    address
            );
        } catch (Exception ignored) {
        }
    }
}
