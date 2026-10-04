package com.hackathon.gemma_app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "shopping_requests")
public class ShoppingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ShoppingGroup group;

    @Column(nullable = false, length = 120)
    private String person;

    @Column(nullable = false, length = 2000)
    private String originalText;

    @Column(nullable = false, length = 200)
    private String item;

    @Column(nullable = false, length = 200)
    private String normalizedItem;

    private Integer quantity;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private boolean ambiguous;

    @Column(length = 500)
    private String ambiguityReason;

    protected ShoppingRequest() {
    }

    public ShoppingRequest(ShoppingGroup group, String person, String originalText, String item,
            String normalizedItem, Integer quantity, String unit, double confidence,
            boolean ambiguous, String ambiguityReason) {
        this.group = group;
        this.person = person;
        this.originalText = originalText;
        this.item = item;
        this.normalizedItem = normalizedItem;
        this.quantity = quantity;
        this.unit = unit;
        this.confidence = confidence;
        this.ambiguous = ambiguous;
        this.ambiguityReason = ambiguityReason;
    }

    public UUID getId() { return id; }
    public ShoppingGroup getGroup() { return group; }
    public String getPerson() { return person; }
    public String getOriginalText() { return originalText; }
    public String getItem() { return item; }
    public String getNormalizedItem() { return normalizedItem; }
    public Integer getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public double getConfidence() { return confidence; }
    public boolean isAmbiguous() { return ambiguous; }
    public String getAmbiguityReason() { return ambiguityReason; }
}
