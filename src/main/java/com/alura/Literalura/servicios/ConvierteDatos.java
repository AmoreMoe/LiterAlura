package com.alura.Literalura.servicios;

import com.alura.Literalura.model.Idioma;
import com.alura.Literalura.model.Deserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ConvierteDatos implements IConvierteDatos {
    private ObjectMapper objectMapper;

    public ConvierteDatos() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Idioma.class, new Deserializer());
        objectMapper.registerModule(module);
    }

    @Override
    public <T> T obtenerDatos(String json, Class<T> clase) {
        if (json == null || json.trim().isEmpty()) {
            throw new RuntimeException("La respuesta JSON está vacía");
        }
        try {
            return objectMapper.readValue(json, clase);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al procesar JSON", e);
        }
    }
}
