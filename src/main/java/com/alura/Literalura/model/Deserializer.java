package com.alura.Literalura.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Arrays;

public class Deserializer extends JsonDeserializer<Idioma> {

    @Override
    public Idioma deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        String key = jsonParser.getText().toUpperCase().trim(); // Convertir el valor a mayúsculas y quitar espacios

        return Arrays.stream(Idioma.values())
                .filter(idioma -> idioma.getIdioma().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No enum constant com.alura.Literalura.model.Idioma." + key));
    }

}
