package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplyOrderBundleResult {
    private List<SupplyOrderBundleItem> items;
    @JsonProperty("total_count")
    private int totalCount;
    @JsonProperty("last_id")
    private String lastId;
    @JsonProperty("has_next")
    private boolean hasNext;
}
