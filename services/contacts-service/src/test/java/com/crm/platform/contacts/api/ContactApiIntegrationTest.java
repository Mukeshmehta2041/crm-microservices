package com.crm.platform.contacts.api;

import com.crm.platform.contacts.dto.ContactRequest;
import com.crm.platform.contacts.dto.ContactResponse;
import com.crm.platform.testing.api.ApiTestBase;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive API integration tests for Contact endpoints
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Contact API Integration Tests")
class ContactApiIntegrationTest extends ApiTestBase {
    
    private static final String CONTACTS_ENDPOINT = "/api/v1/contacts";
    private UUID createdContactId;
    
    @Test
    @DisplayName("Should create contact with valid data")
    void testCreateContact() {
        ContactRequest request = createValidContactRequest();
        
        Response response = postAndExpectCreated(CONTACTS_ENDPOINT, request);
        
        validateResponseMetadata(response);
        
        ContactResponse contact = extractData(response, ContactResponse.class);
        assertNotNull(contact.getId());
        assertEquals(request.getFirstName(), contact.getFirstName());
        assertEquals(request.getLastName(), contact.getLastName());
        assertEquals(request.getEmail(), contact.getEmail());
        
        createdContactId = contact.getId();
    }
    
    @Test
    @DisplayName("Should return validation errors for invalid contact data")
    void testCreateContactValidationErrors() {
        ContactRequest request = new ContactRequest();
        // Missing required fields
        
        Response response = postAndExpectValidationError(CONTACTS_ENDPOINT, request);
        
        validateErrorResponse(response, "VALIDATION_ERROR");
        
        response.then()
                .body("errors", hasSize(greaterThan(0)))
                .body("errors[0].field", notNullValue())
                .body("errors[0].message", notNullValue());
    }
    
