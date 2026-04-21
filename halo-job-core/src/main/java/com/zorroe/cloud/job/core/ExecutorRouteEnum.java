package com.zorroe.cloud.job.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExecutorRouteEnum {

    ROUND(1, "轮询"),
    RANDOM(2, "随机");

    private final Integer code;
    private final String desc;

    public static ExecutorRouteEnum getByCode(Integer code) {
        for (ExecutorRouteEnum e : values()) {
            if (e.getCode().equals(code)) return e;
        }
        return ROUND;
    }
}
