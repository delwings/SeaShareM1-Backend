package com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper;

import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.PortEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Convierte entre el modelo de dominio Port y la entidad JPA. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PortPersistenceMapper {

  Port toDomain(PortEntity entity);

  PortEntity toEntity(Port port);
}
