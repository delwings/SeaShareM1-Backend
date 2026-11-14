package com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper;

import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.VesselEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Convierte entre el modelo de dominio Vessel y la entidad JPA. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VesselPersistenceMapper {

  Vessel toDomain(VesselEntity entity);

  VesselEntity toEntity(Vessel vessel);
}
