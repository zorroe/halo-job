package com.zorroe.cloud.job.executor.schedule;

import com.zorroe.cloud.job.core.anno.HaloJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestJob {

    @HaloJob("dataSyncTask")
    public void dataSync(String param) {
        log.info("【执行器】执行数据同步任务，参数：{}", param);
    }

    @HaloJob("clearLogTask")
    public void clearLog() {
        log.info("【执行器】执行日志清理任务");
    }
}
