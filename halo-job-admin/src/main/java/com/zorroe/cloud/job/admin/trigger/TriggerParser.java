package com.zorroe.cloud.job.admin.trigger;

import com.zorroe.cloud.job.admin.entity.JobInfo;

public interface TriggerParser {

    JobTriggerTypeEnum getType();

    void validate(TriggerDefinition definition);

    Long computeInitialNextExecuteTime(TriggerDefinition definition, long currentTimeMillis);

    TriggerDispatchPlan createDispatchPlan(JobInfo jobInfo, TriggerDefinition definition, long currentTimeMillis);

    default Long computeNextExecuteTimeAfterExecution(TriggerDefinition definition, long startTimeMillis, long endTimeMillis) {
        return null;
    }
}
