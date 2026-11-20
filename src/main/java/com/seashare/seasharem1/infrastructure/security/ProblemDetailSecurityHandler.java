package com.seashare.seasharem1.infrastructure.security;

import com.seashare.seasharem1.infrastructure.adapter.in.web.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Responde los 401 (sin token, token vencido o inválido) y 403 (rol sin permiso) que Spring
 * Security genera antes de llegar a los controladores, con el mismo formato RFC 9457 que el resto
 * de errores. Los textos son fijos, por eso el JSON se arma sin escapes.
 */
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer");
    write(response, HttpStatus.UNAUTHORIZED, "Autenticación requerida o token inválido.");
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(response, HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
  }

  private static void write(HttpServletResponse response, HttpStatus status, String detail)
      throws IOException {
    var problem = ApiProblems.of(status, detail);
    String json =
        "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"correlation_id\":\"%s\"}"
            .formatted(
                problem.getTitle(),
                status.value(),
                detail,
                problem.getProperties().get(ApiProblems.CORRELATION_ID_KEY));
    response.setStatus(status.value());
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(json);
  }
}
