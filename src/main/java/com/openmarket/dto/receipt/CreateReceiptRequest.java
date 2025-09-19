package com.openmarket.dto.receipt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateReceiptRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    @NotBlank(message = "Request number is required")
    @Size(max = 255, message = "Request number must not exceed 255 characters")
    private String requestNumber;
    @NotNull
    private LocalDate receiptDate;
    @NotNull
    private UUID counterpartyContractId;
}

