package com.hackathon.gemma_app.controller;

import com.hackathon.gemma_app.dto.OrderItemDto;
import com.hackathon.gemma_app.dto.UpdateItemStatusRequest;
import com.hackathon.gemma_app.dto.UpdateOrderItemRequest;
import com.hackathon.gemma_app.service.ShoppingGroupService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class OrderItemController {
    private final ShoppingGroupService groupService;

    public OrderItemController(ShoppingGroupService groupService) {
        this.groupService = groupService;
    }

    @PatchMapping("/{id}")
    public OrderItemDto update(@PathVariable UUID id,
            @Valid @RequestBody UpdateOrderItemRequest request) {
        return groupService.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        groupService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public OrderItemDto status(@PathVariable UUID id,
            @Valid @RequestBody UpdateItemStatusRequest request) {
        return groupService.updateStatus(id, request.status());
    }
}
