package com.openmarket.dto.ozon;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class CustomOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    private static final DateTimeFormatter YYYYMMDDTHHMMSS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DDMMYYYYTHHMMSS = DateTimeFormatter.ofPattern("dd.MM.yyyy'T'HH:mm:ssXXX");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final List<DateTimeFormatter> FORMATTERS = List.of(YYYYMMDDTHHMMSS, DDMMYYYYTHHMMSS, YYYYMMDD, DDMMYYYY);
    private static final String DD_MM_YYYY_REGEX = "\\d{2}\\.\\d{2}\\.\\d{4}";
    private static final String POSTFIX_FOR_DATE = "%sT00:00:00+03:00";

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateStr = p.getValueAsString();
        return StringUtils.hasText(dateStr) ? stringToOffsetDateTime(dateStr) : null;
    }

    private static OffsetDateTime stringToOffsetDateTime(String dateStr) {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return OffsetDateTime.parse(dateStr, formatter);
            } catch (Exception e) {
                log.warn("It was not possible to convert the line into the date: {}", dateStr, e);
            }
        }
        if (dateStr.matches(DD_MM_YYYY_REGEX)) {
            return OffsetDateTime.parse(POSTFIX_FOR_DATE.formatted(dateStr), DDMMYYYYTHHMMSS);
        }
        throw new IllegalArgumentException("It was not possible to make out the date: %s".formatted(dateStr));
    }
}
