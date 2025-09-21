package com.openmarket.dto.shipment;

import com.openmarket.entity.Shipment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class ShipmentResponse {
    private UUID id;
    private String shipmentNumber;
    private LocalDate shipmentDate;
    private Long totalQuantity;
    private long itemsCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ShipmentResponse from(Shipment entity) {
        return ShipmentResponse.builder()
                .id(entity.getId())
                .shipmentNumber(entity.getShipmentNumber())
                .shipmentDate(entity.getShipmentDate())
                .totalQuantity(entity.getTotalQuantity())
                .itemsCount(entity.getItems() != null ? entity.getItems().size() : 0)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ShipmentResponse from(Shipment entity, long itemsCount) {
        ShipmentResponse response = from(entity);
        response.setItemsCount(itemsCount);
        return response;
    }
}
