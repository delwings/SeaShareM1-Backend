package com.seashare.seasharem1.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba la API de punta a punta contra un servidor real y un MySQL real: login, formato de los
 * errores, rutas protegidas y CORS.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.jwt.secret=clave-de-pruebas-de-integracion-de-al-menos-32-caracteres")
class AuthApiIT extends AbstractMySqlIT {

  private static final String OWNER_LOGIN =
      "{\"document_number\":\"1010000002\",\"password\":\"SeaShare2026!\"}";

  private final HttpClient http = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int port;

  @Test
  void loginWithCorrectCredentialsReturnsAToken() throws Exception {
    HttpResponse<String> response = post("/api/v1/auth/login", OWNER_LOGIN);

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"access_token\""));
    assertTrue(response.body().contains("\"role\":\"OWNER\""));
  }

  @Test
  void wrongPasswordIsAProblemDetailWithCorrelationId() throws Exception {
    HttpResponse<String> response =
        post(
            "/api/v1/auth/login",
            "{\"document_number\":\"1010000002\",\"password\":\"incorrecta\"}");

    assertEquals(401, response.statusCode());
    assertTrue(response.body().contains("\"correlation_id\""));
    assertTrue(response.headers().firstValue("X-Correlation-Id").isPresent());
  }

  @Test
  void emptyBodyListsTheInvalidFields() throws Exception {
    HttpResponse<String> response = post("/api/v1/auth/login", "{}");

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("document_number"));
    assertTrue(response.body().contains("password"));
  }

  @Test
  void protectedRouteWithoutTokenIs401() throws Exception {
    HttpResponse<String> response = get("/api/v1/fleet/boats", null);

    assertEquals(401, response.statusCode());
  }

  @Test
  void unknownRouteWithTokenIs404() throws Exception {
    String body = post("/api/v1/auth/login", OWNER_LOGIN).body();
    String token = body.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

    HttpResponse<String> response = get("/api/v1/no-existe", token);

    assertEquals(404, response.statusCode());
  }

  @Test
  void corsAllowsTheFrontendOriginAndRejectsOthers() throws Exception {
    HttpResponse<String> allowed = preflight("http://localhost:5500");
    HttpResponse<String> rejected = preflight("http://otro-sitio.com");

    assertEquals(200, allowed.statusCode());
    assertEquals(
        "http://localhost:5500",
        allowed.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    assertEquals(403, rejected.statusCode());
    assertFalse(rejected.headers().firstValue("Access-Control-Allow-Origin").isPresent());
  }

  private HttpResponse<String> post(String path, String json) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path, String token) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> preflight(String origin) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
            .header("Origin", origin)
            .header("Access-Control-Request-Method", "POST")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
