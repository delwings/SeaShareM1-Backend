package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.domain.model.VesselType;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BoatTypeListResponse;
import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tipos de embarcación y su capacidad máxima, para el selector del formulario. */
@RestController
@RequestMapping("/api/v1/fleet/boat-types")
public class BoatTypeController {

  @GetMapping
  public BoatTypeListResponse list() {
    return new BoatTypeListResponse(
        Arrays.stream(VesselType.values()).map(BoatWebMapper::toType).toList());
  }
}
