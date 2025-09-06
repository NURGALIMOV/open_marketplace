package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductInfoResponse {
    private ProductInfoResult result;
}
