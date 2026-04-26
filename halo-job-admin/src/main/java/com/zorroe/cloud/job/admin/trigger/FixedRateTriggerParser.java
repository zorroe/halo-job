package com.zorroe.cloud.job.admin.trigger;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import org.springframework.stereotype.Component;

@Component
public class FixedRateTriggerParser implements TriggerParser {

    @Override
    public JobTriggerTypeEnum getType() {
        return JobTriggerTypeEnum.FIXED_RATE;
    }

    @Override
    public void validate(TriggerDefinition definition) {
        long intervalMillis = resolveIntervalMillis(definition);
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("FIXED_RATE interval must be greater than 0");
        }
    }

    @Override
    public Long computeInitialNextExecuteTime(TriggerDefinition definition, long currentTimeMillis) {
        return computeAlignedNextTime(definition, currentTimeMillis);
    }

    @Override
    public TriggerDispatchPlan createDispatchPlan(JobInfo jobInfo, TriggerDefinition definition, long currentTimeMillis) {
        long intervalMillis = resolveIntervalMillis(definition);
        long anchor = jobInfo.getNextExecuteTime() == null
                ? computeAlignedNextTime(definition, currentTimeMillis)
                : jobInfo.getNextExecuteTime();
        return new TriggerDispatchPlan(anchor + intervalMillis, false);
    }

    private Long computeAlignedNextTime(TriggerDefinition definition, long currentTimeMillis) {
        long intervalMillis = resolveIntervalMillis(definition);
        Long startAt = definition.getOptionalLong("startAt");
        if (startAt == null) {
            return currentTimeMillis + intervalMillis;
        }
        if (startAt > currentTimeMillis) {
            return startAt;
        }
        long elapsed = currentTimeMillis - startAt;
        long steps = Math.floorDiv(elapsed, intervalMillis) + 1;
        return startAt + steps * intervalMillis;
    }

    private long resolveIntervalMillis(TriggerDefinition definition) {
        Long intervalMillis = definition.getOptionalLong("intervalMillis");
        if (intervalMillis != null) {
            return intervalMillis;
        }
        Long intervalSeconds = definition.getOptionalLong("intervalSeconds");
        if (intervalSeconds == null) {
            throw new IllegalArgumentException("missing trigger config: intervalSeconds");
        }
        return intervalSeconds * 1000L;
    }
}
