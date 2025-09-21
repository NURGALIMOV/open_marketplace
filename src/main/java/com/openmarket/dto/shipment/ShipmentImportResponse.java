package com.openmarket.dto.shipment;

import com.openmarket.dto.receipt.ImportError;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ShipmentImportResponse {
    private UUID shipmentId;
    private int rowsProcessed;
    private int rowsCreated;
    private Long totalQuantityDelta;
    private List<ImportError> errors;
}
