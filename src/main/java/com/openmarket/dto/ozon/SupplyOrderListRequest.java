package com.openmarket.dto.ozon;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SupplyOrderListRequest {
    private SupplyOrderFilter filter;
    private SupplyOrderPaging paging;
}
