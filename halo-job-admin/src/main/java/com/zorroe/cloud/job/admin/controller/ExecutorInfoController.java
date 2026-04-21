package com.zorroe.cloud.job.admin.controller;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.core.common.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/executor/api")
public class ExecutorInfoController {

    @Resource
    private ExecutorInfoService executorInfoService;

    @PostMapping("/register")
    public Result<String> register(@RequestParam String name,
                                   @RequestParam String address) {
        ExecutorInfo info = new ExecutorInfo();
        info.setExecutorName(name);
        info.setExecutorAddress(address);
        executorInfoService.register(info);
        return Result.success();
    }

    @PostMapping("/beat")
    public Result<String> beat(@RequestParam String name,
                               @RequestParam String address) {
        ExecutorInfo info = new ExecutorInfo();
        info.setExecutorAddress(address);
        info.setExecutorName(name);
        executorInfoService.register(info);
        return Result.success();
    }

    @GetMapping("/onlineList")
    public Result<List<ExecutorInfo>> onlineList() {
        return Result.success(executorInfoService.getOnlineList());
    }
}
