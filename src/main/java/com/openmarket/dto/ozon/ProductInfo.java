package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Setter
@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductInfo {
    @JsonProperty("offer_id")
    private String offerId;
    private Long sku;
    @JsonProperty("volume_weight")
    private BigDecimal volumeWeight;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
