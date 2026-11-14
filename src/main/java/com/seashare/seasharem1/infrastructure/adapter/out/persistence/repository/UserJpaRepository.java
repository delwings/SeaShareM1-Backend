package com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository;

import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data de usuarios. Solo lo usa el adaptador de persistencia. */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByDocumentNumber(long documentNumber);

  List<UserEntity> findByRole(UserRole role);
}
