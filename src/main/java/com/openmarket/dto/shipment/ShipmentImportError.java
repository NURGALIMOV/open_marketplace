package com.openmarket.dto.shipment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ShipmentImportError {
    private String shipmentNumber;
    private String error;
}
