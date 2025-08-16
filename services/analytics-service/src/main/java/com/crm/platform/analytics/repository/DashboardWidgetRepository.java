package com.crm.platform.analytics.repository;

import com.crm.platform.analytics.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    List<DashboardWidget> findByDashboardIdAndIsActiveTrueOrderByPositionYAscPositionXAsc(Long dashboardId);

    Optional<DashboardWidget> findByIdAndDashboardId(Long id, Long dashboardId);

    @Query("SELECT w FROM DashboardWidget w WHERE w.dashboard.id = :dashboardId " +
           "AND w.dashboard.organizationId = :organizationId AND w.isActive = true " +
           "ORDER BY w.positionY ASC, w.positionX ASC")
    List<DashboardWidget> findByDashboardIdAndOrganizationId(
            @Param("dashboardId") Long dashboardId,
            @Param("organizationId") String organizationId
    );

    @Query("SELECT COUNT(w) FROM DashboardWidget w WHERE w.dashboard.id = :dashboardId AND w.isActive = true")
    long countActiveWidgetsByDashboardId(@Param("dashboardId") Long dashboardId);

    List<DashboardWidget> findByTypeAndIsActiveTrue(DashboardWidget.WidgetType type);
}