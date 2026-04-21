package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.mapper.ExecutorInfoMapper;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.core.ExecutorRouteEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class ExecutorInfoServiceImpl implements ExecutorInfoService {

    @Resource
    private ExecutorInfoMapper executorInfoMapper;

    private Random random = new Random();


    @Override
    public void register(ExecutorInfo info) {
        executorInfoMapper.insertOrUpdate(info);
    }

    @Override
    public List<ExecutorInfo> getOnlineList() {
        return executorInfoMapper.listOnline();
    }

    @Override
    public void checkHeartBeat() {
        executorInfoMapper.updateOffline(30L);
    }

    @Override
    public String route(Long jobId, Integer strategy) {
        List<ExecutorInfo> list = getOnlineList();
        if (list == null || list.isEmpty()) return null;

        ExecutorRouteEnum route = ExecutorRouteEnum.getByCode(strategy);
        if (route == ExecutorRouteEnum.RANDOM) {
            return list.get(this.random.nextInt(list.size())).getExecutorAddress();
        } else {
            return list.get((int) (jobId % list.size())).getExecutorAddress();
        }
    }
}
