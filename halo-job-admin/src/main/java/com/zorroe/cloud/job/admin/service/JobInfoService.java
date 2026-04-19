package com.zorroe.cloud.job.admin.service;

import com.zorroe.cloud.job.admin.entity.JobInfo;

import java.util.List;

public interface JobInfoService {

    public JobInfo getJobById(Long jobId);

    public List<JobInfo> listAllJobs();
}
