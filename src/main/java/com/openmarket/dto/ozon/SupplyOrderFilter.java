package com.openmarket.dto.ozon;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SupplyOrderFilter {
    private String[] states;
}
