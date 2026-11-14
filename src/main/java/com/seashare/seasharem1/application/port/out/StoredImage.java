package com.seashare.seasharem1.application.port.out;

/**
 * Imagen ya guardada.
 *
 * @param url dirección pública (https) para mostrarla
 * @param publicId identificador en el servicio de almacenamiento
 */
public record StoredImage(String url, String publicId) {}
