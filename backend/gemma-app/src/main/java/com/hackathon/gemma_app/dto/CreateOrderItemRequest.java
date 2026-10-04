package com.hackathon.gemma_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderItemRequest(
        @NotBlank @Size(max = 200) String name,
        @Positive int totalQuantity,
        @NotBlank @Size(max = 40) String unit) {
}
