package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seashare.seasharem1.application.port.in.LoginResult;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import java.util.UUID;

/** Respuesta de un inicio de sesión correcto. Nunca incluye la contraseña ni su hash. */
public record LoginResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("user") UserSummary user) {

  public static LoginResponse from(LoginResult result) {
    return new LoginResponse(
        result.accessToken(), "Bearer", result.expiresInSeconds(), UserSummary.from(result.user()));
  }

  /** Datos básicos del usuario autenticado. */
  public record UserSummary(
      @JsonProperty("id") UUID id,
      @JsonProperty("full_name") String fullName,
      @JsonProperty("role") UserRole role) {

    static UserSummary from(User user) {
      return new UserSummary(user.id(), user.fullName(), user.role());
    }
  }
}
