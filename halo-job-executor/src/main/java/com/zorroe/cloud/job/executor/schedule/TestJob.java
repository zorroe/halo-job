package com.zorroe.cloud.job.executor.schedule;

import com.zorroe.cloud.job.core.anno.HaloJob;
import com.zorroe.cloud.job.core.context.HaloJobContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestJob {

    @HaloJob("dataSyncTask")
    public void dataSync(String param, HaloJobContext context) {
        log.info(
                "执行数据同步任务, param={}, shard={}/{}",
                param,
                context.getShardIndex() + 1,
                context.getShardTotal()
        );
    }

    @HaloJob("clearLogTask")
    public void clearLog(HaloJobContext context) {
        log.info(
                "执行日志清理任务, shard={}/{}",
                context.getShardIndex() + 1,
                context.getShardTotal()
        );
    }
}
