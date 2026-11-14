package com.seashare.seasharem1.infrastructure.security;

import com.seashare.seasharem1.application.port.out.PasswordVerifierPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Compara contraseñas contra el hash guardado usando el PasswordEncoder (BCrypt). */
@Component
public class BcryptPasswordVerifier implements PasswordVerifierPort {

  private final PasswordEncoder passwordEncoder;

  public BcryptPasswordVerifier(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public boolean matches(String rawPassword, String passwordHash) {
    return passwordEncoder.matches(rawPassword, passwordHash);
  }
}
