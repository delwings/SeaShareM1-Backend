package com.seashare.seasharem1.application.port.in;

/** Búsqueda de propietarios por nombre o cédula, para el Administrador. */
public interface SearchOwnersUseCase {

  int MIN_QUERY_LENGTH = 3;
  int MAX_QUERY_LENGTH = 100;
  int MAX_RESULTS = 5;

  /**
   * Busca entre todos los propietarios (tengan o no embarcaciones). Coincide con cualquier parte
   * del nombre (sin tildes ni mayúsculas) o de la cédula (ignorando puntos, comas y espacios).
   *
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException el texto tiene menos
   *     de {@value #MIN_QUERY_LENGTH} o más de {@value #MAX_QUERY_LENGTH} caracteres
   */
  OwnerSearchResult search(String query);
}
