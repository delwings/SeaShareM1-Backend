package com.seashare.seasharem1.unit.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.configuration.CorsConfig;
import com.seashare.seasharem1.configuration.CorsProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

  @Test
  void allowsOnlyTheConfiguredOriginsAndIgnoresBlanks() {
    CorsConfiguration configuration =
        CorsConfig.buildConfiguration(
            new CorsProperties(List.of("http://localhost:5500", " http://127.0.0.1:5500 ", "")));

    assertEquals("http://localhost:5500", configuration.checkOrigin("http://localhost:5500"));
    assertEquals("http://127.0.0.1:5500", configuration.checkOrigin("http://127.0.0.1:5500"));
    assertNull(configuration.checkOrigin("http://sitio-malicioso.test"));
  }

  @Test
  void allowsTheAuthorizationHeaderButNotCredentials() {
    CorsConfiguration configuration =
        CorsConfig.buildConfiguration(new CorsProperties(List.of("http://localhost:5500")));

    assertTrue(configuration.checkHeaders(List.of("Authorization", "Content-Type")) != null);
    assertTrue(configuration.checkHttpMethod(org.springframework.http.HttpMethod.POST) != null);
    assertFalse(Boolean.TRUE.equals(configuration.getAllowCredentials()));
  }

  @Test
  void withoutConfiguredOriginsNothingIsAllowed() {
    CorsConfiguration configuration = CorsConfig.buildConfiguration(new CorsProperties(null));

    assertNull(configuration.checkOrigin("http://localhost:5500"));
  }
}
