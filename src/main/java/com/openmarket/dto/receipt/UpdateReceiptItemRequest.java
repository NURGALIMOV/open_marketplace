package com.openmarket.dto.receipt;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateReceiptItemRequest {
    @NotNull
    @Min(value = 0, message = "SKU must be non-negative")
    private Long sku;
    @NotBlank
    @Size(max = 255, message = "Article must not exceed 255 characters")
    private String article;
    @NotNull
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantity;
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost must be non-negative")
    private BigDecimal cost;
}

