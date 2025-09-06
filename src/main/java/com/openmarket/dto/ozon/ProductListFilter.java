package com.openmarket.dto.ozon;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ProductListFilter {
    private String visibility;

    public ProductListFilter(String visibility) {
        this.visibility = visibility;
    }
}