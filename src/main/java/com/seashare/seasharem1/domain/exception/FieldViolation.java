package com.seashare.seasharem1.domain.exception;

/**
 * Un campo con error y su mensaje en español. Se devuelve al cliente en la lista errors del error
 * 400 (y del 409 por matrícula duplicada).
 *
 * @param field nombre del campo tal como lo usa la API, en snake_case
 * @param message mensaje para mostrar debajo del campo
 */
public record FieldViolation(String field, String message) {}
