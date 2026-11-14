package com.seashare.seasharem1.unit.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.VesselEntity;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.NotificationPersistenceMapperImpl;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.PortPersistenceMapperImpl;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapperImpl;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.VesselPersistenceMapperImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Comprueba que convertir a entidad y volver a dominio no pierde ningún dato. */
class PersistenceMappersTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Test
  void userRoundTrip() {
    var mapper = new UserPersistenceMapperImpl();
    User user = new User(ID, "Laura Gómez", 1010000001L, "a@b.test", "300", "hash", UserRole.OWNER);

    assertEquals(user, mapper.toDomain(mapper.toEntity(user)));
  }

  @Test
  void portRoundTrip() {
    var mapper = new PortPersistenceMapperImpl();
    Port port =
        new Port(ID, "Cartagena", new BigDecimal("10.391049"), new BigDecimal("-75.479426"));

    assertEquals(port, mapper.toDomain(mapper.toEntity(port)));
  }

  @Test
  void completeVesselRoundTrip() {
    var mapper = new VesselPersistenceMapperImpl();
    Vessel vessel =
        new Vessel(
            ID,
            "Atlántico Star",
            "CP-05-1042-A",
            VesselType.CATAMARAN,
            25,
            "https://example.test/photo.jpg",
            true,
            false,
            true,
            false,
            true,
            new BigDecimal("180000.00"),
            VesselStatus.MAINTENANCE,
            MaintenanceBy.OWNER,
            OTHER_ID,
            ID,
            true,
            LocalDateTime.of(2026, 11, 4, 10, 30, 15));

    VesselEntity entity = mapper.toEntity(vessel);

    assertEquals((short) 25, entity.getMaxCapacity());
    assertEquals(vessel, mapper.toDomain(entity));
  }

  @Test
  void draftVesselKeepsStepTwoColumnsNull() {
    var mapper = new VesselPersistenceMapperImpl();
    Vessel draft =
        new Vessel(
            ID,
            "Borrador",
            "CP-01-0001-B",
            VesselType.MOTORBOAT,
            8,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            VesselStatus.DRAFT,
            null,
            null,
            ID,
            false,
            null);

    VesselEntity entity = mapper.toEntity(draft);

    assertNull(entity.getPhotoPath());
    assertNull(entity.getPortId());
    assertNull(entity.getBaseRate());
    assertEquals(draft, mapper.toDomain(entity));
  }

  @Test
  void notificationRoundTrip() {
    var mapper = new NotificationPersistenceMapperImpl();
    Notification notification =
        new Notification(
            ID,
            OTHER_ID,
            ID,
            NotificationType.MAINTENANCE_NOTICE,
            "Motivo del mantenimiento",
            true,
            LocalDateTime.of(2026, 11, 2, 20, 0));

    assertEquals(notification, mapper.toDomain(mapper.toEntity(notification)));
  }
}
