package com.crm.platform.analytics.performance;

import com.crm.platform.analytics.dto.AnalyticsQueryRequest;
import com.crm.platform.analytics.service.AnalyticsQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AnalyticsPerformanceTest {

    @Mock
    private com.crm.platform.analytics.service.InfluxDBService influxDBService;

    @Mock
    private com.crm.platform.analytics.service.AnalyticsCacheService cacheService;

    @InjectMocks
    private AnalyticsQueryService analyticsQueryService;

    @Test
    void testConcurrentQueryExecution() throws InterruptedException {
        // Given
        when(cacheService.generateQueryHash(anyString(), any())).thenReturn("hash123");
        when(cacheService.getCachedQueryResult(anyString())).thenReturn(java.util.Optional.empty());
        when(cacheService.isCacheEnabled()).thenReturn(true);
        when(influxDBService.query(anyString())).thenReturn(List.of(Map.of("count", 1)));

        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setQuery("measurement:user_activity");
        request.setLimit(100);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        // When - Execute 100 concurrent queries
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                analyticsQueryService.executeQuery(request, "test-org");
            }, executor);
            futures.add(future);
        }

        // Wait for all queries to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long endTime = System.currentTimeMillis();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        long totalTime = endTime - startTime;
        double averageTime = totalTime / 100.0;
        
        assertThat(totalTime).isLessThan(10000); // Should complete within 10 seconds
        assertThat(averageTime).isLessThan(100); // Average query should be under 100ms
        
        System.out.println("Total time for 100 concurrent queries: " + totalTime + "ms");
        System.out.println("Average time per query: " + averageTime + "ms");
    }

    @Test
    void testQueryResponseTime() {
        // Given
        when(cacheService.generateQueryHash(anyString(), any())).thenReturn("hash123");
        when(cacheService.getCachedQueryResult(anyString())).thenReturn(java.util.Optional.empty());
        when(cacheService.isCacheEnabled()).thenReturn(true);
        when(influxDBService.query(anyString())).thenReturn(List.of(Map.of("count", 1)));

        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setQuery("measurement:user_activity");
        request.setLimit(1000);

        // When
        long startTime = System.currentTimeMillis();
        analyticsQueryService.executeQuery(request, "test-org");
        long endTime = System.currentTimeMillis();

        // Then
        long responseTime = endTime - startTime;
        assertThat(responseTime).isLessThan(1000); // Should respond within 1 second
        
        System.out.println("Query response time: " + responseTime + "ms");
    }

    @Test
    void testMemoryUsageUnderLoad() {
        // Given
        when(cacheService.generateQueryHash(anyString(), any())).thenReturn("hash123");
        when(cacheService.getCachedQueryResult(anyString())).thenReturn(java.util.Optional.empty());
        when(cacheService.isCacheEnabled()).thenReturn(true);
        when(influxDBService.query(anyString())).thenReturn(generateLargeDataset());

        AnalyticsQueryRequest request = new AnalyticsQueryRequest();
        request.setQuery("measurement:user_activity");
        request.setLimit(10000);

        // When
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        for (int i = 0; i < 50; i++) {
            analyticsQueryService.executeQuery(request, "test-org-" + i);
        }
        
        System.gc(); // Suggest garbage collection
        Thread.yield(); // Give GC a chance to run
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Then
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024); // Should use less than 100MB
        
        System.out.println("Memory used: " + (memoryUsed / 1024 / 1024) + "MB");
    }

    private List<Map<String, Object>> generateLargeDataset() {
        List<Map<String, Object>> data = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            data.add(Map.of(
                    "_time", "2023-01-01T" + String.format("%02d", i % 24) + ":00:00Z",
                    "_value", Math.random() * 100,
                    "user_id", "user-" + i,
                    "activity", "login"
            ));
        }
        return data;
    }
}