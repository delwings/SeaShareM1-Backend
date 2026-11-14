package com.seashare.seasharem1.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.seashare.seasharem1.application.port.out.ImageStoragePort;
import com.seashare.seasharem1.application.port.out.StoredImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Registro de embarcaciones de punta a punta con MySQL real. Cloudinary se reemplaza por un doble:
 * aquí se prueba nuestra lógica, no el servicio externo.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.jwt.secret=clave-de-pruebas-de-integracion-de-al-menos-32-caracteres")
class BoatRegistrationApiIT extends AbstractMySqlIT {

  private static final byte[] PNG = {
    (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
  };
  private static final String BOUNDARY = "----seashare-test-boundary";

  private final HttpClient http = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int port;

  @Autowired private JdbcTemplate jdbc;

  @MockitoBean private ImageStoragePort images;

  @BeforeEach
  void cleanAndStubStorage() {
    jdbc.update("DELETE FROM notification");
    jdbc.update("DELETE FROM vessel_status_history");
    jdbc.update("DELETE FROM processed_event");
    jdbc.update("DELETE FROM vessel");
    jdbc.update("DELETE FROM port");
    when(images.store(any(), anyString(), anyString()))
        .thenReturn(new StoredImage("https://img.test/boat.png", "seashare/boats/boat"));
  }

  @Test
  void fullFlowRegistersTheBoatAndListsItAsNew() throws Exception {
    String owner = login("1010000002");

    HttpResponse<String> draft = postJson("/api/v1/fleet/boats/draft", draftJson("MAR-1"), owner);
    assertEquals(201, draft.statusCode());
    String boatId = field(draft.body(), "boat_id");

    HttpResponse<String> done = complete(boatId, owner, validForm(), PNG);
    assertEquals(200, done.statusCode(), done.body());
    assertTrue(done.body().contains("\"status\":\"AVAILABLE\""));
    assertTrue(done.body().contains("https://img.test/boat.png"));

    HttpResponse<String> list = get("/api/v1/fleet/boats", owner);
    assertEquals(200, list.statusCode());
    assertTrue(list.body().contains("\"is_new\":true"));
    assertEquals(404, get("/api/v1/fleet/boats/draft", owner).statusCode());
  }

  @Test
  void sameRegistrationForAnotherOwnerIs409() throws Exception {
    postJson("/api/v1/fleet/boats/draft", draftJson("UNO"), login("1010000002"));

    HttpResponse<String> clash =
        postJson("/api/v1/fleet/boats/draft", draftJson("DOS"), login("1010000003"));

    assertEquals(409, clash.statusCode());
    assertTrue(clash.body().contains("legal_registration"));
  }

  @Test
  void secondDraftForTheSameOwnerIs409() throws Exception {
    String owner = login("1010000002");
    postJson("/api/v1/fleet/boats/draft", draftJson("UNO"), owner);

    HttpResponse<String> second =
        postJson(
            "/api/v1/fleet/boats/draft",
            draftJson("DOS").replace("CP-01-2026-A", "CP-02-2026-B"),
            owner);

    assertEquals(409, second.statusCode());
  }

  @Test
  void availabilityReflectsExistingRegistrations() throws Exception {
    String owner = login("1010000002");
    assertTrue(
        get("/api/v1/fleet/boats/legal-registration-availability?value=CP-01-2026-A", owner)
            .body()
            .contains("true"));
    postJson("/api/v1/fleet/boats/draft", draftJson("UNO"), owner);

    assertTrue(
        get("/api/v1/fleet/boats/legal-registration-availability?value=CP-01-2026-A", owner)
            .body()
            .contains("false"));
  }

  @Test
  void adminCannotUseTheOwnerRoutes() throws Exception {
    assertEquals(403, get("/api/v1/fleet/boats", login("1010000001")).statusCode());
  }

  @Test
  void invalidDraftIs400WithFieldErrors() throws Exception {
    HttpResponse<String> response =
        postJson("/api/v1/fleet/boats/draft", "{}", login("1010000002"));

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("\"errors\""));
    assertTrue(response.body().contains("legal_registration"));
  }

  @Test
  void deletingTheDraftRemovesIt() throws Exception {
    String owner = login("1010000002");
    postJson("/api/v1/fleet/boats/draft", draftJson("UNO"), owner);

    assertEquals(204, send("DELETE", "/api/v1/fleet/boats/draft", owner).statusCode());
    assertEquals(404, get("/api/v1/fleet/boats/draft", owner).statusCode());
  }

