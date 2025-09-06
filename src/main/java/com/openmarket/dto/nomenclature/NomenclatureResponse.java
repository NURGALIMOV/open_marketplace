package com.openmarket.dto.nomenclature;

import com.openmarket.entity.Nomenclature;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class NomenclatureResponse {
    private UUID id;
    private String article;
    private Long sku;
    private BigDecimal weight;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String note;
    /** Indicates if item was updated (for UI highlighting) */
    private boolean updated;

    public static NomenclatureResponse from(Nomenclature nomenclature) {
        return NomenclatureResponse.builder()
                .id(nomenclature.getId())
                .article(nomenclature.getArticle())
                .sku(nomenclature.getSku())
                .weight(nomenclature.getWeight())
                .createdAt(nomenclature.getCreatedAt())
                .updatedAt(nomenclature.getUpdatedAt())
                .note(nomenclature.getNote())
                .updated(nomenclature.isUpdated())
                .build();
    }
}
