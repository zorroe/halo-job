package com.zorroe.cloud.job.admin.mapper;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JobInfoMapper {

    JobInfo getJobById(Long jobId);

    List<JobInfo> listAllJobs();

    void addJob(JobInfo jobInfo);

    void updateJob(JobInfo jobInfo);

    void changeStatus(Long id, Integer status);

    /**
     * 查询已到执行时间的任务
     */
    List<JobInfo> listDueJobs(@Param("currentTime") long currentTime);

    void updateNextExecuteTime(@Param("id") Long id, @Param("nextExecuteTime") Long nextExecuteTime);

}
