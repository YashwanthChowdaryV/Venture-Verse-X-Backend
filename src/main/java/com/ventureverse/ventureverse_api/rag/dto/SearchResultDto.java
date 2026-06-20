package com.ventureverse.ventureverse_api.rag.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

    private String title;

    private String content;

    private Double score;
}