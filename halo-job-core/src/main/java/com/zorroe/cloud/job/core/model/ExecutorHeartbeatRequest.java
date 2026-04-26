package com.zorroe.cloud.job.core.model;

import lombok.Data;

@Data
public class ExecutorHeartbeatRequest {

    private String name;

    private String address;

    private String group;

    private String app;

    private String version;
}
