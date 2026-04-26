package com.zorroe.cloud.job.admin.service;

import com.zorroe.cloud.job.admin.entity.ExecutorHandlerInfo;
import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.model.ExecutorDispatchTarget;

import java.util.List;

public interface ExecutorInfoService {

    void register(ExecutorInfo info, List<ExecutorHandlerInfo> handlers);

    void beat(ExecutorInfo info);

    List<ExecutorInfo> getOnlineList();

    List<ExecutorInfo> listExecutors(String executorGroup, String executorApp, Integer status);

    List<ExecutorHandlerInfo> listAvailableHandlers(String executorGroup, String executorApp);

    void validateHandlerBinding(String executorGroup, String executorApp, String handlerName);

    void checkHeartBeat();

    List<ExecutorDispatchTarget> route(JobInfo jobInfo);
}
