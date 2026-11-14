package com.seashare.seasharem1.unit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.InvalidCredentialsException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.infrastructure.adapter.in.web.ApiProblems;
import com.seashare.seasharem1.infrastructure.adapter.in.web.GlobalExceptionHandler;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.LoginRequest;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void invalidCredentialsIsGenericUnauthorized() {
    ProblemDetail problem = handler.handleInvalidCredentials(new InvalidCredentialsException());

    assertEquals(401, problem.getStatus());
    assertEquals("No autorizado", problem.getTitle());
    assertEquals("Número de documento o contraseña incorrectos.", problem.getDetail());
    assertNotNull(problem.getProperties().get("correlation_id"));
  }

  @Test
  void accessDeniedIsForbidden() {
    ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("x"));

    assertEquals(403, problem.getStatus());
    assertEquals("Prohibido", problem.getTitle());
  }

  @Test
  void notFoundKeepsTheMessage() {
    ProblemDetail problem =
        handler.handleNotFound(new ResourceNotFoundException("La embarcación no existe."));

    assertEquals(404, problem.getStatus());
    assertEquals("La embarcación no existe.", problem.getDetail());
  }

  @Test
  void validationFailureListsEachField() {
    ProblemDetail problem =
        handler.handleValidationFailed(
            new ValidationFailedException(List.of(new FieldViolation("name", "Obligatorio"))));

    assertEquals(400, problem.getStatus());
    assertEquals(
        List.of(Map.of("field", "name", "message", "Obligatorio")),
        problem.getProperties().get("errors"));
  }

  @Test
  void conflictWithViolationsListsThem() {
    ProblemDetail problem =
        handler.handleConflict(
            new BusinessConflictException(
                "Conflicto", List.of(new FieldViolation("legal_registration", "Existente"))));

    assertEquals(409, problem.getStatus());
    assertEquals(
        List.of(Map.of("field", "legal_registration", "message", "Existente")),
        problem.getProperties().get("errors"));
  }

  @Test
  void conflictWithoutViolationsHasNoErrorsList() {
    ProblemDetail problem = handler.handleConflict(new BusinessConflictException("Ya existe."));

    assertFalse(problem.getProperties().containsKey("errors"));
  }

  @Test
  void tooLargeUploadIsAPhotoValidationError() {
    ProblemDetail problem = handler.handleUnexpected(new MaxUploadSizeExceededException(10));

    assertEquals(400, problem.getStatus());
    assertEquals(
        List.of(Map.of("field", "photo", "message", "El tamaño de imagen se excede")),
        problem.getProperties().get("errors"));
  }

  @Test
  void conflictKeepsTheMessage() {
    ProblemDetail problem =
        handler.handleConflict(new BusinessConflictException("Ya tienes un borrador."));

    assertEquals(409, problem.getStatus());
    assertEquals("Conflicto", problem.getTitle());
    assertEquals("Ya tienes un borrador.", problem.getDetail());
  }

  @Test
  void externalServiceFailureIsBadGatewayWithoutLeakingTheCause() {
    ProblemDetail problem =
        handler.handleExternalService(
            new ExternalServiceUnavailableException("Cloudinary caído", new RuntimeException("x")));

    assertEquals(502, problem.getStatus());
    assertFalse(problem.getDetail().contains("Cloudinary"));
  }

  @Test
  void unexpectedErrorHidesTheRealMessage() {
    ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("secreto interno"));

    assertEquals(500, problem.getStatus());
    assertEquals("Error interno", problem.getTitle());
    assertFalse(problem.getDetail().contains("secreto"));
  }

  @Test
  void correlationIdComesFromTheMdcWhenPresent() {
    MDC.put(ApiProblems.CORRELATION_ID_KEY, "abc-123");

    ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("x"));

    assertEquals("abc-123", problem.getProperties().get("correlation_id"));
  }

  @Test
  void validationErrorListsEachFieldInSnakeCase() throws Exception {
    LoginRequest target = new LoginRequest("abc", "");
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(target, "loginRequest");
    new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator())
        .validate(target, result);
    MethodParameter parameter =
        new MethodParameter(
            GlobalExceptionHandlerTest.class.getDeclaredMethod("endpoint", LoginRequest.class), 0);

    ResponseEntity<Object> response =
        handler.handleException(
            new MethodArgumentNotValidException(parameter, result),
            new ServletWebRequest(new MockHttpServletRequest()));

    ProblemDetail problem = (ProblemDetail) response.getBody();
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Solicitud inválida", problem.getTitle());
    @SuppressWarnings("unchecked")
    List<Map<String, String>> errors =
        (List<Map<String, String>>) problem.getProperties().get("errors");
    Set<String> fields = errors.stream().map(e -> e.get("field")).collect(Collectors.toSet());
    assertEquals(Set.of("document_number", "password"), fields);
    assertTrue(errors.stream().allMatch(e -> !e.get("message").isBlank()));
  }

  @Test
  void snakeCaseConversion() {
    assertEquals("document_number", ApiProblems.toSnakeCase("documentNumber"));
    assertEquals("items[0].port_name", ApiProblems.toSnakeCase("items[0].portName"));
  }

  @SuppressWarnings("unused")
  private void endpoint(LoginRequest request) {}
}
