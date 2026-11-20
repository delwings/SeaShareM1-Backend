package com.seashare.seasharem1.unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

class DomainExceptionsTest {

  @Test
  void exceptionsCarryTheirMessage() {
    assertEquals("a", new ResourceNotFoundException("a").getMessage());
    assertEquals("b", new BusinessConflictException("b").getMessage());
  }

  @Test
  void externalServiceExceptionKeepsTheCause() {
    Throwable cause = new RuntimeException("causa");
    assertSame(cause, new ExternalServiceUnavailableException("c", cause).getCause());
  }
}
