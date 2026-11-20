package com.seashare.seasharem1.infrastructure.adapter.out.persistence;

import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador que implementa el puerto de usuarios con Spring Data JPA. */
@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

  private final UserJpaRepository repository;
  private final UserPersistenceMapper mapper;

  public UserPersistenceAdapter(UserJpaRepository repository, UserPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<User> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<User> findAllByRole(UserRole role) {
    return repository.findByRole(role).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<User> findByIds(Collection<UUID> ids) {
    return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<User> findByDocumentNumber(long documentNumber) {
    return repository.findByDocumentNumber(documentNumber).map(mapper::toDomain);
  }
}
