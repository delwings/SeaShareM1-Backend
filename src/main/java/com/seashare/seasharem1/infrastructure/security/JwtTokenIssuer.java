package com.seashare.seasharem1.infrastructure.security;

import com.seashare.seasharem1.application.port.out.IssuedToken;
import com.seashare.seasharem1.application.port.out.TokenIssuerPort;
import com.seashare.seasharem1.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite tokens JWT firmados con HS256. El token lleva el id del usuario (sub) y su rol (claim
 * "role"), que el filtro de seguridad convierte en la autoridad ROLE_OWNER o ROLE_ADMIN.
 */
@Component
public class JwtTokenIssuer implements TokenIssuerPort {

  public static final String ROLE_CLAIM = "role";
  private static final String ISSUER = "seasharem1";

  private final JwtEncoder encoder;
  private final JwtProperties properties;
  private final Clock clock;

  public JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public IssuedToken issue(User user) {
    Instant now = clock.instant();
    Duration lifetime = Duration.ofMinutes(properties.expirationMinutes());

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(now)
            .expiresAt(now.plus(lifetime))
            .subject(user.id().toString())
            .claim(ROLE_CLAIM, user.role().name())
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedToken(token, lifetime.toSeconds());
  }
}
