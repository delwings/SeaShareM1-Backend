package com.seashare.seasharem1.unit.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.seashare.seasharem1.infrastructure.adapter.in.web.ApiProblems;
import com.seashare.seasharem1.infrastructure.adapter.in.web.CorrelationIdFilter;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void keepsAValidIncomingId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "mi-id-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seen = new AtomicReference<>();

    filter.doFilter(
        request, response, (req, res) -> seen.set(MDC.get(ApiProblems.CORRELATION_ID_KEY)));

    assertEquals("mi-id-123", seen.get());
    assertEquals("mi-id-123", response.getHeader(CorrelationIdFilter.HEADER));
    assertNull(MDC.get(ApiProblems.CORRELATION_ID_KEY));
  }

  @Test
  void replacesAnInvalidIncomingIdWithAGeneratedOne() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "id con espacios y <script>");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    String id = response.getHeader(CorrelationIdFilter.HEADER);
    assertNotNull(id);
    assertNotEquals("id con espacios y <script>", id);
  }

  @Test
  void generatesAnIdWhenNoneArrives() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {});

    assertNotNull(response.getHeader(CorrelationIdFilter.HEADER));
  }
}
