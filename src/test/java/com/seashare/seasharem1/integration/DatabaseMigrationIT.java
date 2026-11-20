package com.seashare.seasharem1.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sobre un MySQL 8 real comprueba que Flyway aplica las 9 migraciones y que Hibernate acepta las
 * entidades (ddl-auto: validate): si una entidad no coincide con una tabla, la aplicación ni
 * arranca y esta prueba falla.
 */
@SpringBootTest(
    properties = "app.jwt.secret=clave-de-pruebas-de-integracion-de-al-menos-32-caracteres")
class DatabaseMigrationIT extends AbstractMySqlIT {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void flywayAppliedAllSevenMigrations() {
    Integer applied =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

    assertEquals(11, applied);
  }

  @Test
  void allTablesExist() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
            String.class);

    assertEquals(
        List.of(
            "flyway_schema_history",
            "maintenance_event_outbox",
            "notification",
            "port",
            "processed_event",
            "users",
            "vessel",
            "vessel_status_history"),
        tables.stream().map(String::toLowerCase).sorted().toList());
  }

  @Test
  void seedHasOneAdminAndFiveOwners() {
    Integer admins =
        jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class);
    Integer owners =
        jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'OWNER'", Integer.class);

    assertEquals(1, admins);
    assertEquals(5, owners);
  }
}
