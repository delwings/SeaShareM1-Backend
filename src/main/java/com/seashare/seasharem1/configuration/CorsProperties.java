package com.seashare.seasharem1.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Orígenes del frontend autorizados a llamar al backend, leídos de app.cors.allowed-origins
 * (variable CORS_ALLOWED_ORIGINS, separados por coma).
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {}
