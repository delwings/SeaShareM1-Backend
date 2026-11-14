package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException;
import com.seashare.seasharem1.domain.exception.BoatNotDeletableException;
import com.seashare.seasharem1.domain.exception.BoatNotEditableException;
import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.InvalidCredentialsException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte toda excepción en un error RFC 9457 (Problem Details) siguiendo el catálogo común de
 * errores del plan general: 400, 401, 403, 404, 409, 502 y 500. Cada error lleva correlation_id y,
 * en el 400 de validación, la lista errors con campo y mensaje.
 *
 * <p>Extiende ResponseEntityExceptionHandler, que ya traduce las excepciones estándar de Spring MVC
 * (JSON mal formado, tipo de parámetro incorrecto, ruta inexistente, etc.); aquí solo se les añade
 * el título y el correlation_id.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(InvalidCredentialsException.class)
  public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
    return ApiProblems.of(HttpStatus.UNAUTHORIZED, "Número de documento o contraseña incorrectos.");
  }

  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthentication(AuthenticationException exception) {
    return ApiProblems.of(HttpStatus.UNAUTHORIZED, "Autenticación requerida o token inválido.");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
    return ApiProblems.of(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
    return ApiProblems.of(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(ValidationFailedException.class)
  public ProblemDetail handleValidationFailed(ValidationFailedException exception) {
    ProblemDetail problem =
        ApiProblems.of(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos.");
    problem.setProperty("errors", toErrors(exception.violations()));
    return problem;
  }

  @ExceptionHandler(BusinessConflictException.class)
  public ProblemDetail handleConflict(BusinessConflictException exception) {
    ProblemDetail problem = ApiProblems.of(HttpStatus.CONFLICT, exception.getMessage());
    if (!exception.violations().isEmpty()) {
      problem.setProperty("errors", toErrors(exception.violations()));
    }
    if (exception instanceof BoatNotDeletableException notDeletable) {
      problem.setProperty("current_status", notDeletable.currentStatus().name());
    }
    if (exception instanceof BoatMaintenanceConflictException maintenance) {
      problem.setProperty("current_status", maintenance.currentStatus().name());
      if (maintenance.maintenanceBy() != null) {
        problem.setProperty("maintenance_by", maintenance.maintenanceBy().name());
      }
    }
    if (exception instanceof BoatNotEditableException notEditable) {
      problem.setProperty("current_status", notEditable.currentStatus().name());
    }
    return problem;
  }

  @ExceptionHandler(ExternalServiceUnavailableException.class)
  public ProblemDetail handleExternalService(ExternalServiceUnavailableException exception) {
    LOG.error("Falló un servicio externo", exception);
    return ApiProblems.of(
        HttpStatus.BAD_GATEWAY,
        "Un servicio externo no respondió. Intenta de nuevo en unos minutos.");
  }

  /** Último recurso: el detalle real solo va al log, nunca al cliente. */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception exception) {
    if (exception instanceof MaxUploadSizeExceededException) {
      return photoTooLarge();
    }
    ProblemDetail problem =
        ApiProblems.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocurrió un error inesperado. Si persiste, informa el correlation_id.");
    LOG.error(
        "Error inesperado [correlation_id={}]",
        problem.getProperties().get("correlation_id"),
        exception);
    return problem;
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Map<String, String>> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    Map.of(
                        "field",
                        ApiProblems.toSnakeCase(error.getField()),
                        "message",
                        error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Valor inválido"))
            .toList();
    ProblemDetail problem = exception.getBody();
    problem.setDetail("La solicitud contiene datos inválidos.");
    problem.setProperty("errors", errors);
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  /**
   * Punto único por el que pasan los errores estándar de Spring MVC: aquí se les da la forma común.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    if (exception instanceof MaxUploadSizeExceededException) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(photoTooLarge());
    }
    ResponseEntity<Object> response =
        super.handleExceptionInternal(exception, body, headers, statusCode, request);
    if (response != null && response.getBody() instanceof ProblemDetail problem) {
      ApiProblems.decorate(problem);
    }
    return response;
  }

  /** Una foto de más de 10 MB es un 400 del catálogo (no un 413), con el mensaje de la spec. */
  private static ProblemDetail photoTooLarge() {
    ProblemDetail problem =
        ApiProblems.of(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos.");
    problem.setProperty(
        "errors",
        toErrors(List.of(new FieldViolation("photo", RegistrationRules.IMAGE_TOO_LARGE))));
    return problem;
  }

  private static List<Map<String, String>> toErrors(List<FieldViolation> violations) {
    return violations.stream()
        .map(violation -> Map.of("field", violation.field(), "message", violation.message()))
        .toList();
  }
}
