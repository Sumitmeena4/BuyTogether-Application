package com.hackathon.gemma_app.repository;

import com.hackathon.gemma_app.entity.OrderItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByGroup_IdOrderByNameAsc(UUID groupId);
    Optional<OrderItem> findByIdAndGroup_Id(UUID id, UUID groupId);
    void deleteByGroup_Id(UUID groupId);
    long countByGroup_Id(UUID groupId);
}
