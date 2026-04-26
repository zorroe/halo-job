package com.zorroe.cloud.job.admin.trigger;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TriggerDispatchPlan {

    private Long nextExecuteTimeBeforeDispatch;

    private boolean updateAfterExecution;
}
