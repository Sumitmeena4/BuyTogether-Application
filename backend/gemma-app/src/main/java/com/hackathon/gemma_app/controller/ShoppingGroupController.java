package com.hackathon.gemma_app.controller;

import com.hackathon.gemma_app.dto.AnalyzeRequest;
import com.hackathon.gemma_app.dto.AnalyzeResultDto;
import com.hackathon.gemma_app.dto.CreateGroupRequest;
import com.hackathon.gemma_app.dto.CreateOrderItemRequest;
import com.hackathon.gemma_app.dto.GroupDto;
import com.hackathon.gemma_app.dto.GroupSummaryDto;
import com.hackathon.gemma_app.dto.OrderItemDto;
import com.hackathon.gemma_app.service.ShoppingGroupService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class ShoppingGroupController {
    private final ShoppingGroupService groupService;

    public ShoppingGroupController(ShoppingGroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupSummaryDto> create(@Valid @RequestBody CreateGroupRequest request) {
        GroupSummaryDto created = groupService.create(request);
        return ResponseEntity.created(URI.create("/api/groups/" + created.id())).body(created);
    }

    @GetMapping
    public List<GroupSummaryDto> list() {
        return groupService.list();
    }

    @GetMapping("/{id}")
    public GroupDto get(@PathVariable UUID id) {
        return groupService.get(id);
    }

    @PostMapping("/{id}/analyze")
    public AnalyzeResultDto analyze(@PathVariable UUID id, @Valid @RequestBody AnalyzeRequest request) {
        return groupService.analyze(id, request.conversation());
    }

    @GetMapping("/{id}/items")
    public List<OrderItemDto> items(@PathVariable UUID id) {
        return groupService.getItems(id);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderItemDto> addItem(@PathVariable UUID id,
            @Valid @RequestBody CreateOrderItemRequest request) {
        OrderItemDto item = groupService.addItem(id, request);
        return ResponseEntity.created(URI.create("/api/items/" + item.id())).body(item);
    }
}
