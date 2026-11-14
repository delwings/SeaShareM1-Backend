package com.seashare.seasharem1.infrastructure.adapter.out.persistence;

import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.PortPersistenceMapper;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository.PortJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Adaptador que implementa el puerto de puertos de atraque con Spring Data JPA. */
@Component
public class PortPersistenceAdapter implements PortRepositoryPort {

  private final PortJpaRepository repository;
  private final PortPersistenceMapper mapper;

  public PortPersistenceAdapter(PortJpaRepository repository, PortPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Port> findByName(String name) {
    return repository.findByName(name).map(mapper::toDomain);
  }

  @Override
  public Optional<Port> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Port save(Port port) {
    try {
      return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(port)));
    } catch (DataIntegrityViolationException exception) {
      // Otro registro creó un puerto con ese nombre justo ahora: se reutiliza ese.
      return repository.findByName(port.name()).map(mapper::toDomain).orElseThrow(() -> exception);
    }
  }
}
