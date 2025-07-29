package com.crm.platform.testing.api;

import com.crm.platform.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Base class for API integration tests using RestAssured
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class ApiTestBase {
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected String baseUrl;
    protected String authToken;
    protected UUID tenantId;
    protected UUID userId;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // Set up test data
        setupTestData();
        
        // Authenticate and get token
        authToken = authenticateTestUser();
        tenantId = getTestTenantId();
        userId = getTestUserId();
    }
    
    /**
     * Override this method to set up test-specific data
     */
    protected void setupTestData() {
        // Default implementation - override in subclasses
    }
    
    /**
     * Get authenticated request specification
     */
    protected RequestSpecification authenticatedRequest() {
        RequestSpecification spec = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
        
        if (authToken != null) {
            spec.header("Authorization", "Bearer " + authToken);
        }
        
        if (tenantId != null) {
            spec.header("X-Tenant-ID", tenantId.toString());
        }
        
        if (userId != null) {
            spec.header("X-User-ID", userId.toString());
        }
        
        return spec;
    }
    
    /**
     * Perform GET request and validate success response
     */
    protected Response getAndExpectSuccess(String endpoint) {
        return authenticatedRequest()
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue())
                .extract()
                .response();
    }
    
    /**
     * Perform POST request and validate created response
     */
    protected Response postAndExpectCreated(String endpoint, Object body) {
        return authenticatedRequest()
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data", notNullValue())
                .extract()
                .response();
    }
    
    /**
     * Perform PUT request and validate success response
     */
    protected Response putAndExpectSuccess(String endpoint, Object body) {
        return authenticatedRequest()
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue())
                .extract()
                .response();
    }
    
    /**
     * Perform DELETE request and validate success response
     */
    protected Response deleteAndExpectSuccess(String endpoint) {
        return authenticatedRequest()
                .when()
                .delete(endpoint)
                .then()
                .statusCode(204)
                .extract()
                .response();
    }
    
    /**
     * Perform request and expect validation error
     */
    protected Response postAndExpectValidationError(String endpoint, Object body) {
        return authenticatedRequest()
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors", notNullValue())
                .body("errors", hasSize(greaterThan(0)))
                .extract()
                .response();
    }
    
    /**
     * Perform request and expect not found error
     */
    protected Response getAndExpectNotFound(String endpoint) {
        return authenticatedRequest()
                .when()
                .get(endpoint)
                .then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("errors", notNullValue())
                .extract()
                .response();
    }
    
    /**
     * Perform request and expect unauthorized error
     */
    protected Response getAndExpectUnauthorized(String endpoint) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .get(endpoint)
                .then()
                .statusCode(401)
                .extract()
                .response();
    }
    
    /**
     * Extract data from API response
     */
    protected <T> T extractData(Response response, Class<T> dataType) {
        try {
            String dataJson = response.jsonPath().getString("data");
            return objectMapper.readValue(dataJson, dataType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract data from response", e);
        }
    }
    
    /**
     * Extract data from API response as Map
     */
    protected Map<String, Object> extractDataAsMap(Response response) {
        return response.jsonPath().getMap("data");
    }
    
    /**
     * Validate pagination metadata
     */
    protected void validatePaginationMetadata(Response response, int expectedPage, int expectedLimit) {
        response.then()
                .body("meta.pagination.page", equalTo(expectedPage))
                .body("meta.pagination.limit", equalTo(expectedLimit))
                .body("meta.pagination.total", greaterThanOrEqualTo(0))
                .body("meta.pagination.totalPages", greaterThanOrEqualTo(0))
                .body("meta.pagination.hasNext", notNullValue())
                .body("meta.pagination.hasPrev", notNullValue());
    }
    
    /**
     * Validate response metadata
     */
    protected void validateResponseMetadata(Response response) {
        response.then()
                .body("meta.timestamp", notNullValue())
                .body("meta.version", notNullValue());
    }
    
    /**
     * Validate error response structure
     */
    protected void validateErrorResponse(Response response, String expectedErrorCode) {
        response.then()
                .body("success", equalTo(false))
                .body("errors", notNullValue())
                .body("errors", hasSize(greaterThan(0)))
                .body("errors[0].code", equalTo(expectedErrorCode))
                .body("errors[0].message", notNullValue());
    }
    
    /**
     * Test pagination with different page sizes
     */
    protected void testPagination(String endpoint, int totalExpectedItems) {
        // Test first page
        Response firstPage = authenticatedRequest()
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        validatePaginationMetadata(firstPage, 1, 10);
        
        // Test different page sizes
        int[] pageSizes = {5, 20, 50};
        for (int pageSize : pageSizes) {
            authenticatedRequest()
                    .queryParam("page", 1)
                    .queryParam("limit", pageSize)
                    .when()
                    .get(endpoint)
                    .then()
                    .statusCode(200)
                    .body("meta.pagination.limit", equalTo(pageSize));
        }
    }
    
    /**
     * Test sorting functionality
     */
    protected void testSorting(String endpoint, String sortField) {
        // Test ascending sort
        authenticatedRequest()
                .queryParam("sort", sortField + ":asc")
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
        
        // Test descending sort
        authenticatedRequest()
                .queryParam("sort", sortField + ":desc")
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }
    
    /**
     * Test filtering functionality
     */
    protected void testFiltering(String endpoint, String filterField, String filterValue) {
        authenticatedRequest()
                .queryParam(filterField, filterValue)
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }
    
    /**
     * Test search functionality
     */
    protected void testSearch(String endpoint, String searchTerm) {
        authenticatedRequest()
                .queryParam("search", searchTerm)
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }
    
    /**
     * Test bulk operations
     */
    protected void testBulkOperation(String endpoint, Object bulkRequest) {
        authenticatedRequest()
                .body(bulkRequest)
                .when()
                .post(endpoint)
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(202)))
                .body("success", equalTo(true))
                .body("data.jobId", notNullValue())
                .body("data.status", notNullValue());
    }
    
    // Abstract methods to be implemented by concrete test classes
    protected abstract String authenticateTestUser();
    protected abstract UUID getTestTenantId();
    protected abstract UUID getTestUserId();
}