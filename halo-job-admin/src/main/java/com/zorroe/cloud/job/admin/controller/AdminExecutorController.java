package com.zorroe.cloud.job.admin.controller;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.core.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/executor")
public class AdminExecutorController {

    @Resource
    private ExecutorInfoService executorInfoService;

    @GetMapping("/list")
    public Result<List<ExecutorInfo>> listExecutors(
            @RequestParam(required = false) String executorGroup,
            @RequestParam(required = false) String executorApp,
            @RequestParam(required = false) Integer status
    ) {
        return Result.success(executorInfoService.listExecutors(executorGroup, executorApp, status));
    }
}
