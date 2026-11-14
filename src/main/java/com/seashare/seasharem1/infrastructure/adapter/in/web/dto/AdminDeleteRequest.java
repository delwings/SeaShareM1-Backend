package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo de la eliminación por el administrador. */
public record AdminDeleteRequest(@JsonProperty("reason") String reason) {}
