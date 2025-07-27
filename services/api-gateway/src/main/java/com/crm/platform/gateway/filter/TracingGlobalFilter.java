package com.crm.platform.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Global Filter for Request Tracing and Correlation ID
 * 
 * This filter adds correlation IDs to all requests and responses
 * for distributed tracing and logging
 */
@Component
public class TracingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(TracingGlobalFilter.class);
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String REQUEST_START_TIME = "request-start-time";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Generate or extract correlation ID
        String correlationId = getOrGenerateCorrelationId(request);
        String requestId = UUID.randomUUID().toString();
        String traceId = getOrGenerateTraceId(request);
        
        // Record request start time
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(REQUEST_START_TIME, startTime);
        
        // Add correlation ID to MDC for logging
        MDC.put("correlationId", correlationId);
        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);
        
        // Log incoming request
        logger.info("Incoming request: {} {} from {} - CorrelationId: {}, RequestId: {}, TraceId: {}",
                request.getMethod(),
                request.getURI(),
                getClientIp(request),
                correlationId,
                requestId,
                traceId);
        
        // Add headers to request
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(REQUEST_ID_HEADER, requestId)
                .header(TRACE_ID_HEADER, traceId)
                .header("X-Gateway-Timestamp", Instant.now().toString())
                .build();
        
        // Add headers to response
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signalType -> {
                    // Log response and calculate processing time
                    long endTime = System.currentTimeMillis();
                    long processingTime = endTime - startTime;
                    
                    logger.info("Request completed: {} {} - Status: {}, ProcessingTime: {}ms, CorrelationId: {}, RequestId: {}",
                            request.getMethod(),
                            request.getURI(),
                            exchange.getResponse().getStatusCode(),
                            processingTime,
                            correlationId,
                            requestId);
                    
                    // Add processing time to response headers
                    exchange.getResponse().getHeaders().add("X-Processing-Time", processingTime + "ms");
                    
                    // Clear MDC
                    MDC.clear();
                });
    }

    private String getOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String getOrGenerateTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String getClientIp(ServerHttpRequest request) {
        // Check for X-Forwarded-For header (proxy/load balancer)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check for X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -1; // Execute before other filters
    }
}