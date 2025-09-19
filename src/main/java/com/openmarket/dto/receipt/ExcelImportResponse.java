package com.openmarket.dto.receipt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class ExcelImportResponse {
    private int rowsProcessed;
    private int rowsCreated;
    private BigDecimal totalCostDelta;
    private List<ImportError> errors;
}

