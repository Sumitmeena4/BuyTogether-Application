package com.hackathon.gemma_app.dto;

import com.hackathon.gemma_app.entity.ShoppingRequest;
import java.util.UUID;

public record ShoppingRequestDto(
        UUID id,
        String person,
        String originalText,
        String item,
        String normalizedItem,
        Integer quantity,
        String unit,
        double confidence,
        boolean ambiguous,
        String ambiguityReason) {
    public static ShoppingRequestDto from(ShoppingRequest request) {
        return new ShoppingRequestDto(
                request.getId(), request.getPerson(), request.getOriginalText(),
                request.getItem(), request.getNormalizedItem(), request.getQuantity(),
                request.getUnit(), request.getConfidence(), request.isAmbiguous(),
                request.getAmbiguityReason());
    }
}
