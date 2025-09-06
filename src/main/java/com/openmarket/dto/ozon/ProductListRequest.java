package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductListRequest {
    private ProductListFilter filter;
    private int limit;
    @JsonProperty("last_id")
    private String lastId;
}
