package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarehouseResponse {
    @JsonProperty("warehouse_id")
    private Long warehouseId;
    private String address;
    private String name;
}
