package com.crm.platform.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging security-related operations.
 */
@Aspect
@Component
public class SecurityLoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Around("@annotation(securityLog)")
    public Object logSecurityOperation(ProceedingJoinPoint joinPoint, SecurityLog securityLog) throws Throwable {
        String operationName = securityLog.operation().isEmpty() ? 
            joinPoint.getSignature().getName() : securityLog.operation();
        
        try {
            // Add security context to MDC
            MDC.put("security.operation", operationName);
            MDC.put("security.type", securityLog.type().toString());
            MDC.put("security.risk", securityLog.riskLevel().toString());
            
            logger.info("Security operation initiated: {} [{}]", operationName, securityLog.type());
            
            Object result = joinPoint.proceed();
            
            MDC.put("security.status", "SUCCESS");
            logger.info("Security operation completed: {}", operationName);
            
            return result;
            
        } catch (Exception e) {
            MDC.put("security.status", "FAILED");
            MDC.put("security.error", e.getClass().getSimpleName());
            
            if (securityLog.riskLevel() == SecurityLog.RiskLevel.HIGH) {
                logger.error("HIGH RISK security operation failed: {} - {}", operationName, e.getMessage(), e);
            } else {
                logger.warn("Security operation failed: {} - {}", operationName, e.getMessage());
            }
            
            throw e;
        } finally {
            // Clean up MDC
            MDC.remove("security.operation");
            MDC.remove("security.type");
            MDC.remove("security.risk");
            MDC.remove("security.status");
            MDC.remove("security.error");
        }
    }
}