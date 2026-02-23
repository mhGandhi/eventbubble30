package com.lennadi.eventbubble30.features;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;

@Component
public class DTOLevelConverter implements Converter<String, DTOLevel> {
    @Override
    public DTOLevel convert(String source) {
        if (source == null || source.isBlank()) return DTOLevel.FULL;

        try {
            return DTOLevel.valueOf(source.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid DTOLevel \"" + source + "\" (allowed: "+ Arrays.toString(DTOLevel.values()) +")"
            );
        }
    }
}