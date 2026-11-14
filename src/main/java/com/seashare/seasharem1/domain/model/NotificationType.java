package com.seashare.seasharem1.domain.model;

/** Tipo de notificación. */
public enum NotificationType {
  /** Del administrador al propietario, con el motivo. */
  MAINTENANCE_NOTICE,
  /** Del propietario al administrador. */
  REVIEW_REQUEST,
  /** Automática, al propietario. */
  VESSEL_AVAILABLE,
  /** Al propietario, cuando un administrador elimina su embarcación. */
  VESSEL_DELETED
}
