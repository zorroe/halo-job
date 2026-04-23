package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.mapper.JobInfoMapper;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.core.ExecutorRouteEnum;
import com.zorroe.cloud.job.core.common.BlockStrategyEnum;
import com.zorroe.cloud.job.core.util.CronUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class JobInfoServiceImpl implements JobInfoService {

    @Resource
    private JobInfoMapper jobInfoMapper;

    @Override
    public JobInfo getJobById(Long jobId) {
        return jobInfoMapper.getJobById(jobId);
    }

    @Override
    public List<JobInfo> listAllJobs() {
        return jobInfoMapper.listAllJobs();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addJob(JobInfo jobInfo) {
        if (jobInfo.getJobStatus() == null) {
            jobInfo.setJobStatus(1);
        }
        if (jobInfo.getRetryCount() == null) {
            jobInfo.setRetryCount(0);
        }
        if (jobInfo.getRouteStrategy() == null) {
            jobInfo.setRouteStrategy(ExecutorRouteEnum.ROUND.getCode());
        }
        if (jobInfo.getBlockStrategy() == null) {
            jobInfo.setBlockStrategy(BlockStrategyEnum.QUEUE_WAIT.getCode());
        }

        if (jobInfo.getCronExpression() != null && !jobInfo.getCronExpression().trim().isEmpty()) {
            if (!CronUtils.isValid(jobInfo.getCronExpression())) {
                throw new IllegalArgumentException("无效的 Cron 表达式");
            }
            Long nextTime = CronUtils.getNextFireTimeMillis(jobInfo.getCronExpression(), new Date());
            if (nextTime == null) {
                throw new IllegalArgumentException("无法计算下次执行时间");
            }
            jobInfo.setNextExecuteTime(nextTime);
            log.info("任务 [{}] 下次执行时间: {}", jobInfo.getJobName(), new Date(nextTime));
        }

        jobInfoMapper.addJob(jobInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(JobInfo jobInfo) {
        if (jobInfo.getRouteStrategy() == null) {
            jobInfo.setRouteStrategy(ExecutorRouteEnum.ROUND.getCode());
        }
        if (jobInfo.getBlockStrategy() == null) {
            jobInfo.setBlockStrategy(BlockStrategyEnum.QUEUE_WAIT.getCode());
        }
        if (jobInfo.getRetryCount() == null) {
            jobInfo.setRetryCount(0);
        }

        if (jobInfo.getCronExpression() != null) {
            if (!CronUtils.isValid(jobInfo.getCronExpression())) {
                throw new IllegalArgumentException("无效的 Cron 表达式");
            }
            Long nextTime = CronUtils.getNextFireTimeMillis(jobInfo.getCronExpression(), new Date());
            if (nextTime == null) {
                throw new IllegalArgumentException("无法计算下次执行时间");
            }
            jobInfo.setNextExecuteTime(nextTime);
        }
        jobInfoMapper.updateJob(jobInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        jobInfoMapper.changeStatus(id, status);
    }

    @Override
    public List<JobInfo> listDueJobs(long currentTime) {
        return jobInfoMapper.listDueJobs(currentTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNextExecuteTime(Long id, Long nextExecuteTime) {
        jobInfoMapper.updateNextExecuteTime(id, nextExecuteTime);
    }
}
