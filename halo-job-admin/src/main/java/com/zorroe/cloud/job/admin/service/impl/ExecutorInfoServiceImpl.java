package com.zorroe.cloud.job.admin.service.impl;

import com.zorroe.cloud.job.admin.entity.ExecutorHandlerInfo;
import com.zorroe.cloud.job.admin.entity.ExecutorInfo;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.mapper.ExecutorHandlerMapper;
import com.zorroe.cloud.job.admin.mapper.ExecutorInfoMapper;
import com.zorroe.cloud.job.admin.model.ExecutorDispatchTarget;
import com.zorroe.cloud.job.admin.service.ExecutorInfoService;
import com.zorroe.cloud.job.core.ExecutorRouteEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ExecutorInfoServiceImpl implements ExecutorInfoService {

    @Resource
    private ExecutorInfoMapper executorInfoMapper;

    @Resource
    private ExecutorHandlerMapper executorHandlerMapper;

    private final ConcurrentMap<Long, AtomicInteger> roundCounters = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(ExecutorInfo info, List<ExecutorHandlerInfo> handlers) {
        normalizeExecutorInfo(info);
        if (handlers == null || handlers.isEmpty()) {
            throw new IllegalArgumentException("executor handlers are required during register");
        }
        handlers.forEach(this::normalizeHandlerInfo);
        validateHandlerConsistency(info, handlers);
        executorInfoMapper.insertOrUpdate(info);
        executorHandlerMapper.deleteByExecutorAddress(info.getExecutorAddress());
        executorHandlerMapper.batchInsert(handlers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void beat(ExecutorInfo info) {
        normalizeExecutorInfo(info);
        executorInfoMapper.insertOrUpdate(info);
    }

    @Override
    public List<ExecutorInfo> getOnlineList() {
        return executorInfoMapper.listExecutors(null, null, 1);
    }

    @Override
    public List<ExecutorInfo> listExecutors(String executorGroup, String executorApp, Integer status) {
        return executorInfoMapper.listExecutors(normalizeFilterGroup(executorGroup), normalizeText(executorApp), status);
    }

    @Override
    public List<ExecutorHandlerInfo> listAvailableHandlers(String executorGroup, String executorApp) {
        return executorHandlerMapper.listAvailableHandlers(normalizeFilterGroup(executorGroup), normalizeText(executorApp));
    }

    @Override
    public void validateHandlerBinding(String executorGroup, String executorApp, String handlerName) {
        String normalizedGroup = normalizeGroup(executorGroup);
        String normalizedApp = requireApp(executorApp);
        String normalizedHandler = requireHandler(handlerName);
        List<ExecutorHandlerInfo> handlers = executorHandlerMapper.listRegisteredHandlers(
                normalizedGroup,
                normalizedApp,
                normalizedHandler
        );
        if (handlers.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "handler not registered: group=%s, app=%s, handler=%s",
                    normalizedGroup,
                    normalizedApp,
                    normalizedHandler
            ));
        }
        Set<String> signatures = handlers.stream()
                .map(ExecutorHandlerInfo::getMethodSignature)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (signatures.size() > 1) {
            throw new IllegalArgumentException(String.format(
                    "conflicting handler signatures found: group=%s, app=%s, handler=%s",
                    normalizedGroup,
                    normalizedApp,
                    normalizedHandler
            ));
        }
    }

    @Override
    public void checkHeartBeat() {
        executorInfoMapper.updateOffline(30L);
    }

    @Override
    public List<ExecutorDispatchTarget> route(JobInfo jobInfo) {
        List<ExecutorInfo> executors = new ArrayList<>(
                executorHandlerMapper.listMatchedOnlineExecutors(
                        normalizeGroup(jobInfo == null ? null : jobInfo.getExecutorGroup()),
                        normalizeText(jobInfo == null ? null : jobInfo.getExecutorApp()),
                        jobInfo == null ? null : jobInfo.getExecutorHandler()
                )
        );
        executors.sort(
                Comparator.comparing(ExecutorInfo::getExecutorGroup, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ExecutorInfo::getExecutorApp, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ExecutorInfo::getExecutorName, Comparator.nullsLast(String::compareTo))
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

    private void normalizeExecutorInfo(ExecutorInfo info) {
        if (info == null || !StringUtils.hasText(info.getExecutorName()) || !StringUtils.hasText(info.getExecutorAddress())) {
            throw new IllegalArgumentException("executor name and address are required");
        }
        info.setExecutorName(info.getExecutorName().trim());
        info.setExecutorAddress(info.getExecutorAddress().trim());
        info.setExecutorGroup(normalizeGroup(info.getExecutorGroup()));
        info.setExecutorApp(requireApp(info.getExecutorApp()));
        info.setMetadata(normalizeText(info.getMetadata()));
        info.setVersion(normalizeText(info.getVersion()));
    }

    private void normalizeHandlerInfo(ExecutorHandlerInfo info) {
        if (info == null || !StringUtils.hasText(info.getExecutorAddress()) || !StringUtils.hasText(info.getHandlerName())) {
            throw new IllegalArgumentException("executor handler metadata is invalid");
        }
        info.setExecutorName(normalizeText(info.getExecutorName()));
        info.setExecutorAddress(info.getExecutorAddress().trim());
        info.setExecutorGroup(normalizeGroup(info.getExecutorGroup()));
        info.setExecutorApp(requireApp(info.getExecutorApp()));
        info.setHandlerName(requireHandler(info.getHandlerName()));
        info.setHandlerDesc(normalizeText(info.getHandlerDesc()));
        info.setMethodSignature(normalizeText(info.getMethodSignature()));
    }

    private void validateHandlerConsistency(ExecutorInfo info, List<ExecutorHandlerInfo> handlers) {
        for (ExecutorHandlerInfo handler : handlers) {
            if (!info.getExecutorGroup().equals(handler.getExecutorGroup())) {
                throw new IllegalArgumentException("executor group mismatch between register payload and handler payload");
            }
            if (!info.getExecutorApp().equals(handler.getExecutorApp())) {
                throw new IllegalArgumentException("executor app mismatch between register payload and handler payload");
            }
            List<ExecutorHandlerInfo> existingHandlers = executorHandlerMapper.listRegisteredHandlers(
                    handler.getExecutorGroup(),
                    handler.getExecutorApp(),
                    handler.getHandlerName()
            );
            Set<String> existingSignatures = existingHandlers.stream()
                    .filter(item -> !info.getExecutorAddress().equals(item.getExecutorAddress()))
                    .map(ExecutorHandlerInfo::getMethodSignature)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            if (!existingSignatures.isEmpty()
                    && !existingSignatures.contains(handler.getMethodSignature())) {
                throw new IllegalArgumentException(String.format(
                        "handler signature conflict detected: group=%s, app=%s, handler=%s",
                        handler.getExecutorGroup(),
                        handler.getExecutorApp(),
                        handler.getHandlerName()
                ));
            }
        }
    }

    private String normalizeGroup(String executorGroup) {
        return StringUtils.hasText(executorGroup) ? executorGroup.trim() : "default";
    }

    private String normalizeFilterGroup(String executorGroup) {
        return StringUtils.hasText(executorGroup) ? executorGroup.trim() : null;
    }

    private String requireApp(String executorApp) {
        String normalized = normalizeText(executorApp);
        if (normalized == null) {
            throw new IllegalArgumentException("executorApp is required");
        }
        return normalized;
    }

    private String requireHandler(String handlerName) {
        String normalized = normalizeText(handlerName);
        if (normalized == null) {
            throw new IllegalArgumentException("handlerName is required");
        }
        return normalized;
    }

    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
