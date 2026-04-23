package com.zorroe.cloud.job.admin.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobTriggerSummary {

    private boolean success;

    private int totalCount;

    private int successCount;

    private int failCount;

    private String message;
}
