package com.hackathon.gemma_app.dto;

import com.hackathon.gemma_app.entity.ShoppingGroup;
import java.time.Instant;
import java.util.UUID;

public record GroupSummaryDto(UUID id, String name, Instant createdAt, int itemCount) {
    public static GroupSummaryDto from(ShoppingGroup group, int itemCount) {
        return new GroupSummaryDto(group.getId(), group.getName(), group.getCreatedAt(), itemCount);
    }
}