    @Test
    @DisplayName("Should get contact by ID")
    void testGetContactById() {
        // First create a contact
        ContactRequest request = createValidContactRequest();
        Response createResponse = postAndExpectCreated(CONTACTS_ENDPOINT, request);
        UUID contactId = extractData(createResponse, ContactResponse.class).getId();
        
        // Then get it by ID
        Response response = getAndExpectSuccess(CONTACTS_ENDPOINT + "/" + contactId);
        
        ContactResponse contact = extractData(response, ContactResponse.class);
        assertEquals(contactId, contact.getId());
        assertEquals(request.getFirstName(), contact.getFirstName());
        assertEquals(request.getLastName(), contact.getLastName());
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent contact")
    void testGetNonExistentContact() {
        UUID nonExistentId = UUID.randomUUID();
        
        Response response = getAndExpectNotFound(CONTACTS_ENDPOINT + "/" + nonExistentId);
        
        validateErrorResponse(response, "ENTITY_NOT_FOUND");
    }
    
    @Test
    @DisplayName("Should update contact with valid data")
    void testUpdateContact() {
        // First create a contact
        ContactRequest createRequest = createValidContactRequest();
        Response createResponse = postAndExpectCreated(CONTACTS_ENDPOINT, createRequest);
        UUID contactId = extractData(createResponse, ContactResponse.class).getId();
        
        // Then update it
        ContactRequest updateRequest = createValidContactRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        
        Response response = putAndExpectSuccess(CONTACTS_ENDPOINT + "/" + contactId, updateRequest);
        
        ContactResponse updatedContact = extractData(response, ContactResponse.class);
        assertEquals("Updated", updatedContact.getFirstName());
        assertEquals("Name", updatedContact.getLastName());
    }
    
    @Test
    @DisplayName("Should delete contact")
    void testDeleteContact() {
        // First create a contact
        ContactRequest request = createValidContactRequest();
        Response createResponse = postAndExpectCreated(CONTACTS_ENDPOINT, request);
        UUID contactId = extractData(createResponse, ContactResponse.class).getId();
        
        // Then delete it
        deleteAndExpectSuccess(CONTACTS_ENDPOINT + "/" + contactId);
        
        // Verify it's gone
        getAndExpectNotFound(CONTACTS_ENDPOINT + "/" + contactId);
    }
    
    @Test
    @DisplayName("Should get contacts with pagination")
    void testGetContactsWithPagination() {
        // Create multiple contacts
        for (int i = 0; i < 25; i++) {
            ContactRequest request = createValidContactRequest();
            request.setFirstName("Contact" + i);
            postAndExpectCreated(CONTACTS_ENDPOINT, request);
        }
        
        // Test pagination
        testPagination(CONTACTS_ENDPOINT, 25);
    }
    
    @Test
    @DisplayName("Should sort contacts by different fields")
    void testContactSorting() {
        // Create contacts with different names
        String[] names = {"Alice", "Bob", "Charlie"};
        for (String name : names) {
            ContactRequest request = createValidContactRequest();
            request.setFirstName(name);
            postAndExpectCreated(CONTACTS_ENDPOINT, request);
        }
        
        // Test sorting
        testSorting(CONTACTS_ENDPOINT, "firstName");
        testSorting(CONTACTS_ENDPOINT, "lastName");
        testSorting(CONTACTS_ENDPOINT, "createdAt");
    }
    
    @Test
    @DisplayName("Should filter contacts by various criteria")
    void testContactFiltering() {
        // Create contacts with different attributes
        ContactRequest request1 = createValidContactRequest();
        request1.setCompany("Acme Corp");
        request1.setStatus("ACTIVE");
        postAndExpectCreated(CONTACTS_ENDPOINT, request1);
        
        ContactRequest request2 = createValidContactRequest();
        request2.setCompany("Tech Inc");
        request2.setStatus("INACTIVE");
        postAndExpectCreated(CONTACTS_ENDPOINT, request2);
        
        // Test filtering
        testFiltering(CONTACTS_ENDPOINT, "company", "Acme Corp");
        testFiltering(CONTACTS_ENDPOINT, "status", "ACTIVE");
    }
    
    @Test
    @DisplayName("Should search contacts by text")
    void testContactSearch() {
        // Create contacts with searchable content
        ContactRequest request = createValidContactRequest();
        request.setFirstName("Searchable");
        request.setLastName("Contact");
        request.setCompany("Unique Company Name");
        postAndExpectCreated(CONTACTS_ENDPOINT, request);
        
        // Test search
        testSearch(CONTACTS_ENDPOINT, "Searchable");
        testSearch(CONTACTS_ENDPOINT, "Unique Company");
    }
    
    @Test
    @DisplayName("Should perform advanced search with complex criteria")
    void testAdvancedSearch() {
        // Create test data
        ContactRequest request1 = createValidContactRequest();
        request1.setCompany("Tech Corp");
        request1.setLeadScore(85);
        postAndExpectCreated(CONTACTS_ENDPOINT, request1);
        
        ContactRequest request2 = createValidContactRequest();
        request2.setCompany("Sales Inc");
        request2.setLeadScore(45);
        postAndExpectCreated(CONTACTS_ENDPOINT, request2);
        
        // Advanced search request
        Map<String, Object> searchRequest = Map.of(
                "filters", List.of(
                        Map.of(
                                "field", "leadScore",
                                "operator", "GREATER_THAN",
                                "value", "80"
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
                        "limit", 10
                )
        );
        
        Response response = authenticatedRequest()
                .body(searchRequest)
                .when()
                .post(CONTACTS_ENDPOINT + "/search")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue())
                .extract()
                .response();
        
        validatePaginationMetadata(response, 1, 10);
    }
    
    @Test
    @DisplayName("Should perform bulk contact operations")
    void testBulkContactOperations() {
        List<ContactRequest> contacts = List.of(
                createValidContactRequest(),
                createValidContactRequest(),
                createValidContactRequest()
        );
        
        Map<String, Object> bulkRequest = Map.of(
                "operation", "CREATE",
                "data", contacts,
                "continueOnError", true,
                "batchSize", 10
        );
        
        testBulkOperation(CONTACTS_ENDPOINT + "/bulk", bulkRequest);
    }
    
    @Test
    @DisplayName("Should handle duplicate contact detection")
    void testDuplicateContactDetection() {
        // Create original contact
        ContactRequest original = createValidContactRequest();
        Response createResponse = postAndExpectCreated(CONTACTS_ENDPOINT, original);
        UUID originalId = extractData(createResponse, ContactResponse.class).getId();
        
        // Try to create duplicate
        ContactRequest duplicate = createValidContactRequest();
        duplicate.setEmail(original.getEmail()); // Same email
        
        Response duplicateResponse = authenticatedRequest()
                .body(duplicate)
                .when()
                .post(CONTACTS_ENDPOINT)
                .then()
                .statusCode(409) // Conflict
                .body("success", equalTo(false))
                .extract()
                .response();
        
        validateErrorResponse(duplicateResponse, "DUPLICATE_CONTACT");
        
        // Test find duplicates endpoint
        authenticatedRequest()
                .when()
                .get(CONTACTS_ENDPOINT + "/" + originalId + "/duplicates")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue());
    }
    
