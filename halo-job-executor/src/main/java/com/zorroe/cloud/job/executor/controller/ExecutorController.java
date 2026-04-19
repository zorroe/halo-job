package com.zorroe.cloud.job.executor.controller;

import com.zorroe.cloud.job.core.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/executor")
public class ExecutorController {

    /**
     * 接收调度命令，执行任务
     */
    @GetMapping("/run")
    public Result<String> runTask(
            @RequestParam String handler,
            @RequestParam(required = false) String param
    ) {
        log.info("【执行器】收到任务：handler={}，param={}", handler, param);

        if (handler.equals("dataSyncTask")) {
            return Result.success(dataSyncTask(param));
        }
        return Result.fail("未知任务处理器：" + handler);
    }

    private String dataSyncTask(String param) {
        try {
            Thread.sleep(500);
            log.info("【执行器】任务执行成功，参数：{}", param);
            return "数据同步完成";
        } catch (InterruptedException e) {
            return "操作失败";
        }
    }
}
