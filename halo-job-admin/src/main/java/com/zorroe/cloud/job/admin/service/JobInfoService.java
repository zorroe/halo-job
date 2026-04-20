package com.zorroe.cloud.job.admin.service;

import com.zorroe.cloud.job.admin.entity.JobInfo;

import java.util.List;

public interface JobInfoService {

    JobInfo getJobById(Long jobId);

    List<JobInfo> listAllJobs();

    void addJob(JobInfo jobInfo);

    void updateJob(JobInfo jobInfo);

    void changeStatus(Long id, Integer status);
}
