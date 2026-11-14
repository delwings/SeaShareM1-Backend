package com.seashare.seasharem1.infrastructure.adapter.out.storage;

import com.seashare.seasharem1.application.port.out.ImageStoragePort;
import com.seashare.seasharem1.application.port.out.StoredImage;
import com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Guarda imágenes en Cloudinary con una subida firmada a su API REST. No usa la librería de
 * Cloudinary: la firma es un SHA-1 y la subida un formulario multipart, así no se agrega una
 * dependencia más. Cualquier fallo se reporta como ExternalServiceUnavailableException (error 502).
 */
public class CloudinaryImageStorage implements ImageStoragePort {

  private static final String UPLOAD_URL = "https://api.cloudinary.com/v1_1/%s/image/upload";
  private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
      new ParameterizedTypeReference<>() {};

  private final CloudinaryProperties properties;
  private final RestClient restClient;
  private final Clock clock;

  public CloudinaryImageStorage(
      CloudinaryProperties properties, RestClient restClient, Clock clock) {
    this.properties = properties;
    this.restClient = restClient;
    this.clock = clock;
  }

  @Override
  public StoredImage store(byte[] content, String contentType, String folder) {
    if (!properties.isConfigured()) {
      throw new ExternalServiceUnavailableException(
          "Cloudinary no está configurado (faltan las variables CLOUDINARY_*).", null);
    }
    long timestamp = clock.instant().getEpochSecond();
    String signature = sign("folder=" + folder + "&timestamp=" + timestamp, properties.apiSecret());

    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", namedResource(content, contentType));
    form.add("api_key", properties.apiKey());
    form.add("timestamp", String.valueOf(timestamp));
    form.add("folder", folder);
    form.add("signature", signature);

    try {
      Map<String, Object> response =
          restClient
              .post()
              .uri(UPLOAD_URL.formatted(properties.cloudName()))
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(form)
              .retrieve()
              .body(MAP_TYPE);
      if (response == null
          || response.get("secure_url") == null
          || response.get("public_id") == null) {
        throw new ExternalServiceUnavailableException(
            "Cloudinary respondió sin la dirección de la imagen.", null);
      }
      return new StoredImage(
          String.valueOf(response.get("secure_url")), String.valueOf(response.get("public_id")));
    } catch (RestClientException exception) {
      throw new ExternalServiceUnavailableException(
          "No se pudo subir la imagen a Cloudinary.", exception);
    }
  }

  /** Firma de Cloudinary: SHA-1 de los parámetros ordenados seguidos del secreto. */
  public static String sign(String parameters, String apiSecret) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] hash = digest.digest((parameters + apiSecret).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-1 no está disponible", exception);
    }
  }

  private static ByteArrayResource namedResource(byte[] content, String contentType) {
    String extension = "image/png".equals(contentType) ? "png" : "jpg";
    return new ByteArrayResource(content) {
      @Override
      public String getFilename() {
        return "upload." + extension;
      }
    };
  }
}
