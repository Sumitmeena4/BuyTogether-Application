package com.hackathon.gemma_app.dto;

import com.hackathon.gemma_app.entity.ShoppingGroup;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupDto(
        UUID id,
        String name,
        Instant createdAt,
        List<OrderItemDto> items,
        List<ShoppingRequestDto> requests) {
    public static GroupDto from(ShoppingGroup group, List<OrderItemDto> items,
            List<ShoppingRequestDto> requests) {
        return new GroupDto(group.getId(), group.getName(), group.getCreatedAt(), items, requests);
    }
}
