package com.zorroe.cloud.job.admin.components;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.model.JobTriggerSummary;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.admin.service.JobTriggerService;
import com.zorroe.cloud.job.core.common.JobStatusEnum;
import com.zorroe.cloud.job.core.model.TriggerTypeEnum;
import com.zorroe.cloud.job.core.util.CronUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JobScheduleTask {

    @Resource
    private JobInfoService jobInfoService;

    @Resource
    private ExecutorInfoService executorInfoService;

    @Resource
    private JobTriggerService jobTriggerService;

    @Resource
    private RedisUtil redisUtil;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            10, 20, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(200)
    );

    private static final String LOCK_KEY_PREFIX = "job:lock:";
    private static final int LOCK_EXPIRE_SECONDS = 60;

    @PostConstruct
    public void startSchedule() {
        scheduler.scheduleAtFixedRate(this::scanAndExecute, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(executorInfoService::checkHeartBeat, 0, 10, TimeUnit.SECONDS);
        log.info("调度中心的任务扫描器已启动");
    }

    private void scanAndExecute() {
        try {
            long currentTime = System.currentTimeMillis();
            List<JobInfo> dueJobs = jobInfoService.listDueJobs(currentTime);
            if (dueJobs == null || dueJobs.isEmpty()) {
                return;
            }

            for (JobInfo job : dueJobs) {
                try {
                    if (!JobStatusEnum.RUN.getCode().equals(job.getJobStatus())) {
                        continue;
                    }
                    if (job.getNextExecuteTime() == null || job.getNextExecuteTime() > currentTime) {
                        continue;
                    }

                    String lockKey = LOCK_KEY_PREFIX + job.getId();
                    boolean locked = redisUtil.lock(lockKey, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    if (!locked) {
                        log.info("任务 [{}] 已被其他节点处理，跳过本次扫描", job.getJobName());
                        continue;
                    }

                    try {
                        JobInfo latestJob = jobInfoService.getJobById(job.getId());
                        if (latestJob == null || !JobStatusEnum.RUN.getCode().equals(latestJob.getJobStatus())) {
                            log.warn("任务 [{}] 状态已变更，取消本次执行", job.getJobName());
                            continue;
                        }

                        Long nextExecuteTime = CronUtils.getNextFireTimeMillis(latestJob.getCronExpression(), new Date());
                        if (nextExecuteTime == null) {
                            log.error("任务 [{}] 的 Cron 无法计算下次执行时间，跳过本次调度", job.getJobName());
                            continue;
                        }

                        jobInfoService.updateNextExecuteTime(latestJob.getId(), nextExecuteTime);
                        pool.execute(() -> executeJob(latestJob));
                    } finally {
                        redisUtil.unlock(lockKey);
                    }
                } catch (Exception e) {
                    log.error("处理任务 [{}] 时发生异常", job.getJobName(), e);
                }
            }
        } catch (Exception e) {
            log.error("自动调度扫描失败", e);
        }
    }

    private void executeJob(JobInfo job) {
        try {
            JobTriggerSummary summary = jobTriggerService.trigger(job, TriggerTypeEnum.SCHEDULE);
            if (summary.isSuccess()) {
                log.info("任务 [{}] 调度成功: {}", job.getJobName(), summary.getMessage());
            } else {
                log.warn("任务 [{}] 调度完成但存在失败: {}", job.getJobName(), summary.getMessage());
            }
        } catch (Exception e) {
            log.error("任务 [{}] 调度失败", job.getJobName(), e);
        }
    }
}
