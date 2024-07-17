package com.alura.Literalura.servicios;

public interface IConvierteDatos {
    <T> T obtenerDatos(String json, Class<T> clase);
}
