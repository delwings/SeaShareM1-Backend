package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.OwnerSearchResult;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.usecase.AdminOwnersService;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminOwnersServiceTest {

  private final List<User> all = new ArrayList<>();
  private AdminOwnersService service;

  private User add(String name, long document, UserRole role) {
    User user = new User(UUID.randomUUID(), name, document, name + "@t", "3100000000", "x", role);
    all.add(user);
    return user;
  }

  @BeforeEach
  void setUp() {
    UserRepositoryPort users =
        new UserRepositoryPort() {
          @Override
          public Optional<User> findByDocumentNumber(long documentNumber) {
            return all.stream().filter(u -> u.documentNumber() == documentNumber).findFirst();
          }

          @Override
          public Optional<User> findById(UUID id) {
            return all.stream().filter(u -> u.id().equals(id)).findFirst();
          }

          @Override
          public List<User> findAllByRole(UserRole role) {
            return all.stream().filter(u -> u.role() == role).toList();
          }

          @Override
          public List<User> findByIds(Collection<UUID> ids) {
            return all.stream().filter(u -> ids.contains(u.id())).toList();
          }
        };
    service = new AdminOwnersService(users);
    add("Laura Gómez Restrepo", 1010000001L, UserRole.ADMIN);
    add("Fernando Fernández", 1042389104L, UserRole.OWNER);
    add("Mariana Gómez", 1010000003L, UserRole.OWNER);
  }

  private List<String> names(String query) {
    return service.search(query).owners().stream().map(User::fullName).toList();
  }

  @Test
  void searchesByPartialNameWithoutAccentsOrCase() {
    assertEquals(List.of("Fernando Fernández"), names("fernan"));
    assertEquals(List.of("Fernando Fernández"), names("FERNANDEZ"));
  }

  @Test
  void searchesByDocumentIgnoringDotsCommasAndSpaces() {
    assertEquals(List.of("Fernando Fernández"), names("1042389104"));
    assertEquals(List.of("Fernando Fernández"), names("1.042.389.104"));
    assertEquals(List.of("Fernando Fernández"), names("1,042 389"));
    assertEquals(List.of("Fernando Fernández"), names("  389104  "));
  }

  @Test
  void adminsAreNeverFound() {
    assertTrue(names("Laura").isEmpty());
    assertTrue(names("1010000001").isEmpty());
  }

  @Test
  void ownersWithoutBoatsAreFoundToo() {
    assertEquals(List.of("Mariana Gómez"), names("mariana"));
  }

  @Test
  void showsBothWhenTheNameIsRepeated() {
    add("Fernando Fernández", 1099999999L, UserRole.OWNER);

    OwnerSearchResult result = service.search("fernando");

    assertEquals(2, result.owners().size());
    assertEquals(1042389104L, result.owners().get(0).documentNumber());
    assertFalse(result.hasMore());
  }

  @Test
  void returnsFiveSortedAndFlagsThereAreMore() {
    for (char c = 'A'; c <= 'G'; c++) {
      add("Pedro " + c, 2000L + c, UserRole.OWNER);
    }

    OwnerSearchResult result = service.search("pedro");

    assertEquals(5, result.owners().size());
    assertTrue(result.hasMore());
    assertEquals("Pedro A", result.owners().get(0).fullName());
    assertEquals("Pedro E", result.owners().get(4).fullName());
  }

  @Test
  void exactlyFiveIsNotMore() {
    for (char c = 'A'; c <= 'E'; c++) {
      add("Pedro " + c, 2000L + c, UserRole.OWNER);
    }
    assertFalse(service.search("pedro").hasMore());
  }

  @Test
  void noMatchesGivesAnEmptyResult() {
    OwnerSearchResult result = service.search("zzzz");
    assertTrue(result.owners().isEmpty());
    assertFalse(result.hasMore());
  }

  @Test
  void shortOrLongOrMissingTextIsRejected() {
    ValidationFailedException shortText =
        assertThrows(ValidationFailedException.class, () -> service.search("fe"));
    assertEquals("q", shortText.violations().get(0).field());
    assertThrows(ValidationFailedException.class, () -> service.search("  f  "));
    assertThrows(ValidationFailedException.class, () -> service.search(null));
    assertThrows(ValidationFailedException.class, () -> service.search("a".repeat(101)));
  }

  @Test
  void getsAnOwnerButNotAnAdminOrUnknown() {
    User owner = all.get(1);

    assertEquals(owner, service.getOwner(owner.id()));
    assertThrows(ResourceNotFoundException.class, () -> service.getOwner(all.get(0).id()));
    assertThrows(ResourceNotFoundException.class, () -> service.getOwner(UUID.randomUUID()));
  }
}
