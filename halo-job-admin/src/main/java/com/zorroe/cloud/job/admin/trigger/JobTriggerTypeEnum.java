package com.zorroe.cloud.job.admin.trigger;

public enum JobTriggerTypeEnum {
    CRON,
    ONCE,
    FIXED_RATE,
    FIXED_DELAY;

    public static JobTriggerTypeEnum getByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (JobTriggerTypeEnum value : values()) {
            if (value.name().equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unsupported triggerType: " + code);
    }
}
