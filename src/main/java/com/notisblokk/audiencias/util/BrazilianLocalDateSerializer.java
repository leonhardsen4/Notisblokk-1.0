package com.notisblokk.audiencias.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Serializador Jackson para LocalDate usando formato brasileiro (dd/MM/yyyy).
 *
 * Centraliza toda lógica de conversão LocalDate → String através do DateUtil.
 */
public class BrazilianLocalDateSerializer extends JsonSerializer<LocalDate> {

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(DateUtil.formatDate(value));
        }
    }
}
