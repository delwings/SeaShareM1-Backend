package com.seashare.seasharem1.configuration;

import com.seashare.seasharem1.application.port.in.AdminDeleteBoatUseCase;
import com.seashare.seasharem1.application.port.in.AdminMaintenanceUseCase;
import com.seashare.seasharem1.application.port.in.AssignOperationalStatusUseCase;
import com.seashare.seasharem1.application.port.in.BaseRateQuery;
import com.seashare.seasharem1.application.port.in.CheckLegalRegistrationUseCase;
import com.seashare.seasharem1.application.port.in.CompleteRegistrationUseCase;
import com.seashare.seasharem1.application.port.in.CreateDraftUseCase;
import com.seashare.seasharem1.application.port.in.DeleteBoatUseCase;
import com.seashare.seasharem1.application.port.in.DeleteDraftUseCase;
import com.seashare.seasharem1.application.port.in.EditBoatUseCase;
import com.seashare.seasharem1.application.port.in.GetBoatDetailUseCase;
import com.seashare.seasharem1.application.port.in.GetDraftUseCase;
import com.seashare.seasharem1.application.port.in.ListOwnerBoatsUseCase;
import com.seashare.seasharem1.application.port.in.LoginUseCase;
import com.seashare.seasharem1.application.port.in.RequestMaintenanceReviewUseCase;
import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery;
import com.seashare.seasharem1.application.port.out.ImageStoragePort;
import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.application.port.out.PasswordVerifierPort;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.ProcessedEventRepositoryPort;
import com.seashare.seasharem1.application.port.out.TokenIssuerPort;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.application.usecase.AdminDeleteBoatService;
import com.seashare.seasharem1.application.usecase.AdminFleetService;
import com.seashare.seasharem1.application.usecase.AdminMaintenanceService;
import com.seashare.seasharem1.application.usecase.AdminOwnersService;
import com.seashare.seasharem1.application.usecase.AssignOperationalStatusService;
import com.seashare.seasharem1.application.usecase.AuthenticateUserService;
import com.seashare.seasharem1.application.usecase.BaseRateQueryService;
import com.seashare.seasharem1.application.usecase.CheckLegalRegistrationService;
import com.seashare.seasharem1.application.usecase.CompleteRegistrationService;
import com.seashare.seasharem1.application.usecase.CreateDraftService;
import com.seashare.seasharem1.application.usecase.DeleteBoatService;
import com.seashare.seasharem1.application.usecase.DeleteDraftService;
import com.seashare.seasharem1.application.usecase.EditBoatService;
import com.seashare.seasharem1.application.usecase.GetBoatDetailService;
import com.seashare.seasharem1.application.usecase.GetDraftService;
import com.seashare.seasharem1.application.usecase.ListOwnerBoatsService;
import com.seashare.seasharem1.application.usecase.NotificationService;
import com.seashare.seasharem1.application.usecase.OwnerMaintenanceService;
import com.seashare.seasharem1.application.usecase.RequestMaintenanceReviewService;
import com.seashare.seasharem1.application.usecase.ReservationsVesselQueryService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Registra los casos de uso (Java puro) como beans. Cada funcionalidad agrega los suyos aquí. */
@Configuration
@EnableScheduling // entrega de avisos de mantenimiento al Módulo 2 (outbox)
public class UseCaseConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public LoginUseCase loginUseCase(
      UserRepositoryPort users, PasswordVerifierPort passwords, TokenIssuerPort tokens) {
    return new AuthenticateUserService(users, passwords, tokens);
  }

  @Bean
  public ListOwnerBoatsUseCase listOwnerBoatsUseCase(VesselRepositoryPort vessels, Clock clock) {
    return new ListOwnerBoatsService(vessels, clock);
  }

  @Bean
  public CheckLegalRegistrationUseCase checkLegalRegistrationUseCase(VesselRepositoryPort vessels) {
    return new CheckLegalRegistrationService(vessels);
  }

  @Bean
  public ReservationsVesselQuery reservationsVesselQuery(
      VesselRepositoryPort vessels,
      PortRepositoryPort ports,
      UserRepositoryPort users,
      Clock clock) {
    return new ReservationsVesselQueryService(vessels, ports, users, clock);
  }

  @Bean
  public AssignOperationalStatusUseCase assignOperationalStatusUseCase(
      VesselRepositoryPort vessels, ProcessedEventRepositoryPort processedEvents, Clock clock) {
    return new AssignOperationalStatusService(vessels, processedEvents, clock);
  }

  @Bean
  public BaseRateQuery baseRateQuery(VesselRepositoryPort vessels) {
    return new BaseRateQueryService(vessels);
  }

  @Bean
  public GetDraftUseCase getDraftUseCase(VesselRepositoryPort vessels) {
    return new GetDraftService(vessels);
  }

  @Bean
  public CreateDraftUseCase createDraftUseCase(VesselRepositoryPort vessels) {
    return new CreateDraftService(vessels);
  }

  @Bean
  public DeleteDraftUseCase deleteDraftUseCase(VesselRepositoryPort vessels) {
    return new DeleteDraftService(vessels);
  }

  @Bean
  public CompleteRegistrationUseCase completeRegistrationUseCase(
      VesselRepositoryPort vessels,
      PortRepositoryPort ports,
      ImageStoragePort images,
      Clock clock) {
    return new CompleteRegistrationService(vessels, ports, images, clock);
  }

  @Bean
  public DeleteBoatUseCase deleteBoatUseCase(VesselRepositoryPort vessels, Clock clock) {
    return new DeleteBoatService(vessels, clock);
  }

  @Bean
  public NotificationService notificationService(NotificationRepositoryPort notifications) {
    return new NotificationService(notifications);
  }

  @Bean
  public RequestMaintenanceReviewUseCase requestMaintenanceReviewUseCase(
      VesselRepositoryPort vessels,
      UserRepositoryPort users,
      NotificationRepositoryPort notifications,
      Clock clock) {
    return new RequestMaintenanceReviewService(vessels, users, notifications, clock);
  }

  @Bean
  public AdminMaintenanceUseCase adminMaintenanceUseCase(
      VesselRepositoryPort vessels,
      PortRepositoryPort ports,
      NotificationRepositoryPort notifications,
      Clock clock) {
    return new AdminMaintenanceService(vessels, ports, notifications, clock);
  }

  @Bean
  public AdminDeleteBoatUseCase adminDeleteBoatUseCase(
      VesselRepositoryPort vessels, NotificationRepositoryPort notifications, Clock clock) {
    return new AdminDeleteBoatService(vessels, notifications, clock);
  }

  @Bean
  public GetBoatDetailUseCase getBoatDetailUseCase(
      VesselRepositoryPort vessels, PortRepositoryPort ports) {
    return new GetBoatDetailService(vessels, ports);
  }

  @Bean
  public EditBoatUseCase editBoatUseCase(
      VesselRepositoryPort vessels, PortRepositoryPort ports, ImageStoragePort images) {
    return new EditBoatService(vessels, ports, images);
  }

  @Bean
  public OwnerMaintenanceService ownerMaintenanceService(
      VesselRepositoryPort vessels, PortRepositoryPort ports, Clock clock) {
    return new OwnerMaintenanceService(vessels, ports, clock);
  }

  @Bean
  public AdminFleetService adminFleetService(
      VesselRepositoryPort vessels, UserRepositoryPort users, PortRepositoryPort ports) {
    return new AdminFleetService(vessels, users, ports);
  }

  @Bean
  public AdminOwnersService adminOwnersService(UserRepositoryPort users) {
    return new AdminOwnersService(users);
  }
}
