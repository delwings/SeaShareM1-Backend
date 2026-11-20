package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.GetOwnerUseCase;
import com.seashare.seasharem1.application.port.in.OwnerSearchResult;
import com.seashare.seasharem1.application.port.in.SearchOwnersUseCase;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.OwnerDetailResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.OwnerResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.OwnerSearchResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de propietarios del Administrador: búsqueda por nombre o cédula e información de un
 * propietario. Solo el rol ADMIN entra; todo es de lectura.
 */
@RestController
@RequestMapping("/api/v1/admin/owners")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOwnersController {

  private final SearchOwnersUseCase searchOwners;
  private final GetOwnerUseCase getOwner;

  public AdminOwnersController(SearchOwnersUseCase searchOwners, GetOwnerUseCase getOwner) {
    this.searchOwners = searchOwners;
    this.getOwner = getOwner;
  }

  /** Busca propietarios; el texto debe tener entre 3 y 100 caracteres (si no, 400). */
  @GetMapping
  public OwnerSearchResponse search(@RequestParam(value = "q", required = false) String query) {
    OwnerSearchResult result = searchOwners.search(query);
    return new OwnerSearchResponse(
        result.owners().stream().map(AdminOwnersController::toSummary).toList(), result.hasMore());
  }

  /** Información de un propietario; 404 si no existe o el usuario no es propietario. */
  @GetMapping("/{ownerId}")
  public OwnerDetailResponse detail(@PathVariable UUID ownerId) {
    User owner = getOwner.getOwner(ownerId);
    return new OwnerDetailResponse(
        owner.id(),
        owner.fullName(),
        String.valueOf(owner.documentNumber()),
        owner.email(),
        owner.phone());
  }

  private static OwnerResponse toSummary(User owner) {
    return new OwnerResponse(owner.id(), owner.fullName(), String.valueOf(owner.documentNumber()));
  }
}
