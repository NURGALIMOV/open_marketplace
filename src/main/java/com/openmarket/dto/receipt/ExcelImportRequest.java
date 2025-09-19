package com.openmarket.dto.receipt;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ExcelImportRequest {
    /** column mapping (e.g., "sku" -> "A") */
    private Map<String, String> mapping;
    private boolean hasHeader = true;
    private boolean strict = true;
}

