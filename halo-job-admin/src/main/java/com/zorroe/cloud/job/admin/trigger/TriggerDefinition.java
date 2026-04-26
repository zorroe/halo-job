package com.zorroe.cloud.job.admin.trigger;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class TriggerDefinition {

    private final JobTriggerTypeEnum type;

    private final Map<String, Object> config;

    public TriggerDefinition(JobTriggerTypeEnum type, Map<String, Object> config) {
        this.type = type;
        this.config = config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
    }

    public String getRequiredString(String key) {
        Object value = config.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing trigger config: " + key);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("missing trigger config: " + key);
        }
        return text;
    }

    public Long getRequiredLong(String key) {
        Long value = getOptionalLong(key);
        if (value == null) {
            throw new IllegalArgumentException("missing trigger config: " + key);
        }
        return value;
    }

    public Long getOptionalLong(String key) {
        Object value = config.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid numeric trigger config: " + key);
        }
    }
}
