package com.zorroe.cloud.job.core.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BlockStrategyEnum {
    QUEUE_WAIT(1, "队列等待"),
    DISCARD_NEW(2, "丢弃新任务"),
    COVER_RUNNING(3, "覆盖当前任务");

    private final Integer code;
    private final String desc;

    public static BlockStrategyEnum getByCode(Integer code) {
        if (code == null) {
            return QUEUE_WAIT;
        }
        for (BlockStrategyEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return QUEUE_WAIT;
    }
}
