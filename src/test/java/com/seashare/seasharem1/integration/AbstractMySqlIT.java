package com.seashare.seasharem1.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base de las pruebas de integración: levanta un MySQL 8 real en Docker (la misma versión que en
 * producción) y apunta la aplicación a él. El contenedor se crea una sola vez y lo comparten todas
 * las pruebas. Si Docker no está disponible, las pruebas se omiten en lugar de fallar.
 */
abstract class AbstractMySqlIT {

  private static MySQLContainer mysql;

  @BeforeAll
  static void requireDocker() {
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker no está disponible: se omiten las pruebas de integración");
  }

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    MySQLContainer container = startContainer();
    registry.add("spring.datasource.url", container::getJdbcUrl);
    registry.add("spring.datasource.username", container::getUsername);
    registry.add("spring.datasource.password", container::getPassword);
  }

  private static synchronized MySQLContainer startContainer() {
    if (mysql == null) {
      mysql = new MySQLContainer("mysql:8.0.43");
      mysql.start();
    }
    return mysql;
  }
}
