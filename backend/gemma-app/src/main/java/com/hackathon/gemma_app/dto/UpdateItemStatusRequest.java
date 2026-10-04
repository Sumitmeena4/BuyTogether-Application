package com.hackathon.gemma_app.dto;

import com.hackathon.gemma_app.entity.OrderItem;
import jakarta.validation.constraints.NotNull;

public record UpdateItemStatusRequest(@NotNull OrderItem.Status status) {
}
