package com.openmarket.dto.counterparty;

import com.openmarket.entity.CounterpartyContract;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class CounterpartyContractResponse {
    private UUID id;
    private String counterparty;
    private String contract;
    private LocalDate contractDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CounterpartyContractResponse from(CounterpartyContract entity) {
        return CounterpartyContractResponse.builder()
                .id(entity.getId())
                .counterparty(entity.getCounterparty())
                .contract(entity.getContract())
                .contractDate(entity.getContractDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

