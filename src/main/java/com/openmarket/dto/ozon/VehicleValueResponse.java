package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleValueResponse {
    @JsonProperty("vehicle_model")
    private String vehicleModel;
    @JsonProperty("vehicle_number")
    private String vehicleNumber;
    @JsonProperty("driver_name")
    private String driverName;
    @JsonProperty("driver_phone")
    private String driverPhone;
}
