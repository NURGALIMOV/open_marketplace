package com.openmarket.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "nomenclature")
@EqualsAndHashCode(callSuper = false)
public class Nomenclature {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    /** offer_id from Ozon */
    @Column
    private String article;
    /** SKU from Ozon */
    @Column(nullable = false)
    private Long sku;
    /** volume_weight from Ozon */
    @Column(precision = 10, scale = 4)
    private BigDecimal weight;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Column
    private String note;

    /**
     * Check if this nomenclature item was updated after creation (used for UI highlighting)
     */
    public boolean isUpdated() {
        return Objects.nonNull(createdAt) && Objects.nonNull(updatedAt) && !createdAt.isEqual(updatedAt);
    }
}
