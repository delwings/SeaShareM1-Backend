package com.seashare.seasharem1.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Prueba de humo para confirmar que Maven ejecuta las pruebas de integración (*IT) con Failsafe. Se
 * reemplaza por pruebas con Testcontainers en T020.
 */
class BuildSmokeIT {

  @Test
  void failsafeRunsIntegrationTests() {
    assertTrue(true);
  }
}
