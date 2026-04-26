package com.zorroe.cloud.job.core.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExecutorRegistryRequest {

    private String name;

    private String address;

    private String group;

    private String app;

    private String version;

    private String metadata;

    private List<ExecutorHandlerDefinition> handlers = new ArrayList<>();
}
