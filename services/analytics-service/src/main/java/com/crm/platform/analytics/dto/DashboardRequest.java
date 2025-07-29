package com.crm.platform.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DashboardRequest {

    @NotBlank(message = "Dashboard name is required")
    private String name;

    private String description;

    private Boolean isDefault = false;
}