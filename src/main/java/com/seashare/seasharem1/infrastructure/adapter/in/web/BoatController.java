package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.CheckLegalRegistrationUseCase;
import com.seashare.seasharem1.application.port.in.CompleteRegistrationCommand;
import com.seashare.seasharem1.application.port.in.CompleteRegistrationUseCase;
import com.seashare.seasharem1.application.port.in.CreateDraftCommand;
import com.seashare.seasharem1.application.port.in.CreateDraftUseCase;
import com.seashare.seasharem1.application.port.in.DeleteBoatUseCase;
import com.seashare.seasharem1.application.port.in.DeleteDraftUseCase;
import com.seashare.seasharem1.application.port.in.EditBoatCommand;
import com.seashare.seasharem1.application.port.in.EditBoatUseCase;
import com.seashare.seasharem1.application.port.in.FinishMaintenanceUseCase;
import com.seashare.seasharem1.application.port.in.GetBoatDetailUseCase;
import com.seashare.seasharem1.application.port.in.GetDraftUseCase;
import com.seashare.seasharem1.application.port.in.ListOwnerBoatsUseCase;
import com.seashare.seasharem1.application.port.in.RequestMaintenanceReviewUseCase;
import com.seashare.seasharem1.application.port.in.SendToMaintenanceUseCase;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.AvailabilityResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BoatDraftResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BoatListResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.CompletedBoatResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.CreateDraftRequest;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.MaintenanceRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Registro de embarcaciones del propietario: lista, borrador (paso 1) y registro completo (paso 2).
 * Solo el rol OWNER entra, y cada propietario opera únicamente sobre sus propias embarcaciones.
 */
@RestController
@RequestMapping("/api/v1/fleet/boats")
@PreAuthorize("hasRole('OWNER')")
public class BoatController {

  private final ListOwnerBoatsUseCase listBoats;
  private final CheckLegalRegistrationUseCase checkLegalRegistration;
  private final GetBoatDetailUseCase getBoatDetail;
  private final GetDraftUseCase getDraft;
  private final CreateDraftUseCase createDraft;
  private final DeleteDraftUseCase deleteDraft;
  private final DeleteBoatUseCase deleteBoat;
  private final CompleteRegistrationUseCase completeRegistration;
  private final EditBoatUseCase editBoat;
  private final SendToMaintenanceUseCase sendToMaintenance;
  private final FinishMaintenanceUseCase finishMaintenance;
  private final RequestMaintenanceReviewUseCase requestReview;

  public BoatController(
      ListOwnerBoatsUseCase listBoats,
      CheckLegalRegistrationUseCase checkLegalRegistration,
      GetBoatDetailUseCase getBoatDetail,
      GetDraftUseCase getDraft,
      CreateDraftUseCase createDraft,
      DeleteDraftUseCase deleteDraft,
      DeleteBoatUseCase deleteBoat,
      CompleteRegistrationUseCase completeRegistration,
      EditBoatUseCase editBoat,
      SendToMaintenanceUseCase sendToMaintenance,
      FinishMaintenanceUseCase finishMaintenance,
      RequestMaintenanceReviewUseCase requestReview) {
    this.listBoats = listBoats;
    this.checkLegalRegistration = checkLegalRegistration;
    this.getBoatDetail = getBoatDetail;
    this.getDraft = getDraft;
    this.createDraft = createDraft;
    this.deleteDraft = deleteDraft;
    this.deleteBoat = deleteBoat;
    this.completeRegistration = completeRegistration;
    this.editBoat = editBoat;
    this.sendToMaintenance = sendToMaintenance;
    this.finishMaintenance = finishMaintenance;
    this.requestReview = requestReview;
  }

  /** Lista "Mis Embarcaciones Registradas": sin borradores, la más reciente primero. */
  @GetMapping
  public BoatListResponse list(@AuthenticationPrincipal Jwt jwt) {
    return new BoatListResponse(
        listBoats.list(ownerId(jwt)).stream().map(BoatWebMapper::toListItem).toList());
  }

  /** Verificación en tiempo real de la matrícula legal. */
  @GetMapping("/legal-registration-availability")
  public AvailabilityResponse legalRegistrationAvailability(
      @RequestParam(value = "value", required = false) String value) {
    return new AvailabilityResponse(checkLegalRegistration.isAvailable(value));
  }

