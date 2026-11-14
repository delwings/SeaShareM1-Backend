package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.BoatListItem;
import com.seashare.seasharem1.application.port.in.ListOwnerBoatsUseCase;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.model.Vessel;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Lista las embarcaciones registradas del propietario, de la más reciente a la más antigua según
 * cuándo se completó su registro. Solo la primera lleva la etiqueta NUEVA, y solo durante 168
 * horas.
 */
public class ListOwnerBoatsService implements ListOwnerBoatsUseCase {

  static final Duration NEW_TAG_DURATION = Duration.ofHours(168);

  private final VesselRepositoryPort vessels;
  private final Clock clock;

  public ListOwnerBoatsService(VesselRepositoryPort vessels, Clock clock) {
    this.vessels = vessels;
    this.clock = clock;
  }

  @Override
  public List<BoatListItem> list(UUID ownerId) {
    List<Vessel> sorted =
        vessels.findRegisteredByOwner(ownerId).stream()
            .sorted(
                Comparator.comparing(
                    Vessel::registeredAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    LocalDateTime now = LocalDateTime.now(clock);
    return IntStream.range(0, sorted.size())
        .mapToObj(
            index ->
                new BoatListItem(sorted.get(index), index == 0 && isNew(sorted.get(index), now)))
        .toList();
  }

  private static boolean isNew(Vessel vessel, LocalDateTime now) {
    return vessel.registeredAt() != null
        && Duration.between(vessel.registeredAt(), now).compareTo(NEW_TAG_DURATION) < 0;
  }
}
