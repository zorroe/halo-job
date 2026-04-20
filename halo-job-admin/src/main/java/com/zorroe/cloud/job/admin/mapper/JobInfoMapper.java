package com.zorroe.cloud.job.admin.mapper;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JobInfoMapper {

    JobInfo getJobById(Long jobId);

    List<JobInfo> listAllJobs();

    void addJob(JobInfo jobInfo);

    void updateJob(JobInfo jobInfo);

    void changeStatus(Long id, Integer status);
}
