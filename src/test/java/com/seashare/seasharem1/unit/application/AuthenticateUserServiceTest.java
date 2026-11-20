package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.seashare.seasharem1.application.port.in.LoginCommand;
import com.seashare.seasharem1.application.port.in.LoginResult;
import com.seashare.seasharem1.application.port.out.IssuedToken;
import com.seashare.seasharem1.application.port.out.PasswordVerifierPort;
import com.seashare.seasharem1.application.port.out.TokenIssuerPort;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.usecase.AuthenticateUserService;
import com.seashare.seasharem1.domain.exception.InvalidCredentialsException;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

  private static final User USER =
      new User(
          UUID.randomUUID(),
          "Carlos Mejía",
          1010000002L,
          "c@x.test",
          "300",
          "hash",
          UserRole.OWNER);

  @Mock private UserRepositoryPort users;
  @Mock private PasswordVerifierPort passwords;
  @Mock private TokenIssuerPort tokens;

  private AuthenticateUserService service;

  @BeforeEach
  void setUp() {
    service = new AuthenticateUserService(users, passwords, tokens);
  }

  @Test
  void returnsTokenAndUserWhenCredentialsAreCorrect() {
    when(users.findByDocumentNumber(1010000002L)).thenReturn(Optional.of(USER));
    when(passwords.matches("secret", "hash")).thenReturn(true);
    when(tokens.issue(USER)).thenReturn(new IssuedToken("jwt-token", 1800));

    LoginResult result = service.login(new LoginCommand(1010000002L, "secret"));

    assertEquals("jwt-token", result.accessToken());
    assertEquals(1800, result.expiresInSeconds());
    assertEquals(USER, result.user());
  }

  @Test
  void failsWhenTheDocumentDoesNotExist() {
    when(users.findByDocumentNumber(999L)).thenReturn(Optional.empty());

    assertThrows(
        InvalidCredentialsException.class, () -> service.login(new LoginCommand(999L, "secret")));

    verify(tokens, never()).issue(any());
  }

  @Test
  void failsWhenThePasswordIsWrong() {
    when(users.findByDocumentNumber(1010000002L)).thenReturn(Optional.of(USER));
    when(passwords.matches("wrong", "hash")).thenReturn(false);

    assertThrows(
        InvalidCredentialsException.class,
        () -> service.login(new LoginCommand(1010000002L, "wrong")));

    verify(tokens, never()).issue(any());
  }
}
