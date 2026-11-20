package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.User;
import java.util.List;

/**
 * Resultado de buscar propietarios.
 *
 * @param owners hasta {@code SearchOwnersUseCase.MAX_RESULTS} propietarios, por nombre
 * @param hasMore {@code true} si había más coincidencias que las devueltas
 */
public record OwnerSearchResult(List<User> owners, boolean hasMore) {}
