package com.seashare.seasharem1.unit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.seashare.seasharem1.infrastructure.adapter.in.web.RequestLoggingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void attach() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detach() {
    logger.detachAppender(appender);
  }

  @Test
  void logsMethodPathAndStatusWithoutBodyOrQuery() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
    request.setQueryString("password=secreto");
    request.setContent("{\"password\":\"secreto\"}".getBytes());
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestLoggingFilter().doFilter(request, response, (req, res) -> response.setStatus(401));

    assertEquals(1, appender.list.size());
    String message = appender.list.get(0).getFormattedMessage();
    assertTrue(message.startsWith("POST /api/v1/auth/login -> 401 ("));
    assertFalse(message.contains("secreto"));
  }
}
