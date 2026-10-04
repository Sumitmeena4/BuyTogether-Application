package com.hackathon.gemma_app.service;

import com.hackathon.gemma_app.ai.GemmaService;
import com.hackathon.gemma_app.dto.AnalyzeResultDto;
import com.hackathon.gemma_app.dto.CreateGroupRequest;
import com.hackathon.gemma_app.dto.CreateOrderItemRequest;
import com.hackathon.gemma_app.dto.GemmaRequestDto;
import com.hackathon.gemma_app.dto.GroupDto;
import com.hackathon.gemma_app.dto.GroupSummaryDto;
import com.hackathon.gemma_app.dto.OrderItemDto;
import com.hackathon.gemma_app.dto.ShoppingRequestDto;
import com.hackathon.gemma_app.dto.UpdateOrderItemRequest;
import com.hackathon.gemma_app.entity.OrderItem;
import com.hackathon.gemma_app.entity.ShoppingGroup;
import com.hackathon.gemma_app.entity.ShoppingRequest;
import com.hackathon.gemma_app.exception.InvalidAiResponseException;
import com.hackathon.gemma_app.exception.ResourceNotFoundException;
import com.hackathon.gemma_app.repository.OrderItemRepository;
import com.hackathon.gemma_app.repository.ShoppingGroupRepository;
import com.hackathon.gemma_app.repository.ShoppingRequestRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingGroupService {
    private final ShoppingGroupRepository groupRepository;
    private final ShoppingRequestRepository requestRepository;
    private final OrderItemRepository orderItemRepository;
    private final GemmaService gemmaService;
    private final ProductNormalizationService normalizationService;
    private final OrderAggregationService aggregationService;

    public ShoppingGroupService(ShoppingGroupRepository groupRepository,
            ShoppingRequestRepository requestRepository, OrderItemRepository orderItemRepository,
            GemmaService gemmaService, ProductNormalizationService normalizationService,
            OrderAggregationService aggregationService) {
        this.groupRepository = groupRepository;
        this.requestRepository = requestRepository;
        this.orderItemRepository = orderItemRepository;
        this.gemmaService = gemmaService;
        this.normalizationService = normalizationService;
        this.aggregationService = aggregationService;
    }

    @Transactional
    public GroupSummaryDto create(CreateGroupRequest request) {
        ShoppingGroup group = groupRepository.save(new ShoppingGroup(request.name().trim()));
        return GroupSummaryDto.from(group, 0);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryDto> list() {
        return groupRepository.findAll().stream()
                .sorted(Comparator.comparing(ShoppingGroup::getCreatedAt).reversed())
                .map(group -> GroupSummaryDto.from(group,
                        (int) orderItemRepository.countByGroup_Id(group.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDto get(UUID groupId) {
        return toDto(requireGroup(groupId));
    }

    @Transactional(readOnly = true)
    public List<OrderItemDto> getItems(UUID groupId) {
        requireGroup(groupId);
        return orderItemRepository.findByGroup_IdOrderByNameAsc(groupId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AnalyzeResultDto analyze(UUID groupId, String conversation) {
        ShoppingGroup group = requireGroup(groupId);
        List<GemmaRequestDto> extracted = gemmaService.analyze(conversation);
        if (extracted.size() > 200) {
            throw new InvalidAiResponseException("Gemma returned more than 200 requests.");
        }
        List<ShoppingRequest> validated = extracted.stream()
                .map(result -> toEntity(group, result))
                .toList();

        requestRepository.deleteByGroup_Id(groupId);
        requestRepository.saveAll(validated);
        requestRepository.flush();
        aggregationService.rebuild(group);

        GroupDto details = toDto(group);
        List<ShoppingRequestDto> ambiguous = details.requests().stream()
                .filter(ShoppingRequestDto::ambiguous)
                .toList();
        return new AnalyzeResultDto(details, validated.size(), details.items().size(), ambiguous);
    }

    @Transactional
    public OrderItemDto addItem(UUID groupId, CreateOrderItemRequest request) {
        ShoppingGroup group = requireGroup(groupId);
        String name = normalizationService.normalize(request.name());
        OrderItem item = orderItemRepository.save(new OrderItem(
                group, name, request.totalQuantity(), request.unit().trim()));
        return toDto(item);
    }

    @Transactional
    public OrderItemDto updateItem(UUID itemId, UpdateOrderItemRequest request) {
        OrderItem item = requireItem(itemId);
        item.update(normalizationService.normalize(request.name()),
                request.totalQuantity(), request.unit().trim());
        return toDto(item);
    }

    @Transactional
    public void deleteItem(UUID itemId) {
        orderItemRepository.delete(requireItem(itemId));
    }

    @Transactional
    public OrderItemDto updateStatus(UUID itemId, OrderItem.Status status) {
        OrderItem item = requireItem(itemId);
        item.setStatus(status);
        return toDto(item);
    }

    private ShoppingRequest toEntity(ShoppingGroup group, GemmaRequestDto result) {
        if (result == null || blank(result.person()) || blank(result.originalText()) || blank(result.item())) {
            throw new InvalidAiResponseException("Gemma returned a request missing its person, source text, or item.");
        }
        if (result.item().length() > 200 || result.person().length() > 120
                || result.originalText().length() > 2000) {
            throw new InvalidAiResponseException("Gemma returned a request with fields that exceed supported lengths.");
        }
        if (result.unit() == null || result.unit().isBlank() || result.unit().length() > 40) {
            throw new InvalidAiResponseException("Gemma returned a request with an invalid unit.");
        }
        if (result.quantity() != null && result.quantity() <= 0) {
            throw new InvalidAiResponseException("Gemma returned a non-positive quantity.");
        }
        if (result.confidence() != null
                && (!Double.isFinite(result.confidence())
                        || result.confidence() < 0 || result.confidence() > 1)) {
            throw new InvalidAiResponseException("Gemma returned a confidence outside the 0 to 1 range.");
        }
        boolean ambiguous = Boolean.TRUE.equals(result.ambiguous()) || result.quantity() == null;
        String reason = ambiguous
                ? (blank(result.ambiguityReason()) ? "Quantity or request details need clarification."
                        : result.ambiguityReason())
                : null;
        if (reason != null && reason.length() > 500) {
            throw new InvalidAiResponseException("Gemma returned an ambiguity reason that is too long.");
        }
        String proposedName = blank(result.normalizedItem()) ? result.item() : result.normalizedItem();
        String normalizedItem;
        try {
            normalizedItem = normalizationService.normalize(proposedName);
        } catch (IllegalArgumentException exception) {
            throw new InvalidAiResponseException("Gemma returned an invalid product name.", exception);
        }
        return new ShoppingRequest(group, result.person().trim(), result.originalText(),
                result.item().trim(), normalizedItem, result.quantity(),
                result.unit().trim(), result.confidence() == null ? 0.0 : result.confidence(),
                ambiguous, reason);
    }

    private GroupDto toDto(ShoppingGroup group) {
        List<ShoppingRequestDto> requests = requestRepository
                .findByGroup_IdOrderByPersonAscIdAsc(group.getId()).stream()
                .map(ShoppingRequestDto::from)
                .toList();
        List<OrderItemDto> items = orderItemRepository
                .findByGroup_IdOrderByNameAsc(group.getId()).stream()
                .map(item -> toDto(item, requests))
                .toList();
        return GroupDto.from(group, items, requests);
    }

    private OrderItemDto toDto(OrderItem item) {
        List<ShoppingRequestDto> requests = requestRepository
                .findByGroup_IdOrderByPersonAscIdAsc(item.getGroup().getId()).stream()
                .map(ShoppingRequestDto::from)
                .toList();
        return toDto(item, requests);
    }

    private OrderItemDto toDto(OrderItem item, List<ShoppingRequestDto> requests) {
        String itemKey = normalizationService.key(item.getName());
        List<ShoppingRequestDto> associated = requests.stream()
                .filter(request -> !request.ambiguous() && request.quantity() != null)
                .filter(request -> normalizationService.key(request.normalizedItem()).equals(itemKey))
                .filter(request -> request.unit().equalsIgnoreCase(item.getUnit()))
                .toList();
        return OrderItemDto.from(item, associated);
    }

    private ShoppingGroup requireGroup(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping group not found: " + id));
    }

    private OrderItem requireItem(UUID id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + id));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
