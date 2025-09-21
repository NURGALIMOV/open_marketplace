package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SupplyOrderPaging {
    private int limit;
    @JsonProperty("from_supply_order_id")
    private Long fromSupplyOrderId;
}
