package com.seashare.seasharem1.application.port.out;

/** Puerto de salida para guardar imágenes (hoy lo implementa Cloudinary). */
public interface ImageStoragePort {

  /**
   * Guarda una imagen y devuelve su dirección pública.
   *
   * @param content bytes de la imagen
   * @param contentType tipo de contenido, por ejemplo image/png o image/jpeg
   * @param folder carpeta lógica donde se guarda, por ejemplo seashare/vessels
   * @throws com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException si el
   *     servicio no responde o no está configurado
   */
  StoredImage store(byte[] content, String contentType, String folder);
}
