package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.CompleteRegistrationCommand;
import com.seashare.seasharem1.application.port.in.CompleteRegistrationUseCase;
import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.port.out.ImageStoragePort;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.StoredImage;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.rules.Parsed;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Paso 2 del registro de una embarcación: completa los datos y la deja Disponible. */
public class CompleteRegistrationService implements CompleteRegistrationUseCase {

  static final String DRAFT_GONE = "El borrador ya no existe.";
  static final String PHOTO_FOLDER = "seashare/boats";

  private final VesselRepositoryPort vessels;
  private final PortRepositoryPort ports;
  private final ImageStoragePort images;
  private final Clock clock;

  public CompleteRegistrationService(
      VesselRepositoryPort vessels,
      PortRepositoryPort ports,
      ImageStoragePort images,
      Clock clock) {
    this.vessels = vessels;
    this.ports = ports;
    this.images = images;
    this.clock = clock;
  }

  @Override
  public CompletedBoat complete(CompleteRegistrationCommand command) {
    Vessel vessel =
        vessels
            .findById(command.boatId())
            .filter(found -> found.ownerId().equals(command.ownerId()))
            .orElseThrow(() -> new ResourceNotFoundException(DRAFT_GONE));

    if (vessel.status() == VesselStatus.AVAILABLE) {
      // Reintento de un guardado que sí se completó: se devuelve el resultado, sin repetir nada.
      return new CompletedBoat(vessel, findPort(vessel.portId()));
    }
    if (!vessel.isDraft()) {
      throw new BusinessConflictException("La embarcación no está en estado de borrador.");
    }

    String portName = RegistrationRules.normalize(command.portName());
    Parsed<BigDecimal> latitude = RegistrationRules.parseCoordinate(command.latitude(), 90);
    Parsed<BigDecimal> longitude = RegistrationRules.parseCoordinate(command.longitude(), 180);
    Parsed<Long> baseRate = RegistrationRules.parseBaseRate(command.baseRate());

    List<FieldViolation> violations = new ArrayList<>();
    CreateDraftService.addIfPresent(
        violations,
        "port_name",
        RegistrationRules.nameError(portName, RegistrationRules.PORT_NAME_INVALID));
    CreateDraftService.addIfPresent(
        violations, "coordinates", latitude.isError() ? latitude.error() : longitude.error());
    CreateDraftService.addIfPresent(violations, "base_rate", baseRate.error());
    CreateDraftService.addIfPresent(
        violations, "photo", RegistrationRules.imageError(command.photo()));
    if (!violations.isEmpty()) {
      throw new ValidationFailedException(violations);
    }

    Port port =
        ports
            .findByName(portName)
            .orElseGet(
                () ->
                    ports.save(
                        new Port(
                            UUID.randomUUID(), portName, latitude.value(), longitude.value())));
    StoredImage image =
        images.store(
            command.photo(), RegistrationRules.detectImageType(command.photo()), PHOTO_FOLDER);

    Vessel completed =
        vessel.complete(
            image.url(),
            command.services(),
            BigDecimal.valueOf(baseRate.value()),
            port.id(),
            LocalDateTime.now(clock));
    return new CompletedBoat(vessels.save(completed), port);
  }

  private Port findPort(UUID portId) {
    return ports
        .findById(portId)
        .orElseThrow(() -> new IllegalStateException("La embarcación registrada no tiene puerto"));
  }
}
