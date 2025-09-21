package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class SupplyOrderResponse {
    @JsonProperty("supply_order_id")
    private Long supplyOrderId;
    @JsonProperty("supply_order_number")
    private String supplyOrderNumber;
    @JsonProperty("creation_date")
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime creationDate;
    private String state;
    @JsonProperty("data_filling_deadline_utc")
    private String dataFillingDeadlineUtc;
    @JsonProperty("dropoff_warehouse_id")
    private Long dropoffWarehouseId;
    private TimeslotResponse timeslot;
    private VehicleResponse vehicle;
    private List<SupplyResponse> supplies;
    @JsonProperty("can_cancel")
    private boolean canCancel;
    @JsonProperty("is_econom")
    private boolean isEconom;
    @JsonProperty("is_virtual")
    private boolean isVirtual;
    @JsonProperty("is_super_fbo")
    private boolean isSuperFbo;
    @JsonProperty("product_super_fbo")
    private boolean productSuperFbo;
}
