package com.samay.scheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Service
@Slf4j
public class WorkflowService {

    private final RestTemplate restTemplate;

    @Autowired
    public WorkflowService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void triggerWorkflow(String triggerSource) {
        // Build request payload
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of("source", triggerSource);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity("http://workflow-service/trigger", request, Void.class);
    }

}
