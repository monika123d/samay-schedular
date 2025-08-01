package com.samay.scheduler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samay.scheduler.model.PollerConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Component
public class PollerConfigLoader {

    private Map<String, PollerConfig> configMap;


    @PostConstruct
    public void loadConfig() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("poller-config.json")) {
            configMap = mapper.readValue(is, new TypeReference<Map<String, PollerConfig>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load frequency-config.json", e);
        }
    }

    public PollerConfig getConfig(String scheduleType) {
        return configMap.get(scheduleType);
    }

    public Map<String, PollerConfig> getAllConfigs() {
        return configMap;
    }
}
