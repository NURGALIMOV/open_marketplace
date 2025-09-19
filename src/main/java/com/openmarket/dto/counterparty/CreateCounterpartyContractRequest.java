package com.openmarket.dto.counterparty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateCounterpartyContractRequest {
    @NotBlank(message = "Counterparty is required")
    @Size(max = 255, message = "Counterparty must not exceed 255 characters")
    private String counterparty;
    @NotBlank(message = "Contract is required")
    @Size(max = 255, message = "Contract must not exceed 255 characters")
    private String contract;
    @NotNull
    private LocalDate contractDate;
}

