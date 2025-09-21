package com.openmarket.dto.shipment;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class ShipmentImportRequest {
    /** column mapping (e.g., "sku" -> "A") */
    private Map<String, String> mapping;
    private boolean hasHeader = true;
    private boolean strict = true;
    private LocalDate shipmentDate;
    /** Starting row for parsing (1-based index) */
    private Integer startRow = 1;
}
