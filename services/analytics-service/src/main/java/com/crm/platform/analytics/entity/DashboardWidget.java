package com.crm.platform.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "dashboard_widgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DashboardWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WidgetType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String queryDefinition;

    @ElementCollection
    @CollectionTable(name = "widget_configuration", joinColumns = @JoinColumn(name = "widget_id"))
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value")
    private Map<String, String> configuration;

    @Column(nullable = false)
    private Integer positionX = 0;

    @Column(nullable = false)
    private Integer positionY = 0;

    @Column(nullable = false)
    private Integer width = 4;

    @Column(nullable = false)
    private Integer height = 3;

    @Column(nullable = false)
    private Integer refreshInterval = 30; // seconds

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum WidgetType {
        CHART_LINE,
        CHART_BAR,
        CHART_PIE,
        CHART_AREA,
        METRIC_SINGLE,
        METRIC_COMPARISON,
        TABLE,
        GAUGE,
        HEATMAP,
        FUNNEL
    }
}