package com.zorroe.cloud.job.admin.service;

import com.zorroe.cloud.job.admin.entity.JobExecutionLog;

import java.util.List;


public interface JobExecutionLogService {

    void insertLog(JobExecutionLog jobExecutionLog);

    List<JobExecutionLog> listExecutionLogs();
}
