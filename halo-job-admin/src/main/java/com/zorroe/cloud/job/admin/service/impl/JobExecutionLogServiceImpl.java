package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.JobExecutionLog;
import com.zorroe.cloud.job.admin.mapper.JobExecutionLogMapper;
import com.zorroe.cloud.job.admin.service.JobExecutionLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobExecutionLogServiceImpl implements JobExecutionLogService {

    @Resource
    private JobExecutionLogMapper jobExecutionLogMapper;

    @Override
    public void insertLog(JobExecutionLog jobExecutionLog) {
        jobExecutionLogMapper.save(jobExecutionLog);
    }

    @Override
    public List<JobExecutionLog> listExecutionLogs() {
        return jobExecutionLogMapper.listByPage();
    }
}
