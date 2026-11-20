package com.seashare.seasharem1.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.infrastructure.security.ProblemDetailSecurityHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class ProblemDetailSecurityHandlerTest {

  private final ProblemDetailSecurityHandler handler = new ProblemDetailSecurityHandler();

  @Test
  void missingOrInvalidTokenGives401ProblemJson() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.commence(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

    assertEquals(401, response.getStatus());
    assertTrue(response.getContentType().startsWith("application/problem+json"));
    assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
    String body = response.getContentAsString();
    assertTrue(body.contains("\"status\":401"));
    assertTrue(body.contains("\"title\":\"No autorizado\""));
    assertTrue(body.contains("\"correlation_id\":\""));
  }

  @Test
  void wrongRoleGives403ProblemJson() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("x"));

    assertEquals(403, response.getStatus());
    String body = response.getContentAsString();
    assertTrue(body.contains("\"status\":403"));
    assertTrue(body.contains("\"title\":\"Prohibido\""));
  }
}
