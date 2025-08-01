package com.samay.scheduler.persistence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component; // Import and add this
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
@Converter // This tells JPA it's a converter
@Component // This makes it a Spring bean, allowing ObjectMapper injection
@Slf4j
public class JsonToMapConverter implements AttributeConverter<Map<String, String>, String> {
    private final ObjectMapper objectMapper;

    // Constructor injection is preferred for mandatory dependencies
    public JsonToMapConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Could not convert map to JSON string for database: {}", attribute, e);
            throw new IllegalArgumentException("Error converting map to JSON for database", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Could not convert JSON string from database to map: {}", dbData, e);
            throw new IllegalArgumentException("Error converting JSON from database to map", e);
        }
    }
}