  /** Detalles de una embarcación registrada; 404 si no existe, es ajena, eliminada o borrador. */
  @GetMapping("/{boatId}")
  public CompletedBoatResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID boatId) {
    return BoatWebMapper.toCompleted(getBoatDetail.getDetail(ownerId(jwt), boatId));
  }

  /**
   * Eliminación lógica; 404 si no existe, es ajena, ya fue eliminada o es borrador; 409 por estado.
   */
  @DeleteMapping("/{boatId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID boatId) {
    deleteBoat.deleteBoat(ownerId(jwt), boatId);
  }

  /** Envía la embarcación a mantenimiento (sin motivo); la fecha de fin es obligatoria. */
  @PostMapping("/{boatId}/maintenance/start")
  public CompletedBoatResponse startMaintenance(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID boatId,
      @RequestBody(required = false) MaintenanceRequest request) {
    return BoatWebMapper.toCompleted(
        sendToMaintenance.sendToMaintenance(
            ownerId(jwt),
            boatId,
            MaintenanceDates.parse(
                "maintenance_start", request == null ? null : request.maintenanceStart()),
            MaintenanceDates.parse(
                "maintenance_until", request == null ? null : request.maintenanceUntil())));
  }

  /** Termina el mantenimiento y deja la embarcación Disponible. */
  @PostMapping("/{boatId}/maintenance/finish")
  public CompletedBoatResponse finishMaintenance(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID boatId) {
    return BoatWebMapper.toCompleted(finishMaintenance.finishMaintenance(ownerId(jwt), boatId));
  }

  /** Pide al administrador que revise una embarcación que él envió a mantenimiento. */
  @PostMapping("/{boatId}/maintenance/request-review")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void requestReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID boatId) {
    requestReview.requestReview(ownerId(jwt), boatId);
  }

  /** Borrador del propietario; 404 si no tiene. */
  @GetMapping("/draft")
  public BoatDraftResponse getDraft(@AuthenticationPrincipal Jwt jwt) {
    return BoatWebMapper.toDraft(getDraft.getDraft(ownerId(jwt)));
  }

  /** Paso 1: guarda los datos básicos y deja la embarcación en borrador. */
  @PostMapping("/draft")
  @ResponseStatus(HttpStatus.CREATED)
  public BoatDraftResponse createDraft(
      @AuthenticationPrincipal Jwt jwt, @RequestBody CreateDraftRequest request) {
    return BoatWebMapper.toDraft(
        createDraft.createDraft(
            new CreateDraftCommand(
                ownerId(jwt),
                request.name(),
                request.legalRegistration(),
                request.type(),
                request.maxCapacity())));
  }

  /** Elimina el borrador de forma permanente. */
  @DeleteMapping("/draft")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDraft(@AuthenticationPrincipal Jwt jwt) {
    deleteDraft.deleteDraft(ownerId(jwt));
  }

  /**
   * Paso 2: completa el registro. Llega como formulario con la foto; todos los campos son
   * opcionales a este nivel porque las reglas de negocio devuelven los mensajes exactos de cada
   * campo.
   */
  @PostMapping(path = "/{boatId}/registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CompletedBoatResponse completeRegistration(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID boatId,
      @RequestParam(value = "port_name", required = false) String portName,
      @RequestParam(value = "latitude", required = false) String latitude,
      @RequestParam(value = "longitude", required = false) String longitude,
      @RequestParam(value = "base_rate", required = false) String baseRate,
      @RequestParam(value = "life_jackets", defaultValue = "false") boolean lifeJackets,
      @RequestParam(value = "cooler_with_ice", defaultValue = "false") boolean coolerWithIce,
      @RequestParam(value = "sound_system", defaultValue = "false") boolean soundSystem,
      @RequestParam(value = "fishing_gear", defaultValue = "false") boolean fishingGear,
      @RequestParam(value = "diving_gear", defaultValue = "false") boolean divingGear,
      @RequestParam(value = "photo", required = false) MultipartFile photo) {
    return BoatWebMapper.toCompleted(
        completeRegistration.complete(
            new CompleteRegistrationCommand(
                ownerId(jwt),
                boatId,
                portName,
                latitude,
                longitude,
                baseRate,
                new AdditionalServices(
                    lifeJackets, coolerWithIce, soundSystem, fishingGear, divingGear),
                bytesOf(photo))));
  }

  /**
   * Edición de una embarcación registrada. Llega como formulario porque la foto es opcional (si no
   * se envía, se conserva la actual). Los cinco servicios adicionales van juntos: si no se envía
   * ninguno, se conservan los guardados. 404 si no existe o no es del propietario; 409 si el estado
   * no permite editar.
   */
  @PutMapping(path = "/{boatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CompletedBoatResponse edit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID boatId,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "max_capacity", required = false) String maxCapacity,
      @RequestParam(value = "port_name", required = false) String portName,
      @RequestParam(value = "latitude", required = false) String latitude,
      @RequestParam(value = "longitude", required = false) String longitude,
      @RequestParam(value = "base_rate", required = false) String baseRate,
      @RequestParam(value = "life_jackets", required = false) Boolean lifeJackets,
      @RequestParam(value = "cooler_with_ice", required = false) Boolean coolerWithIce,
      @RequestParam(value = "sound_system", required = false) Boolean soundSystem,
      @RequestParam(value = "fishing_gear", required = false) Boolean fishingGear,
      @RequestParam(value = "diving_gear", required = false) Boolean divingGear,
      @RequestParam(value = "photo", required = false) MultipartFile photo) {
    boolean servicesSent =
        lifeJackets != null
            || coolerWithIce != null
            || soundSystem != null
            || fishingGear != null
            || divingGear != null;
    AdditionalServices services =
        servicesSent
            ? new AdditionalServices(
                Boolean.TRUE.equals(lifeJackets),
                Boolean.TRUE.equals(coolerWithIce),
                Boolean.TRUE.equals(soundSystem),
                Boolean.TRUE.equals(fishingGear),
                Boolean.TRUE.equals(divingGear))
            : null;
    return BoatWebMapper.toCompleted(
        editBoat.edit(
            new EditBoatCommand(
                ownerId(jwt),
                boatId,
                name,
                maxCapacity,
                portName,
                latitude,
                longitude,
                baseRate,
                services,
                bytesOf(photo))));
  }

  private static UUID ownerId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }

  private static byte[] bytesOf(MultipartFile photo) {
    if (photo == null || photo.isEmpty()) {
      return null;
    }
    try {
      return photo.getBytes();
    } catch (IOException exception) {
      throw new UncheckedIOException("No se pudo leer la foto enviada", exception);
    }
  }
}
