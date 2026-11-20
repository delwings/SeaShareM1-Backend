package com.seashare.seasharem1.application.port.out;

import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: consulta de usuarios. */
public interface UserRepositoryPort {

  Optional<User> findByDocumentNumber(long documentNumber);

  Optional<User> findById(UUID id);

  /** Todos los usuarios con ese rol. */
  List<User> findAllByRole(UserRole role);

  /** Usuarios con esos identificadores; los que no existen se omiten. */
  List<User> findByIds(Collection<UUID> ids);
}
