package com.openmarket.dto.shop;

import com.openmarket.entity.Shop;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Setter
@Getter
@Builder
public class ShopResponse {
    private UUID id;
    private String name;
    private String externalId;
    /** Don't expose the actual token */
    private boolean hasToken;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private long nomenclatureCount;
    
    public static ShopResponse from(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .externalId(shop.getExternalId())
                .hasToken(Objects.nonNull(shop.getTokenEncrypted()))
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .nomenclatureCount(Objects.nonNull(shop.getNomenclature()) ? shop.getNomenclature().size() : 0)
                .build();
    }
    
    public static ShopResponse from(Shop shop, long nomenclatureCount) {
        ShopResponse response = from(shop);
        response.setNomenclatureCount(nomenclatureCount);
        return response;
    }
}
