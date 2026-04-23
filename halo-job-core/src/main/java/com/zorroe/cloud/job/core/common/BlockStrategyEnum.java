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

    /**
     * 根据阻塞策略编码获取枚举，未配置时默认进入排队等待。
     *
     * @param code 阻塞策略编码
     * @return 对应的阻塞策略
     */
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