  @Test
  void invalidPhotoFormatIs400() throws Exception {
    String owner = login("1010000002");
    String boatId =
        field(postJson("/api/v1/fleet/boats/draft", draftJson("UNO"), owner).body(), "boat_id");

    HttpResponse<String> response =
        complete(boatId, owner, validForm(), "no soy imagen".getBytes());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("Formato de imagen inválido"));
  }

  @Test
  void twoBoatsWithTheSamePortNameShareOnePort() throws Exception {
    String first = login("1010000002");
    String second = login("1010000003");
    String idOne =
        field(postJson("/api/v1/fleet/boats/draft", draftJson("UNO"), first).body(), "boat_id");
    String idTwo =
        field(
            postJson(
                    "/api/v1/fleet/boats/draft",
                    draftJson("DOS").replace("CP-01-2026-A", "CP-02-2026-B"),
                    second)
                .body(),
            "boat_id");

    assertEquals(200, complete(idOne, first, validForm(), PNG).statusCode());
    assertEquals(200, complete(idTwo, second, validForm(), PNG).statusCode());

    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM port", Integer.class));
  }

  @Test
  void detailShowsTheRegisteredBoatToItsOwner() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");

    HttpResponse<String> response = get("/api/v1/fleet/boats/" + boatId, owner);

    assertEquals(200, response.statusCode(), response.body());
    assertTrue(response.body().contains("\"name\":\"UNO\""));
    assertTrue(response.body().contains("\"base_rate\":\"250000.00\""));
    assertTrue(response.body().contains("Marina Central"));
    assertTrue(response.body().contains("https://img.test/boat.png"));
  }

  @Test
  void detailOfAnotherOwnersBoatIs404() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");

    assertEquals(404, get("/api/v1/fleet/boats/" + boatId, login("1010000003")).statusCode());
  }

  @Test
  void detailOfADraftOrAnUnknownIdIs404() throws Exception {
    String owner = login("1010000002");
    String draftId =
        field(postJson("/api/v1/fleet/boats/draft", draftJson("UNO"), owner).body(), "boat_id");

    assertEquals(404, get("/api/v1/fleet/boats/" + draftId, owner).statusCode());
    assertEquals(
        404, get("/api/v1/fleet/boats/00000000-0000-0000-0000-000000000000", owner).statusCode());
  }

  @Test
  void adminCannotUseTheOwnerDetail() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");

    assertEquals(403, get("/api/v1/fleet/boats/" + boatId, login("1010000001")).statusCode());
  }

  @Test
  void deletingABoatHidesItKeepsTheRecordAndFreesTheRegistration() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");

    assertEquals(204, send("DELETE", "/api/v1/fleet/boats/" + boatId, owner).statusCode());

    assertEquals(404, get("/api/v1/fleet/boats/" + boatId, owner).statusCode());
    assertFalse(get("/api/v1/fleet/boats", owner).body().contains(boatId));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM vessel WHERE id = ? AND deleted = TRUE AND deleted_at IS NOT NULL"
                + " AND deleted_by IS NOT NULL",
            Integer.class,
            boatId));
    // La matrícula quedó libre: se puede registrar otra embarcación con la misma.
    assertTrue(
        get("/api/v1/fleet/boats/legal-registration-availability?value=CP-01-2026-A", owner)
            .body()
            .contains("true"));
    registerBoat(owner, "DOS");
  }

  @Test
  void deletingTwiceIs404() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    send("DELETE", "/api/v1/fleet/boats/" + boatId, owner);

    assertEquals(404, send("DELETE", "/api/v1/fleet/boats/" + boatId, owner).statusCode());
  }

  @Test
  void deletingAnotherOwnersBoatIs404AndDoesNothing() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");

    assertEquals(
        404, send("DELETE", "/api/v1/fleet/boats/" + boatId, login("1010000003")).statusCode());
    assertEquals(200, get("/api/v1/fleet/boats/" + boatId, login("1010000002")).statusCode());
  }

  @Test
  void aReservedBoatIs409WithItsCurrentStatus() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    jdbc.update("UPDATE vessel SET status = 'RESERVED' WHERE id = ?", boatId);

    HttpResponse<String> response = send("DELETE", "/api/v1/fleet/boats/" + boatId, owner);

    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("\"current_status\":\"RESERVED\""));
    assertEquals(200, get("/api/v1/fleet/boats/" + boatId, owner).statusCode());
  }

  @Test
  void aBoatInMaintenanceCanBeDeleted() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    jdbc.update("UPDATE vessel SET status = 'MAINTENANCE' WHERE id = ?", boatId);

    assertEquals(204, send("DELETE", "/api/v1/fleet/boats/" + boatId, owner).statusCode());
  }

  @Test
  void adminDeletesAnyBoatAndTheOwnerIsNotified() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    String admin = login("1010000001");

    assertEquals(204, deleteAsAdmin(boatId, admin, "{\"reason\":\"Fotos falsas\"}").statusCode());

    assertEquals(404, get("/api/v1/fleet/boats/" + boatId, owner).statusCode());
    assertEquals(404, get("/api/v1/admin/fleet/boats/" + boatId, admin).statusCode());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM notification WHERE vessel_id = ? AND type = 'VESSEL_DELETED' "
                + "AND message LIKE 'Tu embarcación UNO (%) fue eliminada por un administrador el %. Motivo: Fotos falsas'",
            Integer.class, boatId));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM vessel v JOIN users u ON u.id = v.deleted_by "
                + "WHERE v.id = ? AND u.document_number = 1010000001",
            Integer.class,
            boatId));
  }

  @Test
  void adminDeleteTwiceIs404AndDoesNotNotifyAgain() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");
    String admin = login("1010000001");
    deleteAsAdmin(boatId, admin, "{\"reason\":\"Fotos falsas\"}");

    assertEquals(404, deleteAsAdmin(boatId, admin, "{\"reason\":\"Fotos falsas\"}").statusCode());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM notification WHERE vessel_id = ?", Integer.class, boatId));
  }

  @Test
  void adminDeleteOfAReservedBoatIs409AndDoesNotNotify() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");
    jdbc.update("UPDATE vessel SET status = 'RESERVED' WHERE id = ?", boatId);

    HttpResponse<String> response =
        deleteAsAdmin(boatId, login("1010000001"), "{\"reason\":\"Fotos falsas\"}");

    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("\"current_status\":\"RESERVED\""));
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
  }

  @Test
  void ownerDeletingDoesNotCreateANotificationAndCannotUseTheAdminDelete() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");

    assertEquals(403, deleteAsAdmin(boatId, owner, "{\"reason\":\"Fotos falsas\"}").statusCode());
    assertEquals(204, send("DELETE", "/api/v1/fleet/boats/" + boatId, owner).statusCode());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
  }

  @Test
  void adminDeleteWithoutAReasonIs400AndChangesNothing() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");
    String admin = login("1010000001");

    HttpResponse<String> response = deleteAsAdmin(boatId, admin, "{}");

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("\"field\":\"reason\""));
    assertEquals(
        400, deleteAsAdmin(boatId, admin, "{\"reason\":\"" + "x".repeat(301) + "\"}").statusCode());
    assertEquals(200, get("/api/v1/admin/fleet/boats/" + boatId, admin).statusCode());
  }

  @Test
  void theOwnerSeesTheReasonInTheirNotificationsAndCanMarkItRead() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    deleteAsAdmin(boatId, login("1010000001"), "{\"reason\":\"Fotos falsas\"}");

    HttpResponse<String> list = get("/api/v1/notifications", owner);

    assertEquals(200, list.statusCode());
    assertTrue(list.body().contains("\"unread_count\":1"));
    assertTrue(list.body().contains("Motivo: Fotos falsas"));
    assertTrue(list.body().contains("\"type\":\"VESSEL_DELETED\""));
    String id = jdbc.queryForObject("SELECT id FROM notification", String.class);
    assertEquals(204, send("PATCH", "/api/v1/notifications/" + id + "/read", owner).statusCode());
    assertTrue(get("/api/v1/notifications", owner).body().contains("\"unread_count\":0"));
    assertEquals(204, send("PATCH", "/api/v1/notifications/read-all", owner).statusCode());
  }

  @Test
  void notificationsArePrivateToEachUser() throws Exception {
    String owner = login("1010000002");
    deleteAsAdmin(registerBoat(owner, "UNO"), login("1010000001"), "{\"reason\":\"Motivo\"}");
    String id = jdbc.queryForObject("SELECT id FROM notification", String.class);

    String other = login("1010000003");
    assertTrue(get("/api/v1/notifications", other).body().contains("\"unread_count\":0"));
    assertEquals(404, send("PATCH", "/api/v1/notifications/" + id + "/read", other).statusCode());
    assertEquals(200, get("/api/v1/notifications", login("1010000001")).statusCode());
    assertEquals(
        401,
        http.send(
                HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/notifications"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString())
            .statusCode());
  }

  @Test
  void baseRatesAreProvidedToModule3WithoutAuthentication() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "TARIFA");
    String unknown = UUID.randomUUID().toString();

    HttpResponse<String> response =
        postJson(
            "/api/v1/boats/base-rates",
            "{\"boat_ids\":[\"" + boatId + "\",\"" + boatId + "\",\"" + unknown + "\"]}",
            null);

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"boat_id\":\"" + boatId + "\""));
    assertTrue(response.body().matches(".*\"base_rate\":\"\\d+\\.00\".*"));
    assertTrue(response.body().contains("\"not_found\":[\"" + unknown + "\"]"));
    assertEquals(1, response.body().split("\"boat_id\"", -1).length - 1);
  }

  @Test
  void baseRatesRejectMalformedBatchesAndNeverExposeDrafts() throws Exception {
    assertEquals(400, postJson("/api/v1/boats/base-rates", "{\"boat_ids\":[]}", null).statusCode());
    assertEquals(400, postJson("/api/v1/boats/base-rates", "{}", null).statusCode());
    assertEquals(
        400, postJson("/api/v1/boats/base-rates", "{\"boat_ids\":[\"abc\"]}", null).statusCode());
    assertEquals(400, postJson("/api/v1/boats/base-rates", "no es json", null).statusCode());
    String tooMany =
        java.util.stream.IntStream.range(0, 101)
            .mapToObj(i -> "\"" + UUID.randomUUID() + "\"")
            .collect(java.util.stream.Collectors.joining(","));
    assertEquals(
        400,
        postJson("/api/v1/boats/base-rates", "{\"boat_ids\":[" + tooMany + "]}", null)
            .statusCode());
  }

  @Test
  void module2ReadsBoatInfoAndOperationalStatusWithoutAuthentication() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "RESERVAS");

    HttpResponse<String> info = getPublic("/api/v1/vessels/" + boatId);
    assertEquals(200, info.statusCode());
    assertTrue(info.body().contains("\"vessel_id\":\"" + boatId + "\""));
    assertTrue(info.body().contains("\"registration_number\""));
    assertTrue(info.body().contains("\"timezone\":\"America/Bogota\""));
    assertTrue(info.body().contains("\"owner_id\""));
    assertTrue(info.body().contains("\"operational_status\":\"AVAILABLE\""));
    assertTrue(info.body().contains("\"photo_url\""));
    assertTrue(!info.body().contains("base_rate"));

    HttpResponse<String> status = getPublic("/api/v1/vessels/" + boatId + "/operational-status");
    assertEquals(200, status.statusCode());
    assertTrue(status.body().contains("\"operational_status\":\"AVAILABLE\""));
    assertTrue(status.body().matches(".*\"queried_at\":\"[^\"]+-05:00\".*"));
    assertEquals(3, status.body().split("\":", -1).length - 1);

    assertEquals(
        200,
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/start", owner).statusCode());
    assertTrue(
        getPublic("/api/v1/vessels/" + boatId + "/operational-status")
            .body()
            .contains("\"operational_status\":\"MAINTENANCE\""));
  }

  @Test
  void module2GetsNotFoundForUnknownBoatsAndBadRequestForInvalidIds() throws Exception {
    String unknown = UUID.randomUUID().toString();
    assertEquals(404, getPublic("/api/v1/vessels/" + unknown).statusCode());
    assertEquals(404, getPublic("/api/v1/vessels/" + unknown + "/operational-status").statusCode());
    assertEquals(400, getPublic("/api/v1/vessels/abc").statusCode());
    assertEquals(400, getPublic("/api/v1/vessels/abc/operational-status").statusCode());
  }

  private static String statusEvent(String eventId, String boatId, String status, String at) {
    return "{\"event_id\":\""
        + eventId
        + "\",\"vessel_id\":\""
        + boatId
        + "\",\"operational_status\":\""
        + status
        + "\",\"occurred_at\":\""
        + at
        + "\",\"amount\":50000}";
  }

  @Test
  void module2StatusEventsChangeTheStateWithoutRepeatingOrGoingBack() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "AVISOS");
    String url = "/api/v1/internal/status-events";

    HttpResponse<String> reserved =
        postJson(url, statusEvent("evt-1", boatId, "RESERVED", "2026-11-13T10:00:00-05:00"), null);
    assertEquals(200, reserved.statusCode());
    assertTrue(reserved.body().contains("\"outcome\":\"APPLIED\""));
    assertTrue(
        getPublic("/api/v1/vessels/" + boatId + "/operational-status")
            .body()
            .contains("\"operational_status\":\"RESERVED\""));

    assertTrue(
        postJson(url, statusEvent("evt-1", boatId, "RESERVED", "2026-11-13T10:00:00-05:00"), null)
            .body()
            .contains("\"outcome\":\"DUPLICATE\""));
    assertTrue(
        postJson(url, statusEvent("evt-old", boatId, "AVAILABLE", "2026-01-01T10:00:00Z"), null)
            .body()
            .contains("\"outcome\":\"STALE\""));

    assertTrue(
        postJson(
                url,
                statusEvent("evt-2", boatId, "IN_NAVIGATION", "2026-11-13T11:00:00-05:00"),
                null)
            .body()
            .contains("\"outcome\":\"APPLIED\""));
    assertTrue(
        postJson(
                url,
                statusEvent("evt-3", boatId, "MAINTENANCE_CLEANING", "2026-11-13T12:00:00-05:00"),
                null)
            .body()
            .contains("\"outcome\":\"APPLIED\""));
    assertEquals(
        3,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM vessel_status_history WHERE vessel_id = ? AND source = 'AUTOMATIC'",
            Integer.class,
            boatId));

    // El mantenimiento por aviso no tiene origen: lo termina el propietario.
    assertEquals(
        200,
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/finish", owner).statusCode());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM vessel_status_history WHERE vessel_id = ? AND source = 'MANUAL'",
            Integer.class,
            boatId));
  }

  @Test
  void module2StatusEventsIgnoreUnknownBoatsAndInvalidTransitionsAndRejectBadMessages()
      throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "AVISOS");
    String url = "/api/v1/internal/status-events";

    assertTrue(
        postJson(
                url,
                statusEvent(
                    "evt-9", UUID.randomUUID().toString(), "RESERVED", "2026-11-13T10:00:00Z"),
                null)
            .body()
            .contains("\"outcome\":\"UNKNOWN_VESSEL\""));
    assertTrue(
        postJson(url, statusEvent("evt-8", boatId, "IN_NAVIGATION", "2026-11-13T10:00:00Z"), null)
            .body()
            .contains("\"outcome\":\"INVALID_TRANSITION\""));
    assertEquals(400, postJson(url, "{}", null).statusCode());
    assertEquals(
        400,
        postJson(url, statusEvent("evt-7", boatId, "FLYING", "2026-11-13T10:00:00Z"), null)
            .statusCode());
  }

  private static final String REASON_JSON = "{\"reason\":\"Revisión obligatoria del motor\"}";

  @Test
  void adminSendsToMaintenanceWithAReasonAndTheOwnerIsNotified() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    String admin = login("1010000001");

    HttpResponse<String> response =
        postJson("/api/v1/admin/fleet/boats/" + boatId + "/maintenance/start", REASON_JSON, admin);

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"status\":\"MAINTENANCE\""));
    assertTrue(response.body().contains("\"maintenance_by\":\"ADMIN\""));
    assertEquals(
        "ADMIN",
        jdbc.queryForObject(
            "SELECT maintenance_by FROM vessel WHERE id = ?", String.class, boatId));
    HttpResponse<String> notices = get("/api/v1/notifications", owner);
    assertTrue(notices.body().contains("\"type\":\"MAINTENANCE_NOTICE\""));
    assertTrue(notices.body().contains("Motivo: Revisión obligatoria del motor"));
    assertTrue(
        get("/api/v1/fleet/boats/" + boatId, owner)
            .body()
            .contains("\"maintenance_by\":\"ADMIN\""));
  }

  @Test
  void adminSendWithoutAReasonIs400AndRepeatingTheSendDoesNotNotifyAgain() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");
    String admin = login("1010000001");
    String start = "/api/v1/admin/fleet/boats/" + boatId + "/maintenance/start";

    HttpResponse<String> missing = postJson(start, "{}", admin);
    assertEquals(400, missing.statusCode());
    assertTrue(missing.body().contains("\"field\":\"reason\""));
    assertEquals(
        "AVAILABLE",
        jdbc.queryForObject("SELECT status FROM vessel WHERE id = ?", String.class, boatId));

    assertEquals(200, postJson(start, REASON_JSON, admin).statusCode());
    assertEquals(200, postJson(start, REASON_JSON, admin).statusCode());
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
  }

  @Test
  void adminFinishesOnlyTheMaintenanceHeStartedAndTheOwnerIsNotified() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    String admin = login("1010000001");
    postJson("/api/v1/admin/fleet/boats/" + boatId + "/maintenance/start", REASON_JSON, admin);
    String finish = "/api/v1/admin/fleet/boats/" + boatId + "/maintenance/finish";

    assertEquals(
        409,
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/finish", owner).statusCode());
    HttpResponse<String> response = send("POST", finish, admin);

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"status\":\"AVAILABLE\""));
    assertTrue(response.body().contains("\"maintenance_by\":null"));
    assertTrue(
        get("/api/v1/notifications", owner).body().contains("\"type\":\"VESSEL_AVAILABLE\""));
    assertEquals(200, send("POST", finish, admin).statusCode());
    assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
  }

  @Test
  void adminCannotFinishAMaintenanceTheOwnerStarted() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/start", owner);

    HttpResponse<String> response =
        send(
            "POST",
            "/api/v1/admin/fleet/boats/" + boatId + "/maintenance/finish",
            login("1010000001"));

    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("\"current_status\":\"MAINTENANCE\""));
    assertTrue(response.body().contains("\"maintenance_by\":\"OWNER\""));
    assertEquals(
        "MAINTENANCE",
        jdbc.queryForObject("SELECT status FROM vessel WHERE id = ?", String.class, boatId));
  }

  @Test
  void adminMaintenanceOfAReservedBoatIs409AndOwnersCannotUseTheAdminRoutes() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    String start = "/api/v1/admin/fleet/boats/" + boatId + "/maintenance/start";
    String finish = "/api/v1/admin/fleet/boats/" + boatId + "/maintenance/finish";

    assertEquals(403, postJson(start, REASON_JSON, owner).statusCode());
    assertEquals(403, send("POST", finish, owner).statusCode());
    jdbc.update("UPDATE vessel SET status = 'RESERVED' WHERE id = ?", boatId);
    HttpResponse<String> response = postJson(start, REASON_JSON, login("1010000001"));

    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("\"current_status\":\"RESERVED\""));
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
  }

  @Test
  void ownerRequestsReviewOfAnAdminMaintenanceAndTheAdminSeesTheNotice() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    String admin = login("1010000001");
    postJson("/api/v1/admin/fleet/boats/" + boatId + "/maintenance/start", REASON_JSON, admin);
    String request = "/api/v1/fleet/boats/" + boatId + "/maintenance/request-review";

    assertTrue(
        get("/api/v1/fleet/boats/" + boatId, owner).body().contains("\"review_requested\":false"));
    assertEquals(204, send("POST", request, owner).statusCode());
    assertEquals(204, send("POST", request, owner).statusCode());
    assertTrue(
        get("/api/v1/fleet/boats/" + boatId, owner).body().contains("\"review_requested\":true"));

    HttpResponse<String> notices = get("/api/v1/notifications", admin);
    assertTrue(notices.body().contains("\"type\":\"REVIEW_REQUEST\""));
    assertTrue(notices.body().contains("\"vessel_id\":\"" + boatId + "\""));
    assertTrue(notices.body().contains("\"unread_count\":1"));
    assertEquals(
        "MAINTENANCE",
        jdbc.queryForObject("SELECT status FROM vessel WHERE id = ?", String.class, boatId));
    assertEquals(
        200,
        send("POST", "/api/v1/admin/fleet/boats/" + boatId + "/maintenance/finish", admin)
            .statusCode());
    assertEquals(
        Boolean.FALSE,
        jdbc.queryForObject(
            "SELECT review_requested FROM vessel WHERE id = ?", Boolean.class, boatId));
  }

  @Test
  void requestReviewIsOnlyForTheOwnersBoatInAdminMaintenance() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    String request = "/api/v1/fleet/boats/" + boatId + "/maintenance/request-review";

    assertEquals(409, send("POST", request, owner).statusCode());
    assertEquals(404, send("POST", request, login("1010000003")).statusCode());
    assertEquals(403, send("POST", request, login("1010000001")).statusCode());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
  }

  @Test
  void adminCannotUseTheOwnerDelete() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");

    assertEquals(
        403, send("DELETE", "/api/v1/fleet/boats/" + boatId, login("1010000001")).statusCode());
  }

  @Test
  void editingUpdatesTheBoatAndKeepsThePhotoWhenNoneIsSent() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");

    HttpResponse<String> response = edit(boatId, owner, editForm(), null);

    assertEquals(200, response.statusCode(), response.body());
    assertTrue(response.body().contains("\"name\":\"UNO EDITADA\""));
    assertTrue(response.body().contains("\"max_capacity\":9"));
    assertTrue(response.body().contains("\"base_rate\":\"300000.00\""));
    assertTrue(response.body().contains("https://img.test/boat.png"));
    // Sin servicios en la petición, se conservan los guardados.
    assertTrue(response.body().contains("\"life_jackets\":true"));
    assertEquals(200, get("/api/v1/fleet/boats/" + boatId, owner).statusCode());
  }

  @Test
  void editingWithAPhotoAndServicesReplacesThem() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    when(images.store(any(), anyString(), anyString()))
        .thenReturn(new StoredImage("https://img.test/new.png", "seashare/boats/new"));
    Map<String, String> form = editForm();
    form.put("life_jackets", "false");
    form.put("diving_gear", "true");

    HttpResponse<String> response = edit(boatId, owner, form, PNG);

    assertEquals(200, response.statusCode(), response.body());
    assertTrue(response.body().contains("https://img.test/new.png"));
    assertTrue(response.body().contains("\"life_jackets\":false"));
    assertTrue(response.body().contains("\"diving_gear\":true"));
  }

  @Test
  void changingOnlyTheCoordinatesUpdatesTheSharedPort() throws Exception {
    String first = login("1010000002");
    String second = login("1010000003");
    String idOne = registerBoat(first, "UNO");
    String idTwo =
        field(
            postJson(
                    "/api/v1/fleet/boats/draft",
                    draftJson("DOS").replace("CP-01-2026-A", "CP-02-2026-B"),
                    second)
                .body(),
            "boat_id");
    assertEquals(200, complete(idTwo, second, validForm(), PNG).statusCode());
    Map<String, String> form = editForm();
    form.put("port_name", "Marina Central");
    form.put("latitude", "10.5");
    form.put("longitude", "-75.6");

    assertEquals(200, edit(idOne, first, form, null).statusCode());

    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM port", Integer.class));
    String other = get("/api/v1/fleet/boats/" + idTwo, second).body();
    assertTrue(other.contains("\"latitude\":\"10.5000\""));
    assertTrue(other.contains("\"longitude\":\"-75.6000\""));
  }

  @Test
  void aNewPortNameCreatesAnotherPort() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    Map<String, String> form = editForm();
    form.put("port_name", "Puerto Nuevo");

    assertEquals(200, edit(boatId, owner, form, null).statusCode());

    assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM port", Integer.class));
  }

  @Test
  void editingAReservedBoatIs409WithItsStatusAndChangesNothing() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    jdbc.update("UPDATE vessel SET status = 'RESERVED' WHERE id = ?", boatId);
    Map<String, String> form = editForm();
    form.put("port_name", "Puerto Nuevo");

    HttpResponse<String> response = edit(boatId, owner, form, null);

    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("\"current_status\":\"RESERVED\""));
    assertEquals(
        "UNO", jdbc.queryForObject("SELECT name FROM vessel WHERE id = ?", String.class, boatId));
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM port", Integer.class));
  }

  @Test
  void aBoatInMaintenanceCanBeEdited() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    jdbc.update("UPDATE vessel SET status = 'MAINTENANCE' WHERE id = ?", boatId);

    HttpResponse<String> response = edit(boatId, owner, editForm(), null);

    assertEquals(200, response.statusCode(), response.body());
    assertTrue(response.body().contains("\"status\":\"MAINTENANCE\""));
  }

  @Test
  void editingAnotherOwnersDeletedOrUnknownBoatIs404() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");
    String other = login("1010000003");

    assertEquals(404, edit(boatId, other, editForm(), null).statusCode());
    assertEquals(
        404, edit("00000000-0000-0000-0000-000000000000", other, editForm(), null).statusCode());
    send("DELETE", "/api/v1/fleet/boats/" + boatId, login("1010000002"));
    assertEquals(404, edit(boatId, login("1010000002"), editForm(), null).statusCode());
  }

  @Test
  void editingWithInvalidDataIs400WithFieldErrorsAndChangesNothing() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    Map<String, String> form = editForm();
    form.put("name", "12");
    form.put("max_capacity", "11");
    form.put("base_rate", "0");

    HttpResponse<String> response = edit(boatId, owner, form, null);

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("\"field\":\"name\""));
    assertTrue(response.body().contains("\"field\":\"max_capacity\""));
    assertTrue(response.body().contains("\"field\":\"base_rate\""));
    assertEquals(
        "UNO", jdbc.queryForObject("SELECT name FROM vessel WHERE id = ?", String.class, boatId));
  }

  @Test
  void editingWithAnInvalidPhotoIs400() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");

    HttpResponse<String> response = edit(boatId, owner, editForm(), "texto".getBytes());

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("Formato de imagen inválido"));
  }

  @Test
  void adminCannotUseTheOwnerEdit() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");

    assertEquals(403, edit(boatId, login("1010000001"), editForm(), null).statusCode());
  }

  @Test
  void theOwnerSendsABoatToMaintenanceAndBringsItBack() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");

    HttpResponse<String> started =
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/start", owner);

    assertEquals(200, started.statusCode());
    assertTrue(started.body().contains("\"status\":\"MAINTENANCE\""));
    assertEquals(
        "OWNER",
        jdbc.queryForObject(
            "SELECT maintenance_by FROM vessel WHERE id = ?", String.class, boatId));

    HttpResponse<String> finished =
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/finish", owner);

    assertEquals(200, finished.statusCode());
    assertTrue(finished.body().contains("\"status\":\"AVAILABLE\""));
    assertNull(
        jdbc.queryForObject(
            "SELECT maintenance_by FROM vessel WHERE id = ?", String.class, boatId));
  }

  @Test
  void repeatingTheMaintenanceChangeIsHarmless() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    String start = "/api/v1/fleet/boats/" + boatId + "/maintenance/start";
    String finish = "/api/v1/fleet/boats/" + boatId + "/maintenance/finish";

    assertEquals(200, send("POST", finish, owner).statusCode());
    assertEquals(200, send("POST", start, owner).statusCode());
    assertEquals(200, send("POST", start, owner).statusCode());
  }

  @Test
  void aReservedBoatCannotGoToMaintenance() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    jdbc.update("UPDATE vessel SET status = 'RESERVED' WHERE id = ?", boatId);

    HttpResponse<String> response =
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/start", owner);

    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("\"current_status\":\"RESERVED\""));
    assertEquals(
        "RESERVED",
        jdbc.queryForObject("SELECT status FROM vessel WHERE id = ?", String.class, boatId));
  }

  @Test
  void theOwnerCannotLiftAnAdminSuspension() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    jdbc.update(
        "UPDATE vessel SET status = 'MAINTENANCE', maintenance_by = 'ADMIN' WHERE id = ?", boatId);

    HttpResponse<String> response =
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/finish", owner);

    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("\"current_status\":\"MAINTENANCE\""));
    assertTrue(response.body().contains("\"maintenance_by\":\"ADMIN\""));
    assertEquals(
        "MAINTENANCE",
        jdbc.queryForObject("SELECT status FROM vessel WHERE id = ?", String.class, boatId));
  }

  @Test
  void maintenanceOnAnotherOwnersOrUnknownBoatIs404() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");
    String other = login("1010000003");

    assertEquals(
        404,
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/start", other).statusCode());
    assertEquals(
        404,
        send("POST", "/api/v1/fleet/boats/" + UUID.randomUUID() + "/maintenance/finish", other)
            .statusCode());
  }

  @Test
  void adminCannotUseTheOwnerMaintenance() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");

    assertEquals(
        403,
        send("POST", "/api/v1/fleet/boats/" + boatId + "/maintenance/start", login("1010000001"))
            .statusCode());
  }

  @Test
  void adminListsAllBoatsWithOwnersAndFilters() throws Exception {
    String owner = login("1010000002");
    String other = login("1010000003");
    String first = registerBoat(owner, "UNO");
    registerBoat(other, "DOS", "CP-02-2026-B");
    jdbc.update("UPDATE vessel SET status = 'MAINTENANCE' WHERE id = ?", first);
    String admin = login("1010000001");

    HttpResponse<String> all = get("/api/v1/admin/fleet/boats", admin);
    assertEquals(200, all.statusCode());
    assertTrue(all.body().contains("\"name\":\"UNO\""));
    assertTrue(all.body().contains("\"name\":\"DOS\""));
    assertTrue(all.body().contains("\"full_name\":\"Carlos Andrés Mejía\""));

    HttpResponse<String> byStatus = get("/api/v1/admin/fleet/boats?status=MAINTENANCE", admin);
    assertTrue(byStatus.body().contains("\"name\":\"UNO\""));
    assertFalse(byStatus.body().contains("\"name\":\"DOS\""));

    HttpResponse<String> byOwner = get("/api/v1/admin/fleet/boats?q=1010000003", admin);
    assertTrue(byOwner.body().contains("\"name\":\"DOS\""));
    assertFalse(byOwner.body().contains("\"name\":\"UNO\""));
  }

  @Test
  void adminListsOwnersWithBoats() throws Exception {
    registerBoat(login("1010000002"), "UNO");

    HttpResponse<String> response = get("/api/v1/admin/fleet/owners", login("1010000001"));

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"full_name\":\"Carlos Andrés Mejía\""));
    assertFalse(response.body().contains("María Fernanda"));
  }

  @Test
  void adminSeesTheDetailOfAnyBoat() throws Exception {
    String boatId = registerBoat(login("1010000002"), "UNO");

    HttpResponse<String> response = get("/api/v1/admin/fleet/boats/" + boatId, login("1010000001"));

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"name\":\"UNO\""));
  }

  @Test
  void adminDetailOfUnknownOrDeletedBoatIs404() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");
    send("DELETE", "/api/v1/fleet/boats/" + boatId, owner);
    String admin = login("1010000001");

    assertEquals(404, get("/api/v1/admin/fleet/boats/" + boatId, admin).statusCode());
    assertEquals(404, get("/api/v1/admin/fleet/boats/" + UUID.randomUUID(), admin).statusCode());
  }

  @Test
  void invalidAdminFiltersAre400() throws Exception {
    String admin = login("1010000001");

    HttpResponse<String> response =
        get("/api/v1/admin/fleet/boats?status=NOPE&owner_id=abc", admin);

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("\"field\":\"status\""));
    assertTrue(response.body().contains("\"field\":\"owner_id\""));
  }

  @Test
  void ownersCannotUseTheAdminFleet() throws Exception {
    String owner = login("1010000002");
    String boatId = registerBoat(owner, "UNO");

    assertEquals(403, get("/api/v1/admin/fleet/boats", owner).statusCode());
    assertEquals(403, get("/api/v1/admin/fleet/owners", owner).statusCode());
    assertEquals(403, get("/api/v1/admin/fleet/boats/" + boatId, owner).statusCode());
  }

  @Test
  void adminSearchesOwnersByNameAndDocument() throws Exception {
    String admin = login("1010000001");

    HttpResponse<String> byName = get("/api/v1/admin/owners?q=carlos", admin);
    assertEquals(200, byName.statusCode());
    assertTrue(byName.body().contains("\"full_name\":\"Carlos Andrés Mejía\""));
    assertTrue(byName.body().contains("\"has_more\":false"));
    assertFalse(byName.body().contains("Laura"));

    HttpResponse<String> byDocument = get("/api/v1/admin/owners?q=1.010.000.003", admin);
    assertTrue(byDocument.body().contains("María Fernanda Ospina"));
    assertFalse(byDocument.body().contains("Carlos"));
  }

  @Test
  void ownerSearchNeedsAtLeastThreeCharacters() throws Exception {
    HttpResponse<String> response = get("/api/v1/admin/owners?q=ca", login("1010000001"));

    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("\"field\":\"q\""));
    assertEquals(400, get("/api/v1/admin/owners", login("1010000001")).statusCode());
  }

  @Test
  void adminSeesAnOwnersInformationWithoutThePassword() throws Exception {
    String admin = login("1010000001");
    String ownerId =
        jdbc.queryForObject(
            "SELECT id FROM users WHERE document_number = 1010000002", String.class);

    HttpResponse<String> response = get("/api/v1/admin/owners/" + ownerId, admin);

    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"document_number\":\"1010000002\""));
    assertTrue(response.body().contains("\"email\":\"carlos.mejia@seashare.test\""));
    assertTrue(response.body().contains("\"phone\":\"3001000002\""));
    assertFalse(response.body().contains("password"));
  }

  @Test
  void ownerInformationOfAnAdminOrUnknownIs404() throws Exception {
    String admin = login("1010000001");
    String adminId =
        jdbc.queryForObject(
            "SELECT id FROM users WHERE document_number = 1010000001", String.class);

    assertEquals(404, get("/api/v1/admin/owners/" + adminId, admin).statusCode());
    assertEquals(404, get("/api/v1/admin/owners/" + UUID.randomUUID(), admin).statusCode());
  }

  @Test
  void ownersCannotSearchOrReadOwners() throws Exception {
    String owner = login("1010000002");

    assertEquals(403, get("/api/v1/admin/owners?q=carlos", owner).statusCode());
    assertEquals(403, get("/api/v1/admin/owners/" + UUID.randomUUID(), owner).statusCode());
  }

  private static Map<String, String> editForm() {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("name", "  UNO   EDITADA ");
    form.put("max_capacity", "9");
    form.put("port_name", "Marina Central");
    form.put("latitude", "10.4");
    form.put("longitude", "-75.5");
    form.put("base_rate", "300000");
    return form;
  }

  private HttpResponse<String> edit(
      String boatId, String token, Map<String, String> form, byte[] photo) throws Exception {
    return multipart("PUT", "/api/v1/fleet/boats/" + boatId, token, form, photo);
  }

  private String registerBoat(String token, String name) throws Exception {
    return registerBoat(token, name, "CP-01-2026-A");
  }

  /** Registra una embarcación completa; la matrícula debe ser distinta si hay otra en la prueba. */
  private String registerBoat(String token, String name, String legalRegistration)
      throws Exception {
    String boatId =
        field(
            postJson(
                    "/api/v1/fleet/boats/draft",
                    draftJson(name).replace("CP-01-2026-A", legalRegistration),
                    token)
                .body(),
            "boat_id");
    assertEquals(200, complete(boatId, token, validForm(), PNG).statusCode());
    return boatId;
  }

  private static Map<String, String> validForm() {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("port_name", "Marina Central");
    form.put("latitude", "10.4");
    form.put("longitude", "-75.5");
    form.put("base_rate", "250000");
    form.put("life_jackets", "true");
    return form;
  }

  private static String draftJson(String name) {
    return "{\"name\":\""
        + name
        + "\",\"legal_registration\":\"CP-01-2026-A\",\"type\":\"MOTORBOAT\",\"max_capacity\":8}";
  }

  private String login(String document) throws Exception {
    String body = "{\"document_number\":\"" + document + "\",\"password\":\"SeaShare2026!\"}";
    HttpResponse<String> response = postJson("/api/v1/auth/login", body, null);
    return field(response.body(), "access_token");
  }

  private static String field(String json, String name) {
    return json.replaceAll(".*\"" + name + "\"\\s*:\\s*\"([^\"]+)\".*", "$1");
  }

  private HttpResponse<String> postJson(String path, String json, String token) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return http.send(
        builder.POST(HttpRequest.BodyPublishers.ofString(json)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  /** Consulta sin cabecera de autenticación, como la hacen los otros módulos. */
  private HttpResponse<String> getPublic(String path) throws Exception {
    return http.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path, String token) throws Exception {
    return send("GET", path, token);
  }

  private HttpResponse<String> deleteAsAdmin(String boatId, String token, String json)
      throws Exception {
    return http.send(
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/api/v1/admin/fleet/boats/" + boatId))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .method("DELETE", HttpRequest.BodyPublishers.ofString(json))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> send(String method, String path, String token) throws Exception {
    return http.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Authorization", "Bearer " + token)
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> complete(
      String boatId, String token, Map<String, String> form, byte[] photo) throws Exception {
    return multipart("POST", "/api/v1/fleet/boats/" + boatId + "/registration", token, form, photo);
  }

  private HttpResponse<String> multipart(
      String method, String path, String token, Map<String, String> form, byte[] photo)
      throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (Map.Entry<String, String> entry : form.entrySet()) {
      write(
          out,
          "--"
              + BOUNDARY
              + "\r\nContent-Disposition: form-data; name=\""
              + entry.getKey()
              + "\"\r\n\r\n"
              + entry.getValue()
              + "\r\n");
    }
    if (photo != null) {
      write(
          out,
          "--"
              + BOUNDARY
              + "\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"boat.png\""
              + "\r\nContent-Type: image/png\r\n\r\n");
      out.write(photo);
      write(out, "\r\n");
    }
    write(out, "--" + BOUNDARY + "--\r\n");
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .method(method, HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static void write(ByteArrayOutputStream out, String text) throws Exception {
    out.write(text.getBytes(StandardCharsets.UTF_8));
  }
}
