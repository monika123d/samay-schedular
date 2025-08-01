package com.samay.scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @GetMapping("/start")
    public ResponseEntity<String> startWorkflow(@RequestParam String name) {
        return ResponseEntity.ok("👋 Welcome to " + name + " Workflow!");
    }

}
