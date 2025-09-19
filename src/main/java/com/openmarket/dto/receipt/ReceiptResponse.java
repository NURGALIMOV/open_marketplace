package com.openmarket.dto.receipt;

import com.openmarket.entity.Receipt;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class ReceiptResponse {
    private UUID id;
    private String name;
    private String requestNumber;
    private LocalDate receiptDate;
    private UUID counterpartyContractId;
    private String counterpartyName;
    private String contract;
    private BigDecimal totalCost;
    private long itemsCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ReceiptResponse from(Receipt entity) {
        return ReceiptResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .requestNumber(entity.getRequestNumber())
                .receiptDate(entity.getReceiptDate())
                .counterpartyContractId(entity.getCounterpartyContract() != null ? entity.getCounterpartyContract().getId() : null)
                .counterpartyName(entity.getCounterpartyContract() != null ? entity.getCounterpartyContract().getCounterparty() : null)
                .contract(entity.getContract())
                .totalCost(entity.getTotalCost())
                .itemsCount(entity.getItems() != null ? entity.getItems().size() : 0)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ReceiptResponse from(Receipt entity, long itemsCount) {
        ReceiptResponse response = from(entity);
        response.setItemsCount(itemsCount);
        return response;
    }
}

