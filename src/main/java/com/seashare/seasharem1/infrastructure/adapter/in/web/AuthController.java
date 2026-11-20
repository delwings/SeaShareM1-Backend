package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.LoginCommand;
import com.seashare.seasharem1.application.port.in.LoginUseCase;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.LoginRequest;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint de autenticación. Solo traduce HTTP a casos de uso: no tiene lógica de negocio. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final LoginUseCase loginUseCase;

  public AuthController(LoginUseCase loginUseCase) {
    this.loginUseCase = loginUseCase;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    LoginCommand command =
        new LoginCommand(Long.parseLong(request.documentNumber()), request.password());
    return LoginResponse.from(loginUseCase.login(command));
  }
}
