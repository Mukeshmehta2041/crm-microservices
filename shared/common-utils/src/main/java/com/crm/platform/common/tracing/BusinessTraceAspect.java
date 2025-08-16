package com.crm.platform.common.tracing;

import brave.Span;
import brave.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect for adding business context to distributed traces.
 */
@Aspect
@Component
public class BusinessTraceAspect {

  @Autowired
  private Tracer tracer;

  @Around("@annotation(traced)")
  public Object traceBusinessOperation(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
    Span span = tracer.nextSpan()
        .name(traced.operationName().isEmpty() ? joinPoint.getSignature().getName() : traced.operationName())
        .tag("business.operation", traced.operationName())
        .tag("business.domain", traced.domain())
        .start();

    try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
      // Add method parameters as tags if specified
      if (traced.includeParameters()) {
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length && i < 5; i++) { // Limit to 5 parameters
          if (args[i] != null) {
            span.tag("param." + i, args[i].toString());
          }
        }
      }

      Object result = joinPoint.proceed();

      // Add result information if specified
      if (traced.includeResult() && result != null) {
        span.tag("result.type", result.getClass().getSimpleName());
      }

      span.tag("status", "success");
      return result;

    } catch (Exception e) {
      span.tag("status", "error")
          .tag("error.class", e.getClass().getSimpleName())
          .tag("error.message", e.getMessage());
      throw e;
    } finally {
      span.finish();
    }
  }
}