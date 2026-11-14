package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.GetOwnerUseCase;
import com.seashare.seasharem1.application.port.in.OwnerSearchResult;
import com.seashare.seasharem1.application.port.in.SearchOwnersUseCase;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Consulta de propietarios del Administrador: búsqueda por nombre o cédula e información. */
public class AdminOwnersService implements SearchOwnersUseCase, GetOwnerUseCase {

  static final String NOT_FOUND = "Propietario no encontrado.";
  static final String TOO_SHORT = "Escribe al menos 3 caracteres";
  static final String TOO_LONG = "La búsqueda no puede superar 100 caracteres";

  private final UserRepositoryPort users;

  public AdminOwnersService(UserRepositoryPort users) {
    this.users = users;
  }

  @Override
  public OwnerSearchResult search(String query) {
    String text = query == null ? "" : query.trim().replaceAll("\\s+", " ");
    if (text.length() < MIN_QUERY_LENGTH) {
      throw new ValidationFailedException(List.of(new FieldViolation("q", TOO_SHORT)));
    }
    if (text.length() > MAX_QUERY_LENGTH) {
      throw new ValidationFailedException(List.of(new FieldViolation("q", TOO_LONG)));
    }
    String name = normalize(text);
    String digits = text.replaceAll("\\D", "");
    List<User> matches =
        users.findAllByRole(UserRole.OWNER).stream()
            .filter(owner -> matches(owner, name, digits))
            .sorted(
                Comparator.comparing(User::fullName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingLong(User::documentNumber))
            .toList();
    boolean hasMore = matches.size() > MAX_RESULTS;
    return new OwnerSearchResult(hasMore ? matches.subList(0, MAX_RESULTS) : matches, hasMore);
  }

  @Override
  public User getOwner(UUID ownerId) {
    return users
        .findById(ownerId)
        .filter(user -> user.role() == UserRole.OWNER)
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }

  /** Coincide por parte del nombre, o por parte de la cédula si el texto trae dígitos. */
  private static boolean matches(User owner, String name, String digits) {
    return normalize(owner.fullName()).contains(name)
        || (!digits.isEmpty() && String.valueOf(owner.documentNumber()).contains(digits));
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
