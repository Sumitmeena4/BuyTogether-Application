package com.hackathon.gemma_app.repository;

import com.hackathon.gemma_app.entity.ShoppingRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingRequestRepository extends JpaRepository<ShoppingRequest, UUID> {
    List<ShoppingRequest> findByGroup_IdOrderByPersonAscIdAsc(UUID groupId);
    void deleteByGroup_Id(UUID groupId);
}
