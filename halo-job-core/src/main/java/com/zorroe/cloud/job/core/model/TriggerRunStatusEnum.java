package com.zorroe.cloud.job.core.model;

public enum TriggerRunStatusEnum {
    SUCCESS,
    FAILED,
    DISCARDED,
    CANCELED;

    /**
     * 判断当前运行状态是否表示执行成功。
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
