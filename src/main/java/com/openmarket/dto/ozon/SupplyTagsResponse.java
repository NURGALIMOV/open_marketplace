package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplyTagsResponse {
    @JsonProperty("is_evsd_required")
    private boolean isEvsdRequired;
    @JsonProperty("is_marking_required")
    private boolean isMarkingRequired;
    @JsonProperty("is_marking_possible")
    private boolean isMarkingPossible;
    @JsonProperty("is_jewelry")
    private boolean isJewelry;
    @JsonProperty("is_traceable")
    private boolean isTraceable;
    @JsonProperty("is_ettn_required")
    private boolean isEttnRequired;
}
