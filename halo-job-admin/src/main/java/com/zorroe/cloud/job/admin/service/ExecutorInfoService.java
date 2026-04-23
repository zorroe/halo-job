package com.zorroe.cloud.job.admin.service;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.model.ExecutorDispatchTarget;

import java.util.List;

public interface ExecutorInfoService {

    void register(ExecutorInfo info);

    List<ExecutorInfo> getOnlineList();

    void checkHeartBeat();

    List<ExecutorDispatchTarget> route(JobInfo jobInfo);
}
