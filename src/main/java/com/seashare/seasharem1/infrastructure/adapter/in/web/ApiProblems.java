package com.seashare.seasharem1.infrastructure.adapter.in.web;

import java.util.Locale;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Construye los errores RFC 9457 del catálogo común: título en español y la extensión
 * correlation_id. Lo usan el manejador global y los manejadores de Spring Security, para que todos
 * los errores tengan la misma forma.
 */
public final class ApiProblems {

  /** Nombre de la clave en el MDC del log y del campo en el cuerpo del error. */
  public static final String CORRELATION_ID_KEY = "correlation_id";

  private ApiProblems() {}

  public static ProblemDetail of(HttpStatus status, String detail) {
    return decorate(ProblemDetail.forStatusAndDetail(status, detail));
  }

  /** Pone el título del catálogo y el correlation_id a un error ya creado. */
  public static ProblemDetail decorate(ProblemDetail problem) {
    String title = titleFor(problem.getStatus());
    if (title != null) {
      problem.setTitle(title);
    }
    problem.setProperty(CORRELATION_ID_KEY, currentCorrelationId());
    return problem;
  }

  public static String currentCorrelationId() {
    String id = MDC.get(CORRELATION_ID_KEY);
    return id != null ? id : UUID.randomUUID().toString();
  }

  /** documentNumber -> document_number; también en rutas como items[0].portName. */
  public static String toSnakeCase(String name) {
    return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
  }

  private static String titleFor(int status) {
    return switch (status) {
      case 400 -> "Solicitud inválida";
      case 401 -> "No autorizado";
      case 403 -> "Prohibido";
      case 404 -> "No encontrado";
      case 405 -> "Método no permitido";
      case 409 -> "Conflicto";
      case 413 -> "Archivo demasiado grande";
      case 415 -> "Tipo de contenido no soportado";
      case 502 -> "Error en un servicio externo";
      case 500 -> "Error interno";
      default -> null;
    };
  }
}
