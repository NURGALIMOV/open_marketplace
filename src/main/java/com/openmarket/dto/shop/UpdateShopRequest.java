package com.openmarket.dto.shop;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShopRequest {
    @Size(max = 255, message = "Shop name must not exceed 255 characters")
    private String name;
    /** Client-Id for Ozon */
    @Size(max = 255, message = "External ID must not exceed 255 characters")
    private String externalId;
    /** API-Key for Ozon (will be encrypted) */
    @Size(max = 1000, message = "Token must not exceed 1000 characters")
    private String token;
}
