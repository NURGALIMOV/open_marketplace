package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimezoneInfoResponse {
    private String offset;
    @JsonProperty("iana_name")
    private String ianaName;
}
