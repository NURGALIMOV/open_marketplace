package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SupplyOrderBundleRequest {
    @JsonProperty("bundle_ids")
    private List<String> bundleIds;
    @JsonProperty("last_id")
    private String lastId;
    private int limit;
}
