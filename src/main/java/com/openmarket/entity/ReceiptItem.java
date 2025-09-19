package com.openmarket.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "receipt_items")
@EqualsAndHashCode(callSuper = false)
public class ReceiptItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;
    @Column
    private Long sku;
    @Column
    private String article;
    @Column
    private Integer quantity;
    @Column(precision = 18, scale = 2)
    private BigDecimal cost;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Calculate total cost for this item (quantity * cost)
     */
    public BigDecimal getTotalCost() {
        return (Objects.isNull(quantity) || Objects.isNull(cost)) ?
                BigDecimal.ZERO : cost.multiply(BigDecimal.valueOf(quantity));
    }
}

