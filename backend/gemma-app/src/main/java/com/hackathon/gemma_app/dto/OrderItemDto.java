package com.hackathon.gemma_app.dto;

import com.hackathon.gemma_app.entity.OrderItem;
import java.util.List;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        String name,
        int totalQuantity,
        String unit,
        String status,
        List<ShoppingRequestDto> requests) {
    public static OrderItemDto from(OrderItem item, List<ShoppingRequestDto> requests) {
        return new OrderItemDto(item.getId(), item.getName(), item.getTotalQuantity(),
                item.getUnit(), item.getStatus().name(), requests);
    }
}
