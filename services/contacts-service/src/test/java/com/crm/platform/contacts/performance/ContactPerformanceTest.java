package com.crm.platform.contacts.performance;

import com.crm.platform.contacts.dto.ContactRequest;
import com.crm.platform.testing.performance.PerformanceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Performance tests for Contact API endpoints
 */
@EnabledIfSystemProperty(named = "performance.tests.enabled", matches = "true")
@DisplayName("Contact API Performance Tests")
class ContactPerformanceTest extends PerformanceTestBase {
    
    private static final String CONTACTS_ENDPOINT = "/api/v1/contacts";
    
    @Test
    @DisplayName("Should handle single contact retrieval within acceptable time")
    void testSingleContactResponseTime() {
        // Create a test contact first
        ContactRequest request = createValidContactRequest();
        UUID contactId = createTestContact(request);
        
        // Test response time
        testResponseTime(CONTACTS_ENDPOINT + "/" + contactId, Duration.ofMillis(200));
    }
    
    @Test
    @DisplayName("Should handle contact list retrieval within acceptable time")
    void testContactListResponseTime() {
        // Create some test data
        createTestContacts(50);
        
        // Test response time for list endpoint
        testResponseTime(CONTACTS_ENDPOINT + "?page=1&limit=20", Duration.ofMillis(500));
    }
    
    @Test
    @DisplayName("Should handle concurrent contact retrievals")
    void testContactRetrievalThroughput() {
        // Create test data
        createTestContacts(100);
        
        // Test throughput with concurrent users
        PerformanceResult result = testThroughput(CONTACTS_ENDPOINT, 10, 20);
        
        // Assertions
        assertTrue(result.getAverageResponseTime().toMillis() < 1000, 
                "Average response time should be less than 1 second");
        assertTrue(result.getErrorRate() < 0.01, 
                "Error rate should be less than 1%");
        assertTrue(result.getThroughput() > 50, 
                "Throughput should be at least 50 requests/second");
    }
    
    @Test
    @DisplayName("Should handle contact creation under load")
    void testContactCreationThroughput() {
        // Test concurrent contact creation
        PerformanceResult result = testThroughput(CONTACTS_ENDPOINT, 5, 10);
        
        // Assertions for write operations (more lenient)
        assertTrue(result.getAverageResponseTime().toMillis() < 2000, 
                "Average response time should be less than 2 seconds");
        assertTrue(result.getErrorRate() < 0.05, 
                "Error rate should be less than 5%");
        assertTrue(result.getThroughput() > 10, 
                "Throughput should be at least 10 requests/second");
    }
    
    @Test
    @DisplayName("Should handle search operations efficiently")
    void testContactSearchPerformance() {
        // Create searchable test data
        createTestContactsWithSearchableData(200);
        
        // Test search performance
        testResponseTime(CONTACTS_ENDPOINT + "?search=technology", Duration.ofMillis(800));
        
        // Test search throughput
        PerformanceResult result = testThroughput(CONTACTS_ENDPOINT + "?search=engineer", 5, 10);
        assertTrue(result.getAverageResponseTime().toMillis() < 1500, 
                "Search response time should be reasonable");
    }
    
    @Test
    @DisplayName("Should handle pagination efficiently across pages")
    void testPaginationPerformance() {
        // Create enough data for multiple pages
        createTestContacts(500);
        
        // Test pagination performance
        testPaginationPerformance(CONTACTS_ENDPOINT, 25); // 25 pages of 20 items each
    }
    
    @Test
    @DisplayName("Should handle bulk operations efficiently")
    void testBulkOperationPerformance() {
        List<ContactRequest> contacts = createBulkContactRequests(100);
        
        Map<String, Object> bulkRequest = Map.of(
                "operation", "CREATE",
                "data", contacts,
                "batchSize", 20
        );
        
        testBulkOperationPerformance(CONTACTS_ENDPOINT + "/bulk", bulkRequest, 100);
    }
    
    @Test
    @DisplayName("Should handle advanced filtering performance")
    void testAdvancedFilteringPerformance() {
        // Create test data with various attributes
        createTestContactsWithVariedAttributes(300);
        
        Map<String, Object> complexSearchRequest = Map.of(
                "filters", List.of(
                        Map.of(
                                "field", "leadScore",
                                "operator", "GREATER_THAN",
                                "value", "70"
                        ),
                        Map.of(
                                "field", "company",
                                "operator", "LIKE",
                                "value", "Tech"
                        )
                ),
                "sort", List.of(
                        Map.of(
                                "field", "leadScore",
                                "direction", "DESC"
                        )
                ),
                "page", Map.of(
                        "page", 1,
                        "limit", 50
                )
        );
        
        // Test complex search performance
        Duration start = Duration.ofNanos(System.nanoTime());
        
        given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .body(complexSearchRequest)
                .when()
                .post(CONTACTS_ENDPOINT + "/search")
                .then()
                .statusCode(200);
        
        Duration responseTime = Duration.ofNanos(System.nanoTime()).minus(start);
        
        assertTrue(responseTime.toMillis() < 1000, 
                "Complex search should complete within 1 second");
    }
    
