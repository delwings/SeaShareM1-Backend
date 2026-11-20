package com.seashare.seasharem1.configuration;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS: solo los orígenes de CORS_ALLOWED_ORIGINS pueden llamar a la API desde el navegador. No se
 * usan cookies (el token viaja en la cabecera Authorization), por eso no se permiten credenciales.
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", buildConfiguration(properties));
    return source;
  }

  public static CorsConfiguration buildConfiguration(CorsProperties properties) {
    List<String> origins =
        properties.allowedOrigins() == null
            ? List.of()
            : properties.allowedOrigins().stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
    configuration.setExposedHeaders(List.of("X-Correlation-Id"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);
    return configuration;
  }
}
