package com.zorroe.cloud.job.admin.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ExecutorHandlerInfo {

    private Long id;

    private String executorName;

    private String executorAddress;

    private String executorGroup;

    private String executorApp;

    private String handlerName;

    private String handlerDesc;

    private String methodSignature;

    private Date updateTime;

    private Long onlineExecutorCount;
}
