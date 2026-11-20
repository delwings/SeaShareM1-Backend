package com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper;

import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Convierte entre el modelo de dominio User y la entidad JPA. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserPersistenceMapper {

  User toDomain(UserEntity entity);

  UserEntity toEntity(User user);
}
