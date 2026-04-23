package com.zorroe.cloud.job.admin.controller;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.model.JobTriggerSummary;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.admin.service.JobTriggerService;
import com.zorroe.cloud.job.core.common.JobStatusEnum;
import com.zorroe.cloud.job.core.common.Result;
import com.zorroe.cloud.job.core.common.ResultCode;
import com.zorroe.cloud.job.core.model.TriggerTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Resource
    private JobInfoService jobInfoService;

    @Resource
    private JobTriggerService jobTriggerService;

    @GetMapping("/trigger")
    public Result<String> triggerTask(@RequestParam Long jobId) {
        try {
            JobInfo jobInfo = jobInfoService.getJobById(jobId);
            if (Objects.isNull(jobInfo)) {
                return Result.build(ResultCode.JOB_NOT_EXIST);
            }
            if (JobStatusEnum.STOP.getCode().equals(jobInfo.getJobStatus())) {
                return Result.build(ResultCode.JOB_STOPPED);
            }

            log.info("开始手动触发任务 {}", jobInfo.getJobName());
            JobTriggerSummary summary = jobTriggerService.trigger(jobInfo, TriggerTypeEnum.MANUAL);
            if (summary.isSuccess()) {
                return Result.success(summary.getMessage());
            }
            return Result.fail(summary.getMessage());
        } catch (Exception e) {
            return Result.fail("调度失败：" + e.getMessage());
        }
    }
}
