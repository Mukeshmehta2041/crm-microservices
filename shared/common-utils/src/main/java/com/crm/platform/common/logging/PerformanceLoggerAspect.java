package com.crm.platform.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging performance-related information.
 */
@Aspect
@Component
public class PerformanceLoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger("PERFORMANCE");

    @Around("@annotation(performanceLog)")
    public Object logPerformance(ProceedingJoinPoint joinPoint, PerformanceLog performanceLog) throws Throwable {
        String operationName = performanceLog.operation().isEmpty() ? 
            joinPoint.getSignature().getName() : performanceLog.operation();
        
        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        try {
            // Add performance context to MDC
            MDC.put("performance.operation", operationName);
            MDC.put("performance.category", performanceLog.category().toString());
            
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryUsed = endMemory - startMemory;
            
            MDC.put("performance.duration", String.valueOf(duration));
            MDC.put("performance.memory_used", String.valueOf(memoryUsed));
            
            // Log warning if operation exceeds threshold
            if (duration > performanceLog.thresholdMs()) {
                logger.warn("Slow operation detected: {} took {}ms (threshold: {}ms), memory used: {} bytes", 
                    operationName, duration, performanceLog.thresholdMs(), memoryUsed);
            } else {
                logger.debug("Operation completed: {} in {}ms, memory used: {} bytes", 
                    operationName, duration, memoryUsed);
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("performance.duration", String.valueOf(duration));
            MDC.put("performance.status", "ERROR");
            
            logger.error("Operation failed: {} after {}ms - {}", operationName, duration, e.getMessage());
            
            throw e;
        } finally {
            // Clean up MDC
            MDC.remove("performance.operation");
            MDC.remove("performance.category");
            MDC.remove("performance.duration");
            MDC.remove("performance.memory_used");
            MDC.remove("performance.status");
        }
    }
}