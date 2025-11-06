package com.notisblokk.audiencias.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalTime;

/**
 * Deserializador Jackson para LocalTime usando formato brasileiro (HH:mm ou HH:mm:ss).
 *
 * Centraliza toda lógica de conversão String → LocalTime através do DateUtil.
 */
public class BrazilianLocalTimeDeserializer extends JsonDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String timeStr = p.getText();

        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return DateUtil.parseTime(timeStr);
        } catch (IllegalArgumentException e) {
            throw new IOException("Erro ao converter horário: " + timeStr, e);
        }
    }
}
