package com.crm.platform.pipelines.entity;

public enum MetricType {
    CONVERSION_RATE("Conversion Rate", "percentage", "Percentage of deals that convert from one stage to another"),
    AVERAGE_TIME_IN_STAGE("Average Time in Stage", "days", "Average number of days deals spend in a stage"),
    DEAL_COUNT("Deal Count", "count", "Number of deals in the pipeline or stage"),
    DEAL_VALUE("Deal Value", "currency", "Total value of deals in the pipeline or stage"),
    WIN_RATE("Win Rate", "percentage", "Percentage of deals that are won"),
    LOSS_RATE("Loss Rate", "percentage", "Percentage of deals that are lost"),
    VELOCITY("Velocity", "currency_per_day", "Average deal value per day through the pipeline"),
    FORECAST_ACCURACY("Forecast Accuracy", "percentage", "Accuracy of sales forecasts");

    private final String displayName;
    private final String defaultUnit;
    private final String description;

    MetricType(String displayName, String defaultUnit, String description) {
        this.displayName = displayName;
        this.defaultUnit = defaultUnit;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public String getDescription() {
        return description;
    }

    public static MetricType fromString(String value) {
        for (MetricType type : MetricType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown metric type: " + value);
    }
}