package com.zorroe.cloud.job.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExecutorRouteEnum {

    ROUND(1, "轮询"),
    RANDOM(2, "随机"),
    FIRST(3, "首个"),
    LAST(4, "末尾"),
    HASH(5, "哈希"),
    SHARDING_BROADCAST(6, "分片广播");

    private final Integer code;
    private final String desc;

    public static ExecutorRouteEnum getByCode(Integer code) {
        if (code == null) {
            return ROUND;
        }
        for (ExecutorRouteEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return ROUND;
    }

    public boolean isBroadcast() {
        return this == SHARDING_BROADCAST;
    }
}
