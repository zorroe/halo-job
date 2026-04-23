package com.zorroe.cloud.job.core.model;

public enum TriggerRunStatusEnum {
    SUCCESS,
    FAILED,
    DISCARDED,
    CANCELED;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
