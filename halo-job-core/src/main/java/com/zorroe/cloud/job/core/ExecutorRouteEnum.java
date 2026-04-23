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

    /**
     * 根据路由策略编码返回对应枚举，未配置或无法识别时默认使用轮询。
     *
     * @param code 路由策略编码
     * @return 对应的路由策略
     */
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

    /**
     * 判断当前路由策略是否会把任务广播到所有在线执行器。
     *
     * @return 是否为分片广播策略
     */
    public boolean isBroadcast() {
        return this == SHARDING_BROADCAST;
    }
}
