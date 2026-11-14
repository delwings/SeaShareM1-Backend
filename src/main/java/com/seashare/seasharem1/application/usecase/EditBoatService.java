package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.port.in.EditBoatCommand;
import com.seashare.seasharem1.application.port.in.EditBoatUseCase;
import com.seashare.seasharem1.application.port.out.ImageStoragePort;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.StoredImage;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BoatNotEditableException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.rules.Parsed;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Edición de una embarcación registrada. Orden de verificación (FR-025): existe y es del
 * propietario, el estado lo permite, los campos son válidos y, por último, se guarda. No hay
 * control de versión: si el propietario guarda desde dos sesiones, gana el último guardado.
 */
public class EditBoatService implements EditBoatUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";

  private final VesselRepositoryPort vessels;
  private final PortRepositoryPort ports;
  private final ImageStoragePort images;

  public EditBoatService(
      VesselRepositoryPort vessels, PortRepositoryPort ports, ImageStoragePort images) {
    this.vessels = vessels;
    this.ports = ports;
    this.images = images;
  }

  @Override
  public CompletedBoat edit(EditBoatCommand command) {
    Vessel vessel = findOwned(command.ownerId(), command.boatId());
    if (!vessel.canBeEdited()) {
      throw new BoatNotEditableException(vessel.status());
    }

    String name = RegistrationRules.normalizeName(command.name());
    String portName = RegistrationRules.normalizeName(command.portName());
    Parsed<Integer> capacity =
        RegistrationRules.parseCapacity(command.maxCapacity(), vessel.type());
    Parsed<Long> baseRate = RegistrationRules.parseBaseRate(command.baseRate());
    Parsed<BigDecimal> latitude = RegistrationRules.parseCoordinate(command.latitude(), 90);
    Parsed<BigDecimal> longitude = RegistrationRules.parseCoordinate(command.longitude(), 180);

    List<FieldViolation> violations = new ArrayList<>();
    CreateDraftService.addIfPresent(
        violations, "name", RegistrationRules.nameError(name, RegistrationRules.NAME_INVALID));
    CreateDraftService.addIfPresent(violations, "max_capacity", capacity.error());
    CreateDraftService.addIfPresent(violations, "base_rate", baseRate.error());
    CreateDraftService.addIfPresent(
        violations,
        "port_name",
        RegistrationRules.nameError(portName, RegistrationRules.PORT_NAME_INVALID));
    CreateDraftService.addIfPresent(
        violations, "coordinates", latitude.isError() ? latitude.error() : longitude.error());
    if (command.photo() != null) {
      CreateDraftService.addIfPresent(
          violations, "photo", RegistrationRules.imageError(command.photo()));
    }
    if (!violations.isEmpty()) {
      throw new ValidationFailedException(violations);
    }

    String photoPath = vessel.photoPath();
    if (command.photo() != null) {
      StoredImage image =
          images.store(
              command.photo(),
              RegistrationRules.detectImageType(command.photo()),
              CompleteRegistrationService.PHOTO_FOLDER);
      photoPath = image.url();
    }
    AdditionalServices services =
        command.services() != null ? command.services() : vessel.additionalServices();

    Port desiredPort = new Port(UUID.randomUUID(), portName, latitude.value(), longitude.value());
    Vessel edited =
        vessel.edited(
            name,
            capacity.value(),
            photoPath,
            services,
            BigDecimal.valueOf(baseRate.value()),
            desiredPort.id());
    if (!vessels.applyEdit(edited, desiredPort)) {
      // Otro cambio se adelantó: se relee para dar el error correcto (404 si la eliminaron, 409 con
      // el estado nuevo si cambió de estado).
      throw new BoatNotEditableException(findOwned(command.ownerId(), command.boatId()).status());
    }

    Vessel saved = findOwned(command.ownerId(), command.boatId());
    Port savedPort =
        ports
            .findById(saved.portId())
            .orElseThrow(() -> new IllegalStateException("La embarcación editada no tiene puerto"));
    return new CompletedBoat(saved, savedPort);
  }

  private Vessel findOwned(UUID ownerId, UUID boatId) {
    return vessels
        .findById(boatId)
        .filter(found -> found.ownerId().equals(ownerId))
        .filter(found -> !found.isDraft())
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }
}
