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
