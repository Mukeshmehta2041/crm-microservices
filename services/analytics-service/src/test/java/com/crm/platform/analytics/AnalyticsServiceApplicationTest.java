package com.crm.platform.analytics;

import com.crm.platform.analytics.dto.DashboardRequest;
import com.crm.platform.analytics.dto.DashboardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsServiceApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }

    @Test
    void createDashboard_ShouldReturnCreated_WhenValidRequest() throws Exception {
        // Given
        DashboardRequest request = new DashboardRequest();
        request.setName("Test Dashboard");
        request.setDescription("Test Description");
        request.setIsDefault(false);

        // When & Then
        mockMvc.perform(post("/api/v1/dashboards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "test-user")
                        .header("X-Organization-Id", "test-org")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Dashboard"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    void getDashboards_ShouldReturnPagedResults() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/dashboards")
                        .header("X-Organization-Id", "test-org")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void executeAnalyticsQuery_ShouldReturnResults_WhenValidQuery() throws Exception {
        // Given
        String queryRequest = """
                {
                    "query": "measurement:user_activity",
                    "limit": 100
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Organization-Id", "test-org")
                        .content(queryRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.metadata").exists())
                .andExpect(jsonPath("$.executedAt").exists());
    }

    @Test
    void getRealtimeMetrics_ShouldReturnMetrics() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/metrics/realtime")
                        .header("X-Organization-Id", "test-org")
                        .param("measurement", "user_activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createReport_ShouldReturnCreated_WhenValidRequest() throws Exception {
        // Given
        String reportRequest = """
                {
                    "name": "Test Report",
                    "description": "Test Description",
                    "reportType": "DASHBOARD",
                    "queryDefinition": "SELECT * FROM user_activity",
                    "isScheduled": false
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "test-user")
                        .header("X-Organization-Id", "test-org")
                        .content(reportRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Report"))
                .andExpect(jsonPath("$.reportType").value("DASHBOARD"));
    }
}