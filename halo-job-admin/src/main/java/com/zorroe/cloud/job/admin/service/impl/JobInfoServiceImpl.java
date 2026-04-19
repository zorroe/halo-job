package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.mapper.JobInfoMapper;
import com.zorroe.cloud.job.admin.service.JobInfoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
