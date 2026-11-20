package com.seashare.seasharem1.infrastructure.adapter.out.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales de Cloudinary, leídas de app.cloudinary.* (variables CLOUDINARY_CLOUD_NAME,
 * CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET). Pueden estar vacías: la aplicación arranca igual y
 * solo falla al intentar subir una imagen.
 */
@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret) {

  public boolean isConfigured() {
    return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }
}
