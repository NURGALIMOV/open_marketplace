package com.openmarket.dto.ozon;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeslotValueResponse {
    private TimeslotRangeResponse timeslot;
    @JsonProperty("timezone_info")
    private TimezoneInfoResponse timezoneInfo;
}