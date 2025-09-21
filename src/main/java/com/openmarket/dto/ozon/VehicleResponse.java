package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VehicleResponse {
    private VehicleValueResponse value;
    @JsonProperty("can_set")
    private boolean canSet;
    @JsonProperty("can_not_set_reasons")
    private List<String> canNotSetReasons;
    @JsonProperty("is_required")
    private boolean isRequired;
}
