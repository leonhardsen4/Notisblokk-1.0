package com.notisblokk.audiencias.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Deserializador Jackson para LocalDate usando formato brasileiro (dd/MM/yyyy).
 *
 * Centraliza toda lógica de conversão String → LocalDate através do DateUtil.
 */
public class BrazilianLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateStr = p.getText();

        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            return DateUtil.parseDate(dateStr);
        } catch (IllegalArgumentException e) {
            throw new IOException("Erro ao converter data: " + dateStr, e);
        }
    }
}
