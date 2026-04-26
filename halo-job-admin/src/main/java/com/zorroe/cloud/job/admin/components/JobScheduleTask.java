package com.zorroe.cloud.job.admin.components;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.model.JobTriggerSummary;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.admin.service.JobTriggerService;
import com.zorroe.cloud.job.admin.trigger.TriggerDispatchPlan;
import com.zorroe.cloud.job.admin.trigger.TriggerParserManager;
import com.zorroe.cloud.job.core.common.JobStatusEnum;
import com.zorroe.cloud.job.core.model.TriggerTypeEnum;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    @Resource
    private TriggerParserManager triggerParserManager;

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

                        TriggerDispatchPlan dispatchPlan = triggerParserManager.buildDispatchPlan(latestJob, currentTime);
                        jobInfoService.updateNextExecuteTime(latestJob.getId(), dispatchPlan.getNextExecuteTimeBeforeDispatch());
                        pool.execute(() -> executeJob(latestJob, dispatchPlan));
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

    private void executeJob(JobInfo job, TriggerDispatchPlan dispatchPlan) {
        long startTime = System.currentTimeMillis();
        try {
            JobTriggerSummary summary = jobTriggerService.trigger(job, TriggerTypeEnum.SCHEDULE);
            if (summary.isSuccess()) {
                log.info("任务 [{}] 调度成功: {}", job.getJobName(), summary.getMessage());
            } else {
                log.warn("任务 [{}] 调度完成但存在失败: {}", job.getJobName(), summary.getMessage());
            }
        } catch (Exception e) {
            log.error("任务 [{}] 调度失败", job.getJobName(), e);
        } finally {
            if (dispatchPlan != null && dispatchPlan.isUpdateAfterExecution()) {
                try {
                    Long nextExecuteTime = triggerParserManager.computeNextExecuteTimeAfterExecution(
                            job,
                            startTime,
                            System.currentTimeMillis()
                    );
                    jobInfoService.updateNextExecuteTime(job.getId(), nextExecuteTime);
                } catch (Exception e) {
                    log.error("任务 [{}] 更新下次执行时间失败", job.getJobName(), e);
                }
            }
        }
    }
}
