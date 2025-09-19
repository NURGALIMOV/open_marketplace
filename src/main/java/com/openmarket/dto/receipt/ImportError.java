package com.openmarket.dto.receipt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ImportError {
    private int row;
    private String column;
    private String error;
}
