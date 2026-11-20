package com.seashare.seasharem1.domain.model;

/** Estado operativo de una embarcación. DRAFT es el borrador de un registro sin terminar. */
public enum VesselStatus {
  DRAFT,
  AVAILABLE,
  RESERVED,
  MAINTENANCE,
  NAVIGATION
}
