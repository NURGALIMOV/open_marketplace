package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplyOrderInfo {
    private String supplyOrderNumber;
    private OffsetDateTime creationDate;
    private Map<String, List<String>> bundleIdsBySupplyOrderNumber;
    private Long supplyOrderId;
}
