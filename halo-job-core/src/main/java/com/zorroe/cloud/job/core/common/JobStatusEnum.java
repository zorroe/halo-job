package com.zorroe.cloud.job.core.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobStatusEnum {

    /**
     * 停止状态
     */
    STOP(0, "停止"),

    /**
     * 运行状态
     */
    RUN(1, "运行");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static JobStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (JobStatusEnum statusEnum : JobStatusEnum.values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
