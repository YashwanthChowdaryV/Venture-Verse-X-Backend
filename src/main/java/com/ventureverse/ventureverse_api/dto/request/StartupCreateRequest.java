package com.ventureverse.ventureverse_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartupCreateRequest {

    @NotBlank(message = "Startup name is required")
    private String startupName;

    @NotBlank(message = "Idea description is required")
    private String ideaDescription;

    @NotBlank(message = "Industry is required")
    private String industry;

    @NotBlank(message = "Target market is required")
    private String targetMarket;
}