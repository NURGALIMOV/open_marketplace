package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplyResponse {
    @JsonProperty("supply_id")
    private Long supplyId;
    @JsonProperty("bundle_id")
    private String bundleId;
    @JsonProperty("storage_warehouse_id")
    private Long storageWarehouseId;
    @JsonProperty("supply_state")
    private String supplyState;
    @JsonProperty("supply_tags")
    private SupplyTagsResponse supplyTags;
}
