package com.notisblokk.audiencias.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;

/**
 * Serializador Jackson para LocalTime usando formato brasileiro (HH:mm:ss).
 *
 * Centraliza toda lógica de conversão LocalTime → String através do DateUtil.
 */
public class BrazilianLocalTimeSerializer extends JsonSerializer<LocalTime> {

    @Override
    public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(DateUtil.formatTime(value));
        }
    }
}
