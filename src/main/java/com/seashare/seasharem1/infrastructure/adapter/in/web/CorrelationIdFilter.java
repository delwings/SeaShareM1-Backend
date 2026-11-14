package com.seashare.seasharem1.infrastructure.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asigna un correlation_id a cada petición: usa el que llega en la cabecera X-Correlation-Id si es
 * válido, o genera uno. Queda en el MDC (para los logs y los errores) y se devuelve en la
 * respuesta. Corre antes que Spring Security para que también los 401 y 403 lo lleven.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-Id";
  private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String incoming = request.getHeader(HEADER);
    String id = incoming != null && VALID.matcher(incoming).matches() ? incoming : newId();
    MDC.put(ApiProblems.CORRELATION_ID_KEY, id);
    response.setHeader(HEADER, id);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(ApiProblems.CORRELATION_ID_KEY);
    }
  }

  private static String newId() {
    return UUID.randomUUID().toString();
  }
}
