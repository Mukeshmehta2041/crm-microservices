package com.crm.platform.analytics.service;

import com.crm.platform.analytics.dto.DashboardRequest;
import com.crm.platform.analytics.dto.DashboardResponse;
import com.crm.platform.analytics.dto.DashboardWidgetRequest;
import com.crm.platform.analytics.dto.DashboardWidgetResponse;
import com.crm.platform.analytics.entity.Dashboard;
import com.crm.platform.analytics.entity.DashboardWidget;
import com.crm.platform.analytics.repository.DashboardRepository;
import com.crm.platform.analytics.repository.DashboardWidgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DashboardWidgetRepository widgetRepository;

    @Mock
    private AnalyticsQueryService analyticsQueryService;

    @Mock
    private AnalyticsCacheService cacheService;

    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardService dashboardService;

    private DashboardRequest dashboardRequest;
    private Dashboard dashboard;
    private String userId;
    private String organizationId;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        organizationId = "org-456";
        
        dashboardRequest = new DashboardRequest();
        dashboardRequest.setName("Test Dashboard");
        dashboardRequest.setDescription("Test Description");
        dashboardRequest.setIsDefault(false);
        
        dashboard = Dashboard.builder()
                .id(1L)
                .name("Test Dashboard")
                .description("Test Description")
                .createdBy(userId)
                .organizationId(organizationId)
                .isActive(true)
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createDashboard_ShouldCreateDashboard_WhenValidRequest() {
        // Given
        when(dashboardRepository.existsByNameAndOrganizationId(dashboardRequest.getName(), organizationId))
                .thenReturn(false);
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        
        DashboardResponse expectedResponse = DashboardResponse.builder()
                .id(1L)
                .name("Test Dashboard")
                .build();
        when(dashboardMapper.toResponse(dashboard)).thenReturn(expectedResponse);

        // When
        DashboardResponse result = dashboardService.createDashboard(dashboardRequest, userId, organizationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Dashboard");
        
        verify(dashboardRepository).save(any(Dashboard.class));
        verify(dashboardMapper).toResponse(dashboard);
    }

    @Test
    void createDashboard_ShouldThrowException_WhenNameAlreadyExists() {
        // Given
        when(dashboardRepository.existsByNameAndOrganizationId(dashboardRequest.getName(), organizationId))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> dashboardService.createDashboard(dashboardRequest, userId, organizationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dashboard with name 'Test Dashboard' already exists");
        
        verify(dashboardRepository, never()).save(any());
    }

    @Test
    void createDashboard_ShouldUnsetOtherDefaults_WhenSettingAsDefault() {
        // Given
        dashboardRequest.setIsDefault(true);
        Dashboard existingDefault = Dashboard.builder()
                .id(2L)
                .isDefault(true)
                .organizationId(organizationId)
                .build();
        
        when(dashboardRepository.existsByNameAndOrganizationId(dashboardRequest.getName(), organizationId))
                .thenReturn(false);
        when(dashboardRepository.findByOrganizationIdAndIsActiveTrueAndIsDefaultTrue(organizationId))
                .thenReturn(Optional.of(existingDefault));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toResponse(any())).thenReturn(DashboardResponse.builder().build());

        // When
        dashboardService.createDashboard(dashboardRequest, userId, organizationId);

        // Then
        verify(dashboardRepository).save(existingDefault);
        assertThat(existingDefault.getIsDefault()).isFalse();
    }

    @Test
    void getDashboard_ShouldReturnDashboard_WhenExists() {
        // Given
        Long dashboardId = 1L;
        when(dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId))
                .thenReturn(Optional.of(dashboard));
        
        DashboardResponse expectedResponse = DashboardResponse.builder()
                .id(dashboardId)
                .name("Test Dashboard")
                .build();
        when(dashboardMapper.toResponse(dashboard)).thenReturn(expectedResponse);

        // When
        DashboardResponse result = dashboardService.getDashboard(dashboardId, organizationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(dashboardId);
        verify(dashboardMapper).toResponse(dashboard);
    }

    @Test
    void getDashboard_ShouldThrowException_WhenNotFound() {
        // Given
        Long dashboardId = 1L;
        when(dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dashboardService.getDashboard(dashboardId, organizationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dashboard not found");
    }

    @Test
    void addWidget_ShouldAddWidget_WhenValidRequest() {
        // Given
        Long dashboardId = 1L;
        DashboardWidgetRequest widgetRequest = new DashboardWidgetRequest();
        widgetRequest.setTitle("Test Widget");
        widgetRequest.setType("CHART_LINE");
        widgetRequest.setQueryDefinition("test query");
        
        when(dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId))
                .thenReturn(Optional.of(dashboard));
        
        DashboardWidget savedWidget = DashboardWidget.builder()
                .id(1L)
                .dashboard(dashboard)
                .title("Test Widget")
                .type(DashboardWidget.WidgetType.CHART_LINE)
                .build();
        when(widgetRepository.save(any(DashboardWidget.class))).thenReturn(savedWidget);
        
        DashboardWidgetResponse expectedResponse = DashboardWidgetResponse.builder()
                .id(1L)
                .title("Test Widget")
                .build();
        when(dashboardMapper.toWidgetResponse(savedWidget)).thenReturn(expectedResponse);

        // When
        DashboardWidgetResponse result = dashboardService.addWidget(dashboardId, widgetRequest, organizationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Widget");
        
        verify(widgetRepository).save(any(DashboardWidget.class));
        verify(cacheService).invalidateDashboardCache(dashboardId, organizationId);
    }

    @Test
    void deleteDashboard_ShouldMarkAsInactive_WhenExists() {
        // Given
        Long dashboardId = 1L;
        when(dashboardRepository.findByIdAndOrganizationId(dashboardId, organizationId))
                .thenReturn(Optional.of(dashboard));

        // When
        dashboardService.deleteDashboard(dashboardId, organizationId);

        // Then
        assertThat(dashboard.getIsActive()).isFalse();
        verify(dashboardRepository).save(dashboard);
        verify(cacheService).invalidateDashboardCache(dashboardId, organizationId);
    }
}