    @Test
    @DisplayName("Should handle contact relationships")
    void testContactRelationships() {
        // Create two contacts
        ContactRequest contact1 = createValidContactRequest();
        ContactRequest contact2 = createValidContactRequest();
        
        Response response1 = postAndExpectCreated(CONTACTS_ENDPOINT, contact1);
        Response response2 = postAndExpectCreated(CONTACTS_ENDPOINT, contact2);
        
        UUID contact1Id = extractData(response1, ContactResponse.class).getId();
        UUID contact2Id = extractData(response2, ContactResponse.class).getId();
        
        // Create relationship
        Map<String, Object> relationshipRequest = Map.of(
                "contactId", contact1Id,
                "relatedContactId", contact2Id,
                "relationshipType", "COLLEAGUE",
                "description", "Work together at same company"
        );
        
        authenticatedRequest()
                .body(relationshipRequest)
                .when()
                .post(CONTACTS_ENDPOINT + "/" + contact1Id + "/relationships")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
        
        // Get relationships
        authenticatedRequest()
                .when()
                .get(CONTACTS_ENDPOINT + "/" + contact1Id + "/relationships")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(greaterThan(0)));
    }
    
    @Test
    @DisplayName("Should handle contact enrichment")
    void testContactEnrichment() {
        // Create contact
        ContactRequest request = createValidContactRequest();
        Response createResponse = postAndExpectCreated(CONTACTS_ENDPOINT, request);
        UUID contactId = extractData(createResponse, ContactResponse.class).getId();
        
        // Enrich contact
        authenticatedRequest()
                .queryParam("sources", "linkedin", "clearbit")
                .when()
                .post(CONTACTS_ENDPOINT + "/" + contactId + "/enrich")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue());
    }
    
    @Test
    @DisplayName("Should handle contact analytics")
    void testContactAnalytics() {
        // Create some test contacts
        for (int i = 0; i < 10; i++) {
            ContactRequest request = createValidContactRequest();
            request.setCompany(i % 2 == 0 ? "Tech Corp" : "Sales Inc");
            postAndExpectCreated(CONTACTS_ENDPOINT, request);
        }
        
        // Get analytics
        authenticatedRequest()
                .queryParam("startDate", "2024-01-01T00:00:00Z")
                .queryParam("endDate", "2024-12-31T23:59:59Z")
                .queryParam("groupBy", "company")
                .when()
                .get(CONTACTS_ENDPOINT + "/analytics")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.totalContacts", greaterThan(0))
                .body("data.contactsByCompany", notNullValue());
    }
    
    @Test
    @DisplayName("Should require authentication")
    void testAuthenticationRequired() {
        getAndExpectUnauthorized(CONTACTS_ENDPOINT);
    }
    
    @Test
    @DisplayName("Should handle rate limiting")
    void testRateLimiting() {
        // This would test rate limiting by making many requests quickly
        // Implementation depends on your rate limiting configuration
        
        for (int i = 0; i < 5; i++) {
            Response response = getAndExpectSuccess(CONTACTS_ENDPOINT);
            
            // Check rate limit headers
            response.then()
                    .body("meta.rateLimit.limit", notNullValue())
                    .body("meta.rateLimit.remaining", notNullValue())
                    .body("meta.rateLimit.resetAt", notNullValue());
        }
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
        request.setTags(List.of("prospect", "qualified"));
        return request;
    }
    
    @Override
    protected String authenticateTestUser() {
        // Mock authentication - return a test JWT token
        return "test-jwt-token";
    }
    
    @Override
    protected UUID getTestTenantId() {
        return UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    }
    
    @Override
    protected UUID getTestUserId() {
        return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    }
}