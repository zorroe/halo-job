package com.zorroe.cloud.job.admin.components;

import com.zorroe.cloud.job.admin.entity.JobExecutionLog;
import com.zorroe.cloud.job.admin.entity.JobInfo;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JobScheduleTask {

    @Resource
    private JobInfoService jobInfoService;

    @Resource
    private JobExecutionLogService jobExecutionLogService;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Value("${executor.address}")
    private String executorAddress;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String LOCK_KEY_PREFIX = "job:lock:";
    private static final long LOCK_TIMEOUT = 5; // 锁超时5分钟，防止死锁

    @PostConstruct
    public void startSchedule() {
        scheduler.scheduleAtFixedRate(this::scanAndExecute, 0, 1, TimeUnit.SECONDS);
    }

    private void scanAndExecute() {
        try {
            log.info("扫描定时任务开始: {}", LocalDateTime.now());
            List<JobInfo> jobList = jobInfoService.listAllJobs();

            for (JobInfo job : jobList) {
                // 只执行运行中的任务
                if (!JobStatusEnum.RUN.getCode().equals(job.getJobStatus())) {
                    continue;
                }
                // 判断 cron 是否匹配当前时间
                if (CronUtils.isMatch(job.getCronExpression())) {
                    // 🔒 核心：加锁防重复执行
                    String lockKey = LOCK_KEY_PREFIX + job.getId();
                    boolean locked = redisUtil.lock(lockKey, LOCK_TIMEOUT, TimeUnit.MINUTES);
                    if (locked) {
                        try {
                            executeJob(job);
                        } finally {
                            redisUtil.unlock(lockKey);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("【自动调度失败】: {}", e.getMessage());
        }
    }

    private void executeJob(JobInfo job) {

        long start = System.currentTimeMillis();
        JobExecutionLog jobExecutionLog = new JobExecutionLog();
        jobExecutionLog.setJobId(job.getId());
        jobExecutionLog.setJobName(job.getJobName());
        jobExecutionLog.setExecutorHandler(job.getExecutorHandler());
        jobExecutionLog.setExecutorParam(job.getExecutorParam());
        jobExecutionLog.setStartTime(new Date());


        try {
            String url = String.format("%s/executor/run?handler=%s&param=%s",
                    executorAddress,
                    job.getExecutorHandler(),
                    job.getExecutorParam());
            String result = restTemplate.getForObject(url, String.class);
            jobExecutionLog.setStatus(1); // 成功
            log.info("【自动调度执行】任务: {}, 结果: {}", job.getJobName(), result);
        } catch (Exception e) {
            jobExecutionLog.setStatus(0); // 失败
            jobExecutionLog.setErrorMsg(e.getMessage());
            log.error("【自动调度失败】任务: {}", job.getJobName());
        } finally {
            long end = System.currentTimeMillis();
            jobExecutionLog.setEndTime(new Date());
            jobExecutionLog.setExecutionTime(end - start);
            jobExecutionLogService.insertLog(jobExecutionLog);
        }
    }
}
