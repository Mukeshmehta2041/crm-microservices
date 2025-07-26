package com.crm.platform.common.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class ApiResponseTest {

    @Test
    void testSuccessResponse() {
        String testData = "test data";
        ApiResponse<String> response = ApiResponse.success(testData);
        
        assertTrue(response.isSuccess());
        assertEquals(testData, response.getData());
        assertNotNull(response.getMeta());
        assertNotNull(response.getMeta().getTimestamp());
        assertEquals("v1", response.getMeta().getVersion());
        assertNull(response.getErrors());
    }

    @Test
    void testErrorResponse() {
        String errorMessage = "Test error";
        ApiResponse<String> response = ApiResponse.error(errorMessage);
        
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("GENERAL_ERROR", response.getErrors().get(0).getCode());
        assertEquals(errorMessage, response.getErrors().get(0).getMessage());
    }

    @Test
    void testErrorResponseWithDetails() {
        ErrorDetail errorDetail = new ErrorDetail("VALIDATION_ERROR", "Invalid field", "email", "invalid-email", "EMAIL_FORMAT");
        List<ErrorDetail> errors = List.of(errorDetail);
        ApiResponse<String> response = ApiResponse.error(errors);
        
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("VALIDATION_ERROR", response.getErrors().get(0).getCode());
        assertEquals("Invalid field", response.getErrors().get(0).getMessage());
        assertEquals("email", response.getErrors().get(0).getField());
    }
}