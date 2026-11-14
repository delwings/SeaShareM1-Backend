package com.seashare.seasharem1.domain.rules;

/**
 * Resultado de interpretar un texto: el valor, o el mensaje de error en español.
 *
 * @param <T> tipo del valor
 */
public record Parsed<T>(T value, String error) {

  public static <T> Parsed<T> of(T value) {
    return new Parsed<>(value, null);
  }

  public static <T> Parsed<T> error(String message) {
    return new Parsed<>(null, message);
  }

  public boolean isError() {
    return error != null;
  }
}
