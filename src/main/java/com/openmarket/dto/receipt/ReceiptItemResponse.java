package com.openmarket.dto.receipt;

import com.openmarket.entity.ReceiptItem;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class ReceiptItemResponse {
    private UUID id;
    private Long sku;
    private String article;
    private Integer quantity;
    private BigDecimal cost;
    private BigDecimal totalCost;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ReceiptItemResponse from(ReceiptItem entity) {
        return ReceiptItemResponse.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .article(entity.getArticle())
                .quantity(entity.getQuantity())
                .cost(entity.getCost())
                .totalCost(entity.getTotalCost())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

