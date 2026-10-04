package com.hackathon.gemma_app.dto;

import java.util.List;

public record AnalyzeResultDto(
        GroupDto group,
        int requestCount,
        int productCount,
        List<ShoppingRequestDto> ambiguousRequests) {
}