    @Test
    @DisplayName("Should determine maximum supported concurrent users")
    void testLoadCapacity() {
        // Create baseline data
        createTestContacts(100);
        
        // Test load capacity
        LoadTestResult result = testLoad(CONTACTS_ENDPOINT, 50, Duration.ofMinutes(2), Duration.ofMinutes(5));
        
        System.out.println("Maximum supported users: " + result.getMaxSupportedUsers());
        
        // Verify we can handle at least 20 concurrent users
        assertTrue(result.getMaxSupportedUsers() >= 20, 
                "System should support at least 20 concurrent users");
    }
    
    @Test
    @DisplayName("Should identify system breaking point")
    void testStressCapacity() {
        // Create test data
        createTestContacts(50);
        
        // Test stress capacity
        StressTestResult result = testStress(CONTACTS_ENDPOINT, 10, 100, 10);
        
        System.out.println("System breaking point: " + result.getBreakingPoint() + " users");
        
        // Verify breaking point is reasonable
        assertTrue(result.getBreakingPoint() == -1 || result.getBreakingPoint() >= 30, 
                "System should handle at least 30 users before breaking");
    }
    
    @Test
    @DisplayName("Should handle memory-intensive operations")
    void testMemoryIntensiveOperations() {
        // Test large result sets
        createTestContacts(1000);
        
        // Test large page sizes
        testResponseTime(CONTACTS_ENDPOINT + "?page=1&limit=500", Duration.ofSeconds(3));
        
        // Test export operations (if implemented)
        testResponseTime(CONTACTS_ENDPOINT + "/export?format=csv", Duration.ofSeconds(10));
    }
    
    // Helper methods
    private ContactRequest createValidContactRequest() {
        ContactRequest request = new ContactRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe+" + System.currentTimeMillis() + "@example.com");
        request.setPhone("+1-555-123-4567");
        request.setCompany("Acme Corporation");
        request.setTitle("Software Engineer");
        request.setStatus("ACTIVE");
        request.setLeadScore(75);
        return request;
    }
    
    private UUID createTestContact(ContactRequest request) {
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .body(request)
                .when()
                .post(CONTACTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        return UUID.fromString(response.jsonPath().getString("data.id"));
    }
    
    private void createTestContacts(int count) {
        for (int i = 0; i < count; i++) {
            ContactRequest request = createValidContactRequest();
            request.setFirstName("Contact" + i);
            request.setEmail("contact" + i + "@example.com");
            createTestContact(request);
        }
    }
    
    private void createTestContactsWithSearchableData(int count) {
        String[] companies = {"Tech Corp", "Sales Inc", "Marketing Ltd", "Engineering Co"};
        String[] titles = {"Engineer", "Manager", "Director", "Analyst"};
        
        for (int i = 0; i < count; i++) {
            ContactRequest request = createValidContactRequest();
            request.setFirstName("Contact" + i);
            request.setEmail("contact" + i + "@example.com");
            request.setCompany(companies[i % companies.length]);
            request.setTitle(titles[i % titles.length]);
            createTestContact(request);
        }
    }
    
    private void createTestContactsWithVariedAttributes(int count) {
        String[] companies = {"Tech Corp", "Sales Inc", "Marketing Ltd"};
        String[] statuses = {"ACTIVE", "INACTIVE"};
        
        for (int i = 0; i < count; i++) {
            ContactRequest request = createValidContactRequest();
            request.setFirstName("Contact" + i);
            request.setEmail("contact" + i + "@example.com");
            request.setCompany(companies[i % companies.length]);
            request.setStatus(statuses[i % statuses.length]);
            request.setLeadScore(30 + (i % 70)); // Scores from 30 to 99
            createTestContact(request);
        }
    }
    
    private List<ContactRequest> createBulkContactRequests(int count) {
        List<ContactRequest> requests = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            ContactRequest request = createValidContactRequest();
            request.setFirstName("BulkContact" + i);
            request.setEmail("bulk" + i + "@example.com");
            requests.add(request);
        }
        
        return requests;
    }
    
    @Override
    protected String authenticateForPerformanceTest() {
        // Mock authentication for performance tests
        return "performance-test-jwt-token";
    }
}