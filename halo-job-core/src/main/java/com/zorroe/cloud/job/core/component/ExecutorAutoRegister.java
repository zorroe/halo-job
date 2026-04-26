package com.zorroe.cloud.job.core.component;

import com.zorroe.cloud.job.core.model.ExecutorHeartbeatRequest;
import com.zorroe.cloud.job.core.model.ExecutorRegistryRequest;
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

    @Value("${executor.group:default}")
    private String group;

    @Value("${executor.app:}")
    private String app;

    @Value("${executor.version:}")
    private String version;

    @Value("${spring.application.name:}")
    private String applicationName;

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
                    adminAddress + "/executor/api/register",
                    buildRegistryRequest(),
                    String.class
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
                    adminAddress + "/executor/api/beat",
                    buildHeartbeatRequest(),
                    String.class
            );
        } catch (Exception ignored) {
        }
    }

    private ExecutorRegistryRequest buildRegistryRequest() {
        ExecutorRegistryRequest request = new ExecutorRegistryRequest();
        request.setName(name);
        request.setAddress(address);
        request.setGroup(StringUtils.hasText(group) ? group : "default");
        request.setApp(StringUtils.hasText(app) ? app : (StringUtils.hasText(applicationName) ? applicationName : name));
        request.setVersion(version);
        request.setHandlers(JobMethodRegistry.listHandlerDefinitions());
        return request;
    }

    private ExecutorHeartbeatRequest buildHeartbeatRequest() {
        ExecutorHeartbeatRequest request = new ExecutorHeartbeatRequest();
        request.setName(name);
        request.setAddress(address);
        request.setGroup(StringUtils.hasText(group) ? group : "default");
        request.setApp(StringUtils.hasText(app) ? app : (StringUtils.hasText(applicationName) ? applicationName : name));
        request.setVersion(version);
        return request;
    }
}
