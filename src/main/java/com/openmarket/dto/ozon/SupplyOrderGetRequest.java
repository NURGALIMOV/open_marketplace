package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SupplyOrderGetRequest {
    @JsonProperty("order_ids")
    private List<String> orderIds;
}
