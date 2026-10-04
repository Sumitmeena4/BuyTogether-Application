package com.hackathon.gemma_app.repository;

import com.hackathon.gemma_app.entity.ShoppingGroup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingGroupRepository extends JpaRepository<ShoppingGroup, UUID> {
}
