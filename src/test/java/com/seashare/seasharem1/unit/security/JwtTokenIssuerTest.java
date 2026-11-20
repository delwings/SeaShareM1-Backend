package com.seashare.seasharem1.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.seashare.seasharem1.application.port.out.IssuedToken;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.infrastructure.security.JwtProperties;
import com.seashare.seasharem1.infrastructure.security.JwtTokenIssuer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenIssuerTest {

  private static final SecretKey KEY =
      new SecretKeySpec(
          "una-clave-de-prueba-de-al-menos-32-caracteres".getBytes(StandardCharsets.UTF_8),
          "HmacSHA256");

  @Test
  void issuesAValidTokenWithUserIdRoleAndThirtyMinutesOfLife() {
    var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(KEY));
    var issuer = new JwtTokenIssuer(encoder, new JwtProperties("ignorado", 30), Clock.systemUTC());
    UUID id = UUID.randomUUID();
    User user = new User(id, "Laura", 1010000001L, "l@x.test", "300", "hash", UserRole.ADMIN);

    IssuedToken token = issuer.issue(user);

    Jwt jwt =
        NimbusJwtDecoder.withSecretKey(KEY)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .decode(token.value());
    assertEquals(1800, token.expiresInSeconds());
    assertEquals(id.toString(), jwt.getSubject());
    assertEquals("ADMIN", jwt.getClaimAsString(JwtTokenIssuer.ROLE_CLAIM));
    assertEquals(Duration.ofMinutes(30), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
  }
}
