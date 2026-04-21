package com.zorroe.cloud.job.admin.service;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;

import java.util.List;

public interface ExecutorInfoService {

    void register(ExecutorInfo info);

    List<ExecutorInfo> getOnlineList();

    void checkHeartBeat();

    String route(Long jobId, Integer strategy);
}
