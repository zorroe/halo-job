package com.zorroe.cloud.job.admin.trigger;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import org.springframework.stereotype.Component;

@Component
public class OnceTriggerParser implements TriggerParser {

    @Override
    public JobTriggerTypeEnum getType() {
        return JobTriggerTypeEnum.ONCE;
    }

    @Override
    public void validate(TriggerDefinition definition) {
        definition.getRequiredLong("triggerAt");
    }

    @Override
    public Long computeInitialNextExecuteTime(TriggerDefinition definition, long currentTimeMillis) {
        long triggerAt = definition.getRequiredLong("triggerAt");
        if (triggerAt <= currentTimeMillis) {
            throw new IllegalArgumentException("ONCE triggerAt must be greater than current time");
        }
        return triggerAt;
    }

    @Override
    public TriggerDispatchPlan createDispatchPlan(JobInfo jobInfo, TriggerDefinition definition, long currentTimeMillis) {
        return new TriggerDispatchPlan(null, false);
    }
}
