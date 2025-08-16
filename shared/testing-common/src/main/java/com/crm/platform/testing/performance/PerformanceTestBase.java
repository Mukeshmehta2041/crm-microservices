package com.crm.platform.testing.performance;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for performance testing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class PerformanceTestBase {
    
    @LocalServerPort
    protected int port;
    
    protected String baseUrl;
    protected String authToken;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        RestAssured.baseURI = baseUrl;
        
        // Authenticate for performance tests
        authToken = authenticateForPerformanceTest();
    }
    
    /**
     * Test response time for a single request
     */
    protected void testResponseTime(String endpoint, Duration maxResponseTime) {
        Instant start = Instant.now();
        
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        Duration actualResponseTime = Duration.between(start, Instant.now());
        
        assertTrue(actualResponseTime.compareTo(maxResponseTime) <= 0,
                String.format("Response time %dms exceeded maximum %dms for endpoint %s",
                        actualResponseTime.toMillis(), maxResponseTime.toMillis(), endpoint));
    }
    
    /**
     * Test throughput with concurrent requests
     */
    protected PerformanceResult testThroughput(String endpoint, int concurrentUsers, int requestsPerUser) {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<CompletableFuture<RequestResult>> futures = new ArrayList<>();
        
        Instant testStart = Instant.now();
        
        // Submit concurrent requests
        for (int user = 0; user < concurrentUsers; user++) {
            CompletableFuture<RequestResult> future = CompletableFuture.supplyAsync(() -> {
                List<Duration> responseTimes = new ArrayList<>();
                int successCount = 0;
                int errorCount = 0;
                
                for (int request = 0; request < requestsPerUser; request++) {
                    Instant requestStart = Instant.now();
                    
                    try {
                        Response response = given()
                                .header("Authorization", "Bearer " + authToken)
                                .when()
                                .get(endpoint);
                        
                        Duration responseTime = Duration.between(requestStart, Instant.now());
                        responseTimes.add(responseTime);
                        
                        if (response.getStatusCode() == 200) {
                            successCount++;
                        } else {
                            errorCount++;
                        }
                    } catch (Exception e) {
                        errorCount++;
                        responseTimes.add(Duration.between(requestStart, Instant.now()));
                    }
                }
                
                return new RequestResult(responseTimes, successCount, errorCount);
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all requests to complete
        List<RequestResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        
        Duration totalTestTime = Duration.between(testStart, Instant.now());
        executor.shutdown();
        
        return calculatePerformanceMetrics(results, totalTestTime, concurrentUsers, requestsPerUser);
    }
    
    /**
     * Test load with gradually increasing users
     */
    protected LoadTestResult testLoad(String endpoint, int maxUsers, Duration rampUpTime, Duration testDuration) {
        List<PerformanceResult> results = new ArrayList<>();
        Duration stepDuration = rampUpTime.dividedBy(maxUsers);
        
        for (int users = 1; users <= maxUsers; users++) {
            System.out.println("Testing with " + users + " concurrent users");
            
            PerformanceResult result = testThroughput(endpoint, users, 10);
            results.add(result);
            
            // Check if error rate is too high
            if (result.getErrorRate() > 0.05) { // 5% error rate threshold
                System.out.println("Error rate exceeded threshold at " + users + " users");
                break;
            }
            
            // Wait before next step
            try {
                Thread.sleep(stepDuration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new LoadTestResult(results);
    }
    
    /**
     * Test stress by pushing beyond normal capacity
     */
    protected StressTestResult testStress(String endpoint, int startUsers, int maxUsers, int stepSize) {
        List<PerformanceResult> results = new ArrayList<>();
        int breakingPoint = -1;
        
        for (int users = startUsers; users <= maxUsers; users += stepSize) {
            System.out.println("Stress testing with " + users + " concurrent users");
            
            PerformanceResult result = testThroughput(endpoint, users, 5);
            results.add(result);
            
            // Check if system is breaking down
            if (result.getErrorRate() > 0.1 || result.getAverageResponseTime().toMillis() > 5000) {
                breakingPoint = users;
                System.out.println("Breaking point reached at " + users + " users");
                break;
            }
        }
        
        return new StressTestResult(results, breakingPoint);
    }
    
    /**
     * Test pagination performance
     */
    protected void testPaginationPerformance(String endpoint, int totalPages) {
        List<Duration> responseTimes = new ArrayList<>();
        
        for (int page = 1; page <= totalPages; page++) {
            Instant start = Instant.now();
            
            given()
                    .header("Authorization", "Bearer " + authToken)
                    .queryParam("page", page)
                    .queryParam("limit", 20)
                    .when()
                    .get(endpoint)
                    .then()
                    .statusCode(200);
            
            Duration responseTime = Duration.between(start, Instant.now());
            responseTimes.add(responseTime);
        }
        
        // Verify response times don't degrade significantly with page number
        Duration firstPageTime = responseTimes.get(0);
        Duration lastPageTime = responseTimes.get(responseTimes.size() - 1);
        
        assertTrue(lastPageTime.toMillis() <= firstPageTime.toMillis() * 3,
                "Last page response time should not be more than 3x first page time");
    }
    
    /**
     * Test bulk operation performance
     */
    protected void testBulkOperationPerformance(String endpoint, Object bulkRequest, int expectedRecords) {
        Instant start = Instant.now();
        
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .body(bulkRequest)
                .when()
                .post(endpoint)
                .then()
                .statusCode(anyOf(200, 202))
                .extract()
                .response();
        
        Duration responseTime = Duration.between(start, Instant.now());
        
        // Calculate throughput (records per second)
        double throughput = expectedRecords / (responseTime.toMillis() / 1000.0);
        
        System.out.println(String.format("Bulk operation processed %d records in %dms (%.2f records/sec)",
                expectedRecords, responseTime.toMillis(), throughput));
        
        // Assert minimum throughput
        assertTrue(throughput >= 10, "Bulk operation throughput should be at least 10 records/second");
    }
    
    private PerformanceResult calculatePerformanceMetrics(List<RequestResult> results, Duration totalTestTime, 
                                                         int concurrentUsers, int requestsPerUser) {
        List<Duration> allResponseTimes = results.stream()
                .flatMap(r -> r.responseTimes.stream())
                .sorted()
                .toList();
        
        int totalRequests = concurrentUsers * requestsPerUser;
        int totalSuccesses = results.stream().mapToInt(r -> r.successCount).sum();
        int totalErrors = results.stream().mapToInt(r -> r.errorCount).sum();
        
        Duration averageResponseTime = Duration.ofMillis(
                (long) allResponseTimes.stream().mapToLong(Duration::toMillis).average().orElse(0));
        
        Duration p95ResponseTime = allResponseTimes.get((int) (allResponseTimes.size() * 0.95));
        Duration p99ResponseTime = allResponseTimes.get((int) (allResponseTimes.size() * 0.99));
        
        double throughput = totalRequests / (totalTestTime.toMillis() / 1000.0);
        double errorRate = (double) totalErrors / totalRequests;
        
        return new PerformanceResult(
                totalRequests, totalSuccesses, totalErrors,
                averageResponseTime, p95ResponseTime, p99ResponseTime,
                throughput, errorRate, totalTestTime
        );
    }
    
    // Abstract method to be implemented by concrete test classes
    protected abstract String authenticateForPerformanceTest();
    
    // Result classes
    public static class RequestResult {
        final List<Duration> responseTimes;
        final int successCount;
        final int errorCount;
        
        public RequestResult(List<Duration> responseTimes, int successCount, int errorCount) {
            this.responseTimes = responseTimes;
            this.successCount = successCount;
            this.errorCount = errorCount;
        }
    }
    
    public static class PerformanceResult {
        private final int totalRequests;
        private final int successCount;
        private final int errorCount;
        private final Duration averageResponseTime;
        private final Duration p95ResponseTime;
        private final Duration p99ResponseTime;
        private final double throughput;
        private final double errorRate;
        private final Duration totalTime;
        
        public PerformanceResult(int totalRequests, int successCount, int errorCount,
                               Duration averageResponseTime, Duration p95ResponseTime, Duration p99ResponseTime,
                               double throughput, double errorRate, Duration totalTime) {
            this.totalRequests = totalRequests;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.averageResponseTime = averageResponseTime;
            this.p95ResponseTime = p95ResponseTime;
            this.p99ResponseTime = p99ResponseTime;
            this.throughput = throughput;
            this.errorRate = errorRate;
            this.totalTime = totalTime;
        }
        
        // Getters
        public int getTotalRequests() { return totalRequests; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public Duration getAverageResponseTime() { return averageResponseTime; }
        public Duration getP95ResponseTime() { return p95ResponseTime; }
        public Duration getP99ResponseTime() { return p99ResponseTime; }
        public double getThroughput() { return throughput; }
        public double getErrorRate() { return errorRate; }
        public Duration getTotalTime() { return totalTime; }
    }
    
    public static class LoadTestResult {
        private final List<PerformanceResult> results;
        
        public LoadTestResult(List<PerformanceResult> results) {
            this.results = results;
        }
        
        public List<PerformanceResult> getResults() { return results; }
        
        public int getMaxSupportedUsers() {
            return results.stream()
                    .mapToInt(r -> (int) (r.getTotalRequests() / 10)) // Assuming 10 requests per user
                    .max()
                    .orElse(0);
        }
    }
    
    public static class StressTestResult {
        private final List<PerformanceResult> results;
        private final int breakingPoint;
        
        public StressTestResult(List<PerformanceResult> results, int breakingPoint) {
            this.results = results;
            this.breakingPoint = breakingPoint;
        }
        
        public List<PerformanceResult> getResults() { return results; }
        public int getBreakingPoint() { return breakingPoint; }
    }
}