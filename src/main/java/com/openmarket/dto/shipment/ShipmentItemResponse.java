package com.openmarket.dto.shipment;

import com.openmarket.entity.ShipmentItem;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class ShipmentItemResponse {
    private UUID id;
    private Long sku;
    private String article;
    private Integer quantity;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ShipmentItemResponse from(ShipmentItem entity) {
        return ShipmentItemResponse.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .article(entity.getArticle())
                .quantity(entity.getQuantity())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
