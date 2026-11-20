package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.CheckLegalRegistrationUseCase;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.util.List;

/** Verifica si una matrícula está libre. Primero se revisa el formato, y solo después si existe. */
public class CheckLegalRegistrationService implements CheckLegalRegistrationUseCase {

  private final VesselRepositoryPort vessels;

  public CheckLegalRegistrationService(VesselRepositoryPort vessels) {
    this.vessels = vessels;
  }

  @Override
  public boolean isAvailable(String legalRegistration) {
    String normalized = RegistrationRules.normalize(legalRegistration);
    String error = RegistrationRules.legalRegistrationError(normalized);
    if (error != null) {
      throw new ValidationFailedException(List.of(new FieldViolation("legal_registration", error)));
    }
    return !vessels.existsByLegalRegistration(normalized);
  }
}
