package com.seashare.seasharem1.configuration;

import com.seashare.seasharem1.application.port.out.ImageStoragePort;
import com.seashare.seasharem1.infrastructure.adapter.out.storage.CloudinaryImageStorage;
import com.seashare.seasharem1.infrastructure.adapter.out.storage.CloudinaryProperties;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Registra el almacenamiento de imágenes (Cloudinary) como implementación del puerto. */
@Configuration
public class StorageConfig {

  @Bean
  public ImageStoragePort imageStoragePort(CloudinaryProperties properties, Clock clock) {
    return new CloudinaryImageStorage(properties, RestClient.create(), clock);
  }
}
