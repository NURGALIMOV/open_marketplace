package com.openmarket.dto.shipment;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateShipmentItemRequest {
    @NotNull(message = "SKU is required")
    @Min(value = 0, message = "SKU must be positive")
    private Long sku;
    @NotBlank(message = "Article is required")
    @Size(max = 255, message = "Article must not exceed 255 characters")
    private String article;
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    private Integer quantity;
}
