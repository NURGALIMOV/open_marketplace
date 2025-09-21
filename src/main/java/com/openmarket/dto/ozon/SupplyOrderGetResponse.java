package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplyOrderGetResponse {
    private List<SupplyOrderResponse> orders;
    private List<WarehouseResponse> warehouses;
}

