package com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper;

import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Convierte entre el modelo de dominio Notification y la entidad JPA. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationPersistenceMapper {

  Notification toDomain(NotificationEntity entity);

  NotificationEntity toEntity(Notification notification);
}
