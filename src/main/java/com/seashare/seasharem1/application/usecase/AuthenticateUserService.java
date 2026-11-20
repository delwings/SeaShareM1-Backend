package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.LoginCommand;
import com.seashare.seasharem1.application.port.in.LoginResult;
import com.seashare.seasharem1.application.port.in.LoginUseCase;
import com.seashare.seasharem1.application.port.out.IssuedToken;
import com.seashare.seasharem1.application.port.out.PasswordVerifierPort;
import com.seashare.seasharem1.application.port.out.TokenIssuerPort;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.domain.exception.InvalidCredentialsException;
import com.seashare.seasharem1.domain.model.User;

/** Caso de uso de inicio de sesión. Java puro: se registra como bean desde configuration. */
public class AuthenticateUserService implements LoginUseCase {

  private final UserRepositoryPort users;
  private final PasswordVerifierPort passwords;
  private final TokenIssuerPort tokens;

  public AuthenticateUserService(
      UserRepositoryPort users, PasswordVerifierPort passwords, TokenIssuerPort tokens) {
    this.users = users;
    this.passwords = passwords;
    this.tokens = tokens;
  }

  @Override
  public LoginResult login(LoginCommand command) {
    User user =
        users
            .findByDocumentNumber(command.documentNumber())
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwords.matches(command.password(), user.password())) {
      throw new InvalidCredentialsException();
    }

    IssuedToken token = tokens.issue(user);
    return new LoginResult(token.value(), token.expiresInSeconds(), user);
  }
}
