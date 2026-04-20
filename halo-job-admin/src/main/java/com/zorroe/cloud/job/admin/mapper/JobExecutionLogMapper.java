package com.zorroe.cloud.job.admin.mapper;

import com.zorroe.cloud.job.admin.entity.JobExecutionLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JobExecutionLogMapper {

    void save(JobExecutionLog log);

    List<JobExecutionLog> listByPage();
}
