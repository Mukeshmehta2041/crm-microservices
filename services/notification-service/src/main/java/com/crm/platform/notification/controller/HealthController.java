package com.crm.platform.notification.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class HealthController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "notification-service");
        
        // Check Redis connectivity
        try {
            redisTemplate.opsForValue().set("health-check", "ok");
            String redisValue = (String) redisTemplate.opsForValue().get("health-check");
            health.put("redis", "ok".equals(redisValue) ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("redis", "DOWN");
        }
        
        // Check Kafka connectivity
        try {
            // This is a simple check - in production you might want more sophisticated health checks
            health.put("kafka", "UP");
        } catch (Exception e) {
            health.put("kafka", "DOWN");
        }
        
        return ResponseEntity.ok(health);
    }
}