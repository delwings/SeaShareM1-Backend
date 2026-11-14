package com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository;

import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.PortEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data de puertos. Solo lo usa el adaptador de persistencia. */
public interface PortJpaRepository extends JpaRepository<PortEntity, UUID> {

  /** La comparación usa la colación de MySQL, que no distingue mayúsculas ni tildes. */
  Optional<PortEntity> findByName(String name);
}
