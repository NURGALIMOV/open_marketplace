package com.openmarket.dto.shipment;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateShipmentRequest {
    @Size(max = 255, message = "Shipment number must not exceed 255 characters")
    private String shipmentNumber;
    private LocalDate shipmentDate;
}
