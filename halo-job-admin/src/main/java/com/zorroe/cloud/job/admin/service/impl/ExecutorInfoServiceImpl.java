package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.mapper.ExecutorInfoMapper;
import com.zorroe.cloud.job.admin.model.ExecutorDispatchTarget;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.core.ExecutorRouteEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExecutorInfoServiceImpl implements ExecutorInfoService {

    @Resource
    private ExecutorInfoMapper executorInfoMapper;

    private final ConcurrentMap<Long, AtomicInteger> roundCounters = new ConcurrentHashMap<>();

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
    public List<ExecutorDispatchTarget> route(JobInfo jobInfo) {
        List<ExecutorInfo> executors = new ArrayList<>(getOnlineList());
        executors.sort(
                Comparator.comparing(ExecutorInfo::getExecutorName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ExecutorInfo::getExecutorAddress, Comparator.nullsLast(String::compareTo))
        );

        if (executors.isEmpty()) {
            return List.of();
        }

        ExecutorRouteEnum route = ExecutorRouteEnum.getByCode(jobInfo == null ? null : jobInfo.getRouteStrategy());
        return switch (route) {
            case RANDOM -> List.of(toTarget(executors.get(ThreadLocalRandom.current().nextInt(executors.size())), 0, 1));
            case FIRST -> List.of(toTarget(executors.get(0), 0, 1));
            case LAST -> List.of(toTarget(executors.get(executors.size() - 1), 0, 1));
            case HASH -> List.of(toTarget(executors.get(selectHashIndex(jobInfo, executors.size())), 0, 1));
            case SHARDING_BROADCAST -> buildBroadcastTargets(executors);
            case ROUND -> List.of(toTarget(executors.get(selectRoundIndex(jobInfo, executors.size())), 0, 1));
        };
    }

    private List<ExecutorDispatchTarget> buildBroadcastTargets(List<ExecutorInfo> executors) {
        List<ExecutorDispatchTarget> targets = new ArrayList<>(executors.size());
        for (int i = 0; i < executors.size(); i++) {
            targets.add(toTarget(executors.get(i), i, executors.size()));
        }
        return targets;
    }

    private int selectRoundIndex(JobInfo jobInfo, int size) {
        Long key = jobInfo == null || jobInfo.getId() == null ? 0L : jobInfo.getId();
        AtomicInteger counter = roundCounters.computeIfAbsent(key, ignored -> new AtomicInteger(0));
        return Math.floorMod(counter.getAndIncrement(), size);
    }

    private int selectHashIndex(JobInfo jobInfo, int size) {
        String hashKey;
        if (jobInfo == null) {
            hashKey = "default";
        } else {
            hashKey = String.format(
                    "%s|%s|%s|%s",
                    jobInfo.getId(),
                    jobInfo.getJobName(),
                    jobInfo.getExecutorHandler(),
                    jobInfo.getExecutorParam()
            );
        }
        return Math.floorMod(hashKey.hashCode(), size);
    }

    private ExecutorDispatchTarget toTarget(ExecutorInfo info, int shardIndex, int shardTotal) {
        return new ExecutorDispatchTarget(info.getExecutorAddress(), shardIndex, shardTotal);
    }
}
