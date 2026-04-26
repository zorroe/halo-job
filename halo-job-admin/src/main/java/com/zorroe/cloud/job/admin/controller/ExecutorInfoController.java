package com.zorroe.cloud.job.admin.controller;

import com.zorroe.cloud.job.admin.entity.ExecutorHandlerInfo;
import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.core.common.Result;
import com.zorroe.cloud.job.core.model.ExecutorHeartbeatRequest;
import com.zorroe.cloud.job.core.model.ExecutorHandlerDefinition;
import com.zorroe.cloud.job.core.model.ExecutorRegistryRequest;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/executor/api")
public class ExecutorInfoController {

    @Resource
    private ExecutorInfoService executorInfoService;

    @PostMapping("/register")
    public Result<String> register(@RequestBody ExecutorRegistryRequest request) {
        executorInfoService.register(toExecutorInfo(request), toHandlerInfos(request));
        return Result.success();
    }

    @PostMapping("/beat")
    public Result<String> beat(@RequestBody ExecutorHeartbeatRequest request) {
        executorInfoService.beat(toExecutorInfo(request));
        return Result.success();
    }

    @GetMapping("/onlineList")
    public Result<List<ExecutorInfo>> onlineList() {
        return Result.success(executorInfoService.getOnlineList());
    }

    private ExecutorInfo toExecutorInfo(ExecutorRegistryRequest request) {
        ExecutorInfo info = new ExecutorInfo();
        info.setExecutorName(request == null ? null : request.getName());
        info.setExecutorAddress(request == null ? null : request.getAddress());
        info.setExecutorGroup(request == null ? null : request.getGroup());
        info.setExecutorApp(request == null ? null : request.getApp());
        info.setVersion(request == null ? null : request.getVersion());
        info.setMetadata(request == null ? null : request.getMetadata());
        return info;
    }

    private ExecutorInfo toExecutorInfo(ExecutorHeartbeatRequest request) {
        ExecutorInfo info = new ExecutorInfo();
        info.setExecutorName(request == null ? null : request.getName());
        info.setExecutorAddress(request == null ? null : request.getAddress());
        info.setExecutorGroup(request == null ? null : request.getGroup());
        info.setExecutorApp(request == null ? null : request.getApp());
        info.setVersion(request == null ? null : request.getVersion());
        return info;
    }

    private List<ExecutorHandlerInfo> toHandlerInfos(ExecutorRegistryRequest request) {
        if (request == null || request.getHandlers() == null) {
            return null;
        }
        List<ExecutorHandlerInfo> result = new ArrayList<>(request.getHandlers().size());
        for (ExecutorHandlerDefinition definition : request.getHandlers()) {
            ExecutorHandlerInfo info = new ExecutorHandlerInfo();
            info.setExecutorName(request.getName());
            info.setExecutorAddress(request.getAddress());
            info.setExecutorGroup(request.getGroup());
            info.setExecutorApp(request.getApp());
            info.setHandlerName(definition.getHandlerName());
            info.setHandlerDesc(definition.getDescription());
            info.setMethodSignature(definition.getMethodSignature());
            result.add(info);
        }
        return result;
    }
}
