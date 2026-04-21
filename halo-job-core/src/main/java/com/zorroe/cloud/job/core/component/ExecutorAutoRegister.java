package com.zorroe.cloud.job.core.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
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

    private String address;
    private RestTemplate template = new RestTemplate();

    @Override
    public void run(String... args) throws Exception {
        address = "http://localhost:" + port;
        register();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::beat, 5, 5, TimeUnit.SECONDS
        );
    }

    private void register() {
        try {
            template.postForObject(adminAddress + "/executor/api/register?name={name}&address={address}",
                    null, String.class,
                    name, address);
        } catch (Exception e) {
        }
    }

    private void beat() {
        try {
            template.postForObject(adminAddress + "/executor/api/beat?name={name}&address={address}",
                    null, String.class, name, address);
        } catch (Exception e) {
        }
    }
}
