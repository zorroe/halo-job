package com.zorroe.cloud.job.admin.trigger;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import org.springframework.stereotype.Component;

@Component
public class FixedDelayTriggerParser implements TriggerParser {

    @Override
    public JobTriggerTypeEnum getType() {
        return JobTriggerTypeEnum.FIXED_DELAY;
    }

    @Override
    public void validate(TriggerDefinition definition) {
        long delayMillis = resolveDelayMillis(definition);
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("FIXED_DELAY delay must be greater than 0");
        }
    }

    @Override
    public Long computeInitialNextExecuteTime(TriggerDefinition definition, long currentTimeMillis) {
        return computeAlignedNextTime(definition, currentTimeMillis);
    }

    @Override
    public TriggerDispatchPlan createDispatchPlan(JobInfo jobInfo, TriggerDefinition definition, long currentTimeMillis) {
        return new TriggerDispatchPlan(null, true);
    }

    @Override
    public Long computeNextExecuteTimeAfterExecution(TriggerDefinition definition, long startTimeMillis, long endTimeMillis) {
        return endTimeMillis + resolveDelayMillis(definition);
    }

    private Long computeAlignedNextTime(TriggerDefinition definition, long currentTimeMillis) {
        long delayMillis = resolveDelayMillis(definition);
        Long startAt = definition.getOptionalLong("startAt");
        if (startAt == null) {
            return currentTimeMillis + delayMillis;
        }
        if (startAt > currentTimeMillis) {
            return startAt;
        }
        long elapsed = currentTimeMillis - startAt;
        long steps = Math.floorDiv(elapsed, delayMillis) + 1;
        return startAt + steps * delayMillis;
    }

    private long resolveDelayMillis(TriggerDefinition definition) {
        Long delayMillis = definition.getOptionalLong("delayMillis");
        if (delayMillis != null) {
            return delayMillis;
        }
        Long delaySeconds = definition.getOptionalLong("delaySeconds");
        if (delaySeconds == null) {
            throw new IllegalArgumentException("missing trigger config: delaySeconds");
        }
        return delaySeconds * 1000L;
    }
}
