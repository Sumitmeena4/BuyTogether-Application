package com.hackathon.gemma_app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {
    public enum Status { OPEN, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ShoppingGroup group;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false, length = 40)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    protected OrderItem() {
    }

    public OrderItem(ShoppingGroup group, String name, int totalQuantity, String unit) {
        this.group = group;
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.unit = unit;
    }

    public UUID getId() { return id; }
    public ShoppingGroup getGroup() { return group; }
    public String getName() { return name; }
    public int getTotalQuantity() { return totalQuantity; }
    public String getUnit() { return unit; }
    public Status getStatus() { return status; }

    public void update(String name, int totalQuantity, String unit) {
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.unit = unit;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
