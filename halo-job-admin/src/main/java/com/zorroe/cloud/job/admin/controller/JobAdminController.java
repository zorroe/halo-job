package com.zorroe.cloud.job.admin.controller;

import com.zorroe.cloud.job.admin.entity.ExecutorHandlerInfo;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.core.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/job")
public class JobAdminController {

    @Resource
    private JobInfoService jobInfoService;

    @Resource
    private ExecutorInfoService executorInfoService;

    /**
     * 新增/修改任务
     */
    @PostMapping("/save")
    public Result<String> saveJob(@RequestBody JobInfo jobInfo) {
        if (jobInfo.getId() == null) {
            jobInfoService.addJob(jobInfo);
        } else {
            jobInfoService.updateJob(jobInfo);
        }
        return Result.success("操作成功");
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/get/{id}")
    public Result<JobInfo> getJob(@PathVariable Long id) {
        return Result.success(jobInfoService.getJobById(id));
    }

    /**
     * 获取所有任务
     */
    @GetMapping("/list")
    public Result<List<JobInfo>> listJobs() {
        return Result.success(jobInfoService.listAllJobs());
    }

    @GetMapping("/handlers")
    public Result<List<ExecutorHandlerInfo>> listHandlers(
            @RequestParam(required = false) String executorGroup,
            @RequestParam(required = false) String executorApp
    ) {
        return Result.success(executorInfoService.listAvailableHandlers(executorGroup, executorApp));
    }

    /**
     * 启停任务
     */
    @PostMapping("/changeStatus/{id}/{status}")
    public Result<String> changeStatus(
            @PathVariable Long id,
            @PathVariable Integer status
    ) {
        jobInfoService.changeStatus(id, status);
        return Result.success("状态修改成功");
    }
}
