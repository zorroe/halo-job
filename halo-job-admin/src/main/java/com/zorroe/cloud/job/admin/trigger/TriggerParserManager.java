package com.zorroe.cloud.job.admin.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorroe.cloud.job.admin.entity.JobInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TriggerParserManager {

    private static final TypeReference<Map<String, Object>> TRIGGER_CONFIG_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Map<JobTriggerTypeEnum, TriggerParser> parserMap;

    public TriggerParserManager(ObjectMapper objectMapper, List<TriggerParser> parsers) {
        this.objectMapper = objectMapper;
        this.parserMap = parsers.stream().collect(Collectors.toMap(TriggerParser::getType, Function.identity()));
    }

    public void prepareForPersist(JobInfo jobInfo) {
        TriggerDefinition definition = buildDefinition(jobInfo);
        if (definition == null) {
            jobInfo.setTriggerType(null);
            jobInfo.setTriggerConfig(null);
            jobInfo.setNextExecuteTime(null);
            return;
        }

        TriggerParser parser = requireParser(definition.getType());
        parser.validate(definition);
        Long nextExecuteTime = parser.computeInitialNextExecuteTime(definition, System.currentTimeMillis());

        jobInfo.setTriggerType(definition.getType().name());
        jobInfo.setTriggerConfig(writeConfig(definition.getConfig()));
        jobInfo.setNextExecuteTime(nextExecuteTime);
    }

    public Long refreshNextExecuteTime(JobInfo jobInfo) {
        TriggerDefinition definition = buildDefinition(jobInfo);
        if (definition == null) {
            return null;
        }
        TriggerParser parser = requireParser(definition.getType());
        parser.validate(definition);
        return parser.computeInitialNextExecuteTime(definition, System.currentTimeMillis());
    }

    public TriggerDispatchPlan buildDispatchPlan(JobInfo jobInfo, long currentTimeMillis) {
        TriggerDefinition definition = requireDefinition(jobInfo);
        TriggerParser parser = requireParser(definition.getType());
        return parser.createDispatchPlan(jobInfo, definition, currentTimeMillis);
    }

    public Long computeNextExecuteTimeAfterExecution(JobInfo jobInfo, long startTimeMillis, long endTimeMillis) {
        TriggerDefinition definition = requireDefinition(jobInfo);
        TriggerParser parser = requireParser(definition.getType());
        return parser.computeNextExecuteTimeAfterExecution(definition, startTimeMillis, endTimeMillis);
    }

    private TriggerDefinition requireDefinition(JobInfo jobInfo) {
        TriggerDefinition definition = buildDefinition(jobInfo);
        if (definition == null) {
            throw new IllegalStateException("job has no trigger definition: " + (jobInfo == null ? null : jobInfo.getId()));
        }
        return definition;
    }

    private TriggerDefinition buildDefinition(JobInfo jobInfo) {
        if (jobInfo == null) {
            return null;
        }

        String triggerTypeCode = jobInfo.getTriggerType();
        String triggerConfig = jobInfo.getTriggerConfig();
        boolean hasType = StringUtils.hasText(triggerTypeCode);
        boolean hasConfig = StringUtils.hasText(triggerConfig);
        if (!hasType && !hasConfig) {
            return null;
        }
        if (!hasType || !hasConfig) {
            throw new IllegalArgumentException("triggerType and triggerConfig must be configured together");
        }

        JobTriggerTypeEnum triggerType = JobTriggerTypeEnum.getByCode(triggerTypeCode);
        Map<String, Object> config = parseConfig(triggerConfig);
        return new TriggerDefinition(triggerType, config);
    }

    private TriggerParser requireParser(JobTriggerTypeEnum type) {
        TriggerParser parser = parserMap.get(type);
        if (parser == null) {
            throw new IllegalArgumentException("missing trigger parser for type: " + type);
        }
        return parser;
    }

    private Map<String, Object> parseConfig(String triggerConfig) {
        if (!StringUtils.hasText(triggerConfig)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(triggerConfig, TRIGGER_CONFIG_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid triggerConfig json", e);
        }
    }

    private String writeConfig(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config == null ? Map.of() : config);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to serialize triggerConfig", e);
        }
    }
}
