package com.ventureverse.ventureverse_api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartupResponse {

    private Long id;

    private String startupName;

    private String ideaDescription;

    private String industry;

    private String targetMarket;
}