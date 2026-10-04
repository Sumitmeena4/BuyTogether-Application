package com.hackathon.gemma_app.service;

import com.hackathon.gemma_app.entity.OrderItem;
import com.hackathon.gemma_app.entity.ShoppingGroup;
import com.hackathon.gemma_app.entity.ShoppingRequest;
import com.hackathon.gemma_app.repository.OrderItemRepository;
import com.hackathon.gemma_app.repository.ShoppingRequestRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderAggregationService {
    private final ProductNormalizationService normalizationService;
    private final ShoppingRequestRepository requestRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderAggregationService(ProductNormalizationService normalizationService,
            ShoppingRequestRepository requestRepository, OrderItemRepository orderItemRepository) {
        this.normalizationService = normalizationService;
        this.requestRepository = requestRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public List<OrderItem> rebuild(ShoppingGroup group) {
        List<ShoppingRequest> requests = requestRepository.findByGroup_IdOrderByPersonAscIdAsc(group.getId());
        Map<String, Aggregate> totals = new LinkedHashMap<>();
        for (ShoppingRequest request : requests) {
            if (request.isAmbiguous() || request.getQuantity() == null) {
                continue;
            }
            String displayName = normalizationService.normalize(request.getNormalizedItem());
            String unit = request.getUnit().trim();
            String key = normalizationService.key(displayName) + "\u0000" + unit.toLowerCase(Locale.ROOT);
            Aggregate aggregate = totals.computeIfAbsent(key, ignored -> new Aggregate(displayName, unit));
            try {
                aggregate.total = Math.addExact(aggregate.total, request.getQuantity());
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Combined quantity for " + displayName + " exceeds the supported maximum.", exception);
            }
        }

        orderItemRepository.deleteByGroup_Id(group.getId());
        List<OrderItem> items = totals.values().stream()
                .map(aggregate -> new OrderItem(group, aggregate.name, aggregate.total, aggregate.unit))
                .toList();
        return orderItemRepository.saveAll(items);
    }

    private static final class Aggregate {
        private final String name;
        private final String unit;
        private int total;

        private Aggregate(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }
    }
}
