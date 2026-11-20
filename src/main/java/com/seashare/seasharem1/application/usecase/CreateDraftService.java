package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.CreateDraftCommand;
import com.seashare.seasharem1.application.port.in.CreateDraftUseCase;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselType;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Paso 1 del registro de una embarcación. */
public class CreateDraftService implements CreateDraftUseCase {

  static final String DRAFT_EXISTS =
      "Ya tienes un borrador de registro. Continúalo o elimínalo antes de empezar uno nuevo.";

  private final VesselRepositoryPort vessels;

  public CreateDraftService(VesselRepositoryPort vessels) {
    this.vessels = vessels;
  }

  @Override
  public Vessel createDraft(CreateDraftCommand command) {
    String name = RegistrationRules.normalize(command.name());
    String legalRegistration = RegistrationRules.normalize(command.legalRegistration());
    VesselType type = RegistrationRules.parseType(command.type());

    List<FieldViolation> violations = new ArrayList<>();
    addIfPresent(
        violations, "name", RegistrationRules.nameError(name, RegistrationRules.NAME_INVALID));
    addIfPresent(
        violations,
        "legal_registration",
        RegistrationRules.legalRegistrationError(legalRegistration));
    addIfPresent(violations, "type", RegistrationRules.typeError(command.type()));
    addIfPresent(
        violations, "max_capacity", RegistrationRules.capacityError(command.maxCapacity(), type));
    if (!violations.isEmpty()) {
      throw new ValidationFailedException(violations);
    }

    Optional<Vessel> existingDraft = vessels.findDraftByOwner(command.ownerId());
    if (existingDraft.isPresent()) {
      // Mismo borrador y misma matrícula: es un reintento, no se crea otro.
      if (existingDraft.get().legalRegistration().equals(legalRegistration)) {
        return existingDraft.get();
      }
      throw new BusinessConflictException(DRAFT_EXISTS);
    }
    if (vessels.existsByLegalRegistration(legalRegistration)) {
      throw legalRegistrationTaken();
    }
    return vessels.save(
        Vessel.newDraft(
            UUID.randomUUID(),
            command.ownerId(),
            name,
            legalRegistration,
            type,
            command.maxCapacity()));
  }

  static BusinessConflictException legalRegistrationTaken() {
    return new BusinessConflictException(
        RegistrationRules.LEGAL_REGISTRATION_TAKEN,
        List.of(
            new FieldViolation("legal_registration", RegistrationRules.LEGAL_REGISTRATION_TAKEN)));
  }

  static void addIfPresent(List<FieldViolation> violations, String field, String message) {
    if (message != null) {
      violations.add(new FieldViolation(field, message));
    }
  }
}
