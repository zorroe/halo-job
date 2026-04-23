package com.zorroe.cloud.job.admin.service;

import com.zorroe.cloud.job.admin.entity.JobInfo;
import com.zorroe.cloud.job.admin.model.JobTriggerSummary;
import com.zorroe.cloud.job.core.model.TriggerTypeEnum;

public interface JobTriggerService {

    JobTriggerSummary trigger(JobInfo jobInfo, TriggerTypeEnum triggerType);
}
