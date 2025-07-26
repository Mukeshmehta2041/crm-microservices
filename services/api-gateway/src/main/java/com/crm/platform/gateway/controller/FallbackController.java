package com.crm.platform.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker
 * 
 * Provides fallback responses when services are unavailable
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> authServiceFallback() {
        return createFallbackResponse("Auth Service", 
            "Authentication service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/tenant")
    @PostMapping("/tenant")
    public ResponseEntity<Map<String, Object>> tenantServiceFallback() {
        return createFallbackResponse("Tenant Service", 
            "Tenant service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/users")
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> usersServiceFallback() {
        return createFallbackResponse("Users Service", 
            "Users service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/contacts")
    @PostMapping("/contacts")
    public ResponseEntity<Map<String, Object>> contactsServiceFallback() {
        return createFallbackResponse("Contacts Service", 
            "Contacts service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/deals")
    @PostMapping("/deals")
    public ResponseEntity<Map<String, Object>> dealsServiceFallback() {
        return createFallbackResponse("Deals Service", 
            "Deals service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/leads")
    @PostMapping("/leads")
    public ResponseEntity<Map<String, Object>> leadsServiceFallback() {
        return createFallbackResponse("Leads Service", 
            "Leads service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/accounts")
    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> accountsServiceFallback() {
        return createFallbackResponse("Accounts Service", 
            "Accounts service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/activities")
    @PostMapping("/activities")
    public ResponseEntity<Map<String, Object>> activitiesServiceFallback() {
        return createFallbackResponse("Activities Service", 
            "Activities service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/pipelines")
    @PostMapping("/pipelines")
    public ResponseEntity<Map<String, Object>> pipelinesServiceFallback() {
        return createFallbackResponse("Pipelines Service", 
            "Pipelines service is temporarily unavailable. Please try again later.");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName, String message) {
        Map<String, Object> response = Map.of(
            "success", false,
            "error", "Service Unavailable",
            "message", message,
            "service", serviceName,
            "timestamp", Instant.now().toString(),
            "fallback", true
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}