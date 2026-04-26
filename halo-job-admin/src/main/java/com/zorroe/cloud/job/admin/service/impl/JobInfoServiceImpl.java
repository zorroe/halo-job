package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.mapper.JobInfoMapper;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.admin.trigger.TriggerParserManager;
import com.zorroe.cloud.job.core.ExecutorRouteEnum;
import com.zorroe.cloud.job.core.common.BlockStrategyEnum;
import com.zorroe.cloud.job.core.common.JobStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class JobInfoServiceImpl implements JobInfoService {

    @Resource
    private JobInfoMapper jobInfoMapper;

    @Resource
    private TriggerParserManager triggerParserManager;

    @Resource
    private ExecutorInfoService executorInfoService;

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
        prepareJobForPersist(jobInfo);
        jobInfoMapper.addJob(jobInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(JobInfo jobInfo) {
        prepareJobForPersist(jobInfo);
        jobInfoMapper.updateJob(jobInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        JobInfo jobInfo = jobInfoMapper.getJobById(id);
        if (jobInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }

        Long nextExecuteTime = null;
        if (JobStatusEnum.RUN.getCode().equals(status)) {
            nextExecuteTime = triggerParserManager.refreshNextExecuteTime(jobInfo);
        }
        jobInfoMapper.changeStatus(id, status, nextExecuteTime);
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

    private void prepareJobForPersist(JobInfo jobInfo) {
        if (jobInfo == null) {
            throw new IllegalArgumentException("jobInfo cannot be null");
        }
        if (!StringUtils.hasText(jobInfo.getJobName())) {
            throw new IllegalArgumentException("jobName cannot be blank");
        }
        if (!StringUtils.hasText(jobInfo.getExecutorHandler())) {
            throw new IllegalArgumentException("executorHandler cannot be blank");
        }

        if (!StringUtils.hasText(jobInfo.getExecutorGroup())) {
            jobInfo.setExecutorGroup("default");
        } else {
            jobInfo.setExecutorGroup(jobInfo.getExecutorGroup().trim());
        }
        if (!StringUtils.hasText(jobInfo.getExecutorApp())) {
            throw new IllegalArgumentException("executorApp cannot be blank");
        }
        jobInfo.setExecutorApp(jobInfo.getExecutorApp().trim());
        jobInfo.setExecutorHandler(jobInfo.getExecutorHandler().trim());
        jobInfo.setRemark(StringUtils.hasText(jobInfo.getRemark()) ? jobInfo.getRemark().trim() : null);
        jobInfo.setOwner(StringUtils.hasText(jobInfo.getOwner()) ? jobInfo.getOwner().trim() : null);
        jobInfo.setTag(StringUtils.hasText(jobInfo.getTag()) ? jobInfo.getTag().trim() : null);

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
        triggerParserManager.prepareForPersist(jobInfo);
        executorInfoService.validateHandlerBinding(
                jobInfo.getExecutorGroup(),
                jobInfo.getExecutorApp(),
                jobInfo.getExecutorHandler()
        );
        log.info("任务 [{}] 触发器已标准化，nextExecuteTime={}", jobInfo.getJobName(), jobInfo.getNextExecuteTime());
    }
}
