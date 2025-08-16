package com.crm.platform.testing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Base class for contract testing using WireMock
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class ContractTestBase {
    
    protected WireMockServer wireMockServer;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @BeforeEach
    void setUpWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .port(8089)
                .usingFilesUnderDirectory("src/test/resources/wiremock"));
        
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        
        setupDefaultStubs();
    }
    
    @AfterEach
    void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    /**
     * Set up default stubs for common service interactions
     */
    protected void setupDefaultStubs() {
        // Auth service stubs
        stubFor(post(urlEqualTo("/api/v1/auth/validate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createSuccessResponse(Map.of(
                                "valid", true,
                                "userId", "123e4567-e89b-12d3-a456-426614174000",
                                "tenantId", "123e4567-e89b-12d3-a456-426614174001"
                        )))));
        
        // User service stubs
        stubFor(get(urlMatching("/api/v1/users/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createSuccessResponse(Map.of(
                                "id", "123e4567-e89b-12d3-a456-426614174000",
                                "firstName", "John",
                                "lastName", "Doe",
                                "email", "john.doe@example.com"
                        )))));
        
        // Account service stubs
        stubFor(get(urlMatching("/api/v1/accounts/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createSuccessResponse(Map.of(
                                "id", "123e4567-e89b-12d3-a456-426614174002",
                                "name", "Acme Corporation",
                                "industry", "Technology"
                        )))));
        
        // Deal service stubs
        stubFor(get(urlMatching("/api/v1/deals\\?contactId=.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createSuccessResponse(Map.of(
                                "content", java.util.List.of(),
                                "totalElements", 0,
                                "totalPages", 0,
                                "size", 20,
                                "number", 0
                        )))));
    }
    
    /**
     * Create a success response JSON
     */
    protected String createSuccessResponse(Object data) {
        try {
            Map<String, Object> response = Map.of(
                    "success", true,
                    "data", data,
                    "meta", Map.of(
                            "timestamp", "2024-01-24T10:30:00Z",
                            "version", "v1"
                    )
            );
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create success response", e);
        }
    }
    
    /**
     * Create an error response JSON
     */
    protected String createErrorResponse(String errorCode, String errorMessage) {
        try {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "errors", java.util.List.of(Map.of(
                            "code", errorCode,
                            "message", errorMessage
                    )),
                    "meta", Map.of(
                            "timestamp", "2024-01-24T10:30:00Z",
                            "version", "v1"
                    )
            );
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create error response", e);
        }
    }
    
    /**
     * Stub external service call with success response
     */
    protected void stubExternalServiceSuccess(String method, String url, Object responseData) {
        stubFor(request(method, urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createSuccessResponse(responseData))));
    }
    
    /**
     * Stub external service call with error response
     */
    protected void stubExternalServiceError(String method, String url, int statusCode, String errorCode, String errorMessage) {
        stubFor(request(method, urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createErrorResponse(errorCode, errorMessage))));
    }
    
    /**
     * Stub external service call with delay
     */
    protected void stubExternalServiceWithDelay(String method, String url, Object responseData, int delayMs) {
        stubFor(request(method, urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createSuccessResponse(responseData))
                        .withFixedDelay(delayMs)));
    }
    
    /**
     * Verify external service was called
     */
    protected void verifyExternalServiceCalled(String method, String url) {
        verify(requestedFor(request(method, urlEqualTo(url))));
    }
    
    /**
     * Verify external service was called with specific body
     */
    protected void verifyExternalServiceCalledWithBody(String method, String url, String expectedBody) {
        verify(requestedFor(request(method, urlEqualTo(url))
                .withRequestBody(equalToJson(expectedBody))));
    }
    
    /**
     * Verify external service was not called
     */
    protected void verifyExternalServiceNotCalled(String method, String url) {
        verify(0, requestedFor(request(method, urlEqualTo(url))));
    }
    
    /**
     * Reset all WireMock stubs and requests
     */
    protected void resetWireMock() {
        wireMockServer.resetAll();
        setupDefaultStubs();
    }
}