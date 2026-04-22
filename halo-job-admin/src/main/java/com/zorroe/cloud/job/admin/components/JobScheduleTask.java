package com.zorroe.cloud.job.admin.components;

import cn.hutool.core.text.CharSequenceUtil;
import com.zorroe.cloud.job.admin.entity.JobExecutionLog;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.admin.service.JobExecutionLogService;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.core.common.JobStatusEnum;
import com.zorroe.cloud.job.core.util.CronUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class JobScheduleTask {

    @Resource
    private JobInfoService jobInfoService;

    @Resource
    private JobExecutionLogService jobExecutionLogService;

    @Resource
    private ExecutorInfoService executorInfoService;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Value("${executor.address}")
    private String executorAddress;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            10, 20, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(200)
    );

    private static final String LOCK_KEY_PREFIX = "job:lock:";
    private static final int LOCK_EXPIRE_SECONDS = 60; // 锁过期时间60秒
    private static final int EXECUTED_MARK_EXPIRE_MINUTES = 2; // 执行标记过期时间2分钟


    @PostConstruct
    public void startSchedule() {
        // 每秒扫描一次所有任务
        scheduler.scheduleAtFixedRate(this::scanAndExecute, 0, 1, TimeUnit.SECONDS);

        // 每10秒检查一次执行器心跳
        scheduler.scheduleAtFixedRate(executorInfoService::checkHeartBeat, 0, 10, TimeUnit.SECONDS);

        log.info("【调度中心】定时任务调度器已启动");
    }

    private void scanAndExecute() {
        try {
            long currentTime = System.currentTimeMillis();

            // ✅ 只查询已到执行时间的任务（性能优化）
            List<JobInfo> dueJobs = jobInfoService.listDueJobs(currentTime);

            if (dueJobs == null || dueJobs.isEmpty()) {
                return;
            }

            for (JobInfo job : dueJobs) {
                try {
                    // 1. 双重检查：确保任务仍然是运行状态
                    if (!JobStatusEnum.RUN.getCode().equals(job.getJobStatus())) {
                        continue;
                    }

                    // 2. 再次检查：确认确实到了执行时间（防止并发问题）
                    if (job.getNextExecuteTime() == null || job.getNextExecuteTime() > currentTime) {
                        continue;
                    }

                    // 3. 尝试获取分布式锁（集群环境下只有一个节点能执行）
                    String lockKey = LOCK_KEY_PREFIX + job.getId();
                    boolean locked = redisUtil.lock(lockKey, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

                    if (!locked) {
                        log.info("任务 [{}] 正在被其他节点执行，跳过", job.getJobName());
                        continue;
                    }

                    try {
                        // 4. 从数据库重新读取最新状态（防止脏读）
                        JobInfo latestJob = jobInfoService.getJobById(job.getId());
                        if (latestJob == null || !JobStatusEnum.RUN.getCode().equals(latestJob.getJobStatus())) {
                            log.warn("任务 [{}] 状态已变更，取消执行", job.getJobName());
                            continue;
                        }

                        // 5. 立即更新下次执行时间（先更新，防止重复触发）
                        Long nextExecuteTime = CronUtils.getNextFireTimeMillis(
                                latestJob.getCronExpression(),
                                new Date()
                        );

                        if (nextExecuteTime == null) {
                            log.error("任务 [{}] Cron 表达式无效，停止调度", job.getJobName());
                            continue;
                        }

                        // ✅ 关键：先更新下次执行时间，再异步执行任务
                        jobInfoService.updateNextExecuteTime(latestJob.getId(), nextExecuteTime);
                        log.info("📅 任务 [{}] 下次执行时间更新为: {}",
                                latestJob.getJobName(),
                                new Date(nextExecuteTime));

                        // 6. 异步执行任务
                        pool.execute(() -> executeJob(latestJob));
                        log.info("✅ 任务 [{}] 触发执行成功", latestJob.getJobName());

                    } finally {
                        // 不手动释放锁，让锁自动过期
                    }

                } catch (Exception e) {
                    log.error("处理任务 [{}] 时发生异常", job.getJobName(), e);
                }
            }
        } catch (Exception e) {
            log.error("【自动调度失败】: {}", e.getMessage());
        }
    }

    /**
     * 执行具体任务
     */
    private void executeJob(JobInfo job) {
        long startTime = System.currentTimeMillis();

        JobExecutionLog executionLog = new JobExecutionLog();
        executionLog.setJobId(job.getId());
        executionLog.setJobName(job.getJobName());
        executionLog.setExecutorHandler(job.getExecutorHandler());
        executionLog.setExecutorParam(job.getExecutorParam());
        executionLog.setStartTime(new Date());

        try {
            // 1. 路由选择执行器
            String executorAddress = executorInfoService.route(job.getId(), job.getRouteStrategy());
            if (CharSequenceUtil.isEmpty(executorAddress)) {
                throw new RuntimeException("没有可用的执行器");
            }

            // 2. 构建请求 URL
            String url = String.format("%s/executor/run?handler=%s&param=%s",
                    executorAddress,
                    job.getExecutorHandler(),
                    job.getExecutorParam());

            // 3. 执行任务（支持重试）
            int maxRetry = job.getRetryCount() == null ? 0 : job.getRetryCount();
            Exception lastException = null;

            for (int i = 0; i <= maxRetry; i++) {
                try {
                    if (i > 0) {
                        log.warn("任务 [{}] 第 {} 次重试", job.getJobName(), i);
                        Thread.sleep(1000 * i); // 递增延迟重试
                    }

                    restTemplate.getForObject(url, String.class);
                    lastException = null;
                    break; // 成功则跳出循环

                } catch (Exception e) {
                    lastException = e;
                    log.warn("任务 [{}] 第 {} 次执行失败: {}", job.getJobName(), i + 1, e.getMessage());

                    if (i == maxRetry) {
                        throw e; // 最后一次重试仍失败，抛出异常
                    }
                }
            }

            // 4. 记录成功日志
            executionLog.setStatus(1);
            executionLog.setErrorMsg(null);
            log.info("✅ 任务 [{}] 执行成功，耗时: {}ms", job.getJobName(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            // 5. 记录失败日志
            executionLog.setStatus(0);
            executionLog.setErrorMsg(e.getMessage());
            log.error("❌ 任务 [{}] 执行失败", job.getJobName(), e);

        } finally {
            // 6. 保存执行日志
            long endTime = System.currentTimeMillis();
            executionLog.setEndTime(new Date());
            executionLog.setExecutionTime(endTime - startTime);

            try {
                jobExecutionLogService.insertLog(executionLog);
            } catch (Exception e) {
                log.error("保存任务执行日志失败", e);
            }
        }
    }
}
