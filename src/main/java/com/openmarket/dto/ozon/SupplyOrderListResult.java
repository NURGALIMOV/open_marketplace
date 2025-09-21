package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplyOrderListResult {
    @JsonProperty("supply_order_id")
    private List<String> supplyOrderId;
    @JsonProperty("last_supply_order_id")
    private Long lastSupplyOrderId;
}
