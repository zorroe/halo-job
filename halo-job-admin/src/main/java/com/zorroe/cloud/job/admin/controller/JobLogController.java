package com.zorroe.cloud.job.admin.controller;

import com.zorroe.cloud.job.admin.entity.JobExecutionLog;
import com.zorroe.cloud.job.admin.service.JobExecutionLogService;
import com.zorroe.cloud.job.core.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/job/log")
public class JobLogController {

    @Resource
    private JobExecutionLogService jobExecutionLogService;

    /**
     * 获取执行日志列表
     */
    @GetMapping("/list")
    public Result<List<JobExecutionLog>> listLogs() {
        return Result.success(jobExecutionLogService.listExecutionLogs());
    }
}