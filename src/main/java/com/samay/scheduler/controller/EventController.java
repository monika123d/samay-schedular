package com.samay.scheduler.controller;

import com.samay.scheduler.dto.EventRequest;
import com.samay.scheduler.event.DataChangeEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/publish")
    public ResponseEntity<String> publishEvent(@Valid @RequestBody EventRequest request) {
        eventPublisher.publishEvent(new DataChangeEvent(this,
                request.getEntityName(),
                request.getFieldName(),
                request.getNewValue()));
        return ResponseEntity.ok("Event published successfully for entity: " + request.getEntityName());
    }
}