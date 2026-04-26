package com.zorroe.cloud.job.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorHandlerDefinition {

    private String handlerName;

    private String description;

    private String methodSignature;
}
