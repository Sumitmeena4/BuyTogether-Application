package com.hackathon.gemma_app.dto;

public record GemmaRequestDto(
        String person,
        String originalText,
        String item,
        String normalizedItem,
        Integer quantity,
        String unit,
        Double confidence,
        Boolean ambiguous,
        String ambiguityReason) {
}
