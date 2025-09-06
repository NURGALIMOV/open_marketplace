package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductListResult {
    private List<ProductListItem> items;
    @JsonProperty("last_id")
    private String lastId;
    private int total;
}
