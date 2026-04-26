package com.zorroe.cloud.job.admin.trigger;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.core.util.CronUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CronTriggerParser implements TriggerParser {

    @Override
    public JobTriggerTypeEnum getType() {
        return JobTriggerTypeEnum.CRON;
    }

    @Override
    public void validate(TriggerDefinition definition) {
        String cronExpression = definition.getRequiredString("cronExpression");
        if (!CronUtils.isValid(cronExpression)) {
            throw new IllegalArgumentException("invalid cron expression");
        }
    }

    @Override
    public Long computeInitialNextExecuteTime(TriggerDefinition definition, long currentTimeMillis) {
        return CronUtils.getNextFireTimeMillis(definition.getRequiredString("cronExpression"), new Date(currentTimeMillis));
    }

    @Override
    public TriggerDispatchPlan createDispatchPlan(JobInfo jobInfo, TriggerDefinition definition, long currentTimeMillis) {
        long anchor = jobInfo.getNextExecuteTime() == null ? currentTimeMillis : jobInfo.getNextExecuteTime();
        Long nextTime = CronUtils.getNextFireTimeMillis(definition.getRequiredString("cronExpression"), new Date(anchor));
        return new TriggerDispatchPlan(nextTime, false);
    }
}
