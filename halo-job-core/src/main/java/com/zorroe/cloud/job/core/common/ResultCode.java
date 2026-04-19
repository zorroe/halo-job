package com.zorroe.cloud.job.core.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    NO_HANDLER(404, "任务处理器不存在"),
    JOB_NOT_EXIST(4001, "任务不存在"),
    JOB_STOPPED(4002, "任务已停止"),
    ;

    private final int code;
    private final String msg;
}