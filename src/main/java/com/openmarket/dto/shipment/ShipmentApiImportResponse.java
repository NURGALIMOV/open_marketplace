package com.openmarket.dto.shipment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ShipmentApiImportResponse {
    private int shipmentsAdded;
    private int shipmentsSkipped;
    private List<ShipmentImportError> errors;
}
