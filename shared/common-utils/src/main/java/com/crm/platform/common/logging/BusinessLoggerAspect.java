package com.crm.platform.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging business operations with structured format.
 */
@Aspect
@Component
public class BusinessLoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger("BUSINESS_OPERATIONS");

    @Around("@annotation(businessLog)")
    public Object logBusinessOperation(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {
        String operationName = businessLog.operation().isEmpty() ? 
            joinPoint.getSignature().getName() : businessLog.operation();
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Add business context to MDC
            MDC.put("business.operation", operationName);
            MDC.put("business.domain", businessLog.domain());
            MDC.put("business.action", businessLog.action().toString());
            
            logger.info("Business operation started: {}", operationName);
            
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("business.duration", String.valueOf(duration));
            MDC.put("business.status", "SUCCESS");
            
            logger.info("Business operation completed successfully: {} in {}ms", operationName, duration);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("business.duration", String.valueOf(duration));
            MDC.put("business.status", "FAILED");
            MDC.put("business.error", e.getClass().getSimpleName());
            
            logger.error("Business operation failed: {} in {}ms - {}", operationName, duration, e.getMessage(), e);
            
            throw e;
        } finally {
            // Clean up MDC
            MDC.remove("business.operation");
            MDC.remove("business.domain");
            MDC.remove("business.action");
            MDC.remove("business.duration");
            MDC.remove("business.status");
            MDC.remove("business.error");
        }
    }
}