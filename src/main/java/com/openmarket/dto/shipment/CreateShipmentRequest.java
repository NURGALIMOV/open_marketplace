package com.openmarket.dto.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateShipmentRequest {
    @NotBlank(message = "Shipment number is required")
    @Size(max = 255, message = "Shipment number must not exceed 255 characters")
    private String shipmentNumber;
    @NotNull(message = "Shipment date is required")
    private LocalDate shipmentDate;
}
