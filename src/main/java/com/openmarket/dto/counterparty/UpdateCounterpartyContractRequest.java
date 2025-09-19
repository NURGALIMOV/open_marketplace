package com.openmarket.dto.counterparty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateCounterpartyContractRequest {
    @Size(max = 255, message = "Counterparty must not exceed 255 characters")
    private String counterparty;
    @Size(max = 255, message = "Contract must not exceed 255 characters")
    private String contract;
    @NotNull
    private LocalDate contractDate;
}

