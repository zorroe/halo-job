package com.zorroe.cloud.job.admin.controller;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.core.common.JobStatusEnum;
import com.zorroe.cloud.job.core.common.Result;
import com.zorroe.cloud.job.core.common.ResultCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/schedule")
public class ScheduleController {

    @Resource
    private RestTemplate restTemplate;

    @Value("${executor.address}")
    private String executorAddress;

    @Resource
    private JobInfoService jobInfoService;

    /**
     * 手动触发任务：调用执行器执行
     * 访问地址：http://localhost:9090/schedule/trigger
     */
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
            log.info("【调度中心】开始触发执行器执行任务{}", jobInfo.getJobName());
            String url = String.format("%s/executor/run?handler=%s&param=%s",
                    executorAddress,
                    jobInfo.getExecutorHandler(),
                    jobInfo.getExecutorParam());
            // 调用执行器的执行接口
            Result<String> result = restTemplate.getForObject(url, Result.class);
            return Result.success("调度成功：" + result);
        } catch (Exception e) {
            return Result.fail("调度失败：" + e.getMessage());
        }
    }
}
