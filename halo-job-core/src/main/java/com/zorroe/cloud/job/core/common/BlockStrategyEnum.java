package com.zorroe.cloud.job.core.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BlockStrategyEnum {
    SERIAL(1, "串行"),
    DISCARD(2, "丢弃"),
    COVER(3, "覆盖");

    private final Integer code;
    private final String desc;

    public static BlockStrategyEnum getByCode(Integer code) {
        for (BlockStrategyEnum e : values()) {
            if (e.getCode().equals(code)) return e;
        }
        return SERIAL;
    }
}
