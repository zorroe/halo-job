package com.zorroe.cloud.job.executor.schedule;

import com.zorroe.cloud.job.core.anno.HaloJob;
import com.zorroe.cloud.job.core.context.HaloJobContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoJobTemplate {

    @HaloJob("demoNoArgTask")
    public void demoNoArgTask() {
        log.info("run demoNoArgTask");
    }

    @HaloJob("dataSyncTask")
    public void dataSync(String param) {
        log.info("run dataSyncTask, param={}", param);
    }

    @HaloJob("clearLogTask")
    public void clearLog(HaloJobContext context) {
        log.info(
                "run clearLogTask, shard={}/{}",
                context.getShardIndex() + 1,
                context.getShardTotal()
        );
    }

    @HaloJob("reportShardTask")
    public void reportShard(String param, HaloJobContext context) {
        log.info(
                "run reportShardTask, param={}, shard={}/{}",
                param,
                context.getShardIndex() + 1,
                context.getShardTotal()
        );
    }
}
