# Plan técnico: Enviar a Mantenimiento (Propietario y Administrador)

**Fecha**: 10 de noviembre de 2026 (actualizado el 12 de noviembre de 2026 con la parte del Administrador)
**Especificación**: `enviar-mantenimiento-spec.md`
**Contrato**: `enviar-mantenimiento.md`
**Estado**: Implementado — pendiente de `mvn verify` y prueba manual

## Resumen

El Propietario envía su embarcación Disponible a Mantenimiento y la devuelve a Disponible, sin dar motivo. Dos `POST` sin cuerpo; el botón vive en «Ver Detalles». La suspensión del Administrador (historias 3–5) y las notificaciones quedan fuera.

## Decisiones

| # | Decisión | Motivo |
|---|---|---|
| D1 | Rutas `POST /{id}/maintenance/start` y `/finish` | Acciones explícitas; el Administrador añadirá su variante con motivo |
| D2 | Origen en `maintenance_by`; NULL = sistema (fin de alquiler) | El modelo físico ya existe; evita un valor `SYSTEM` que requeriría aprobación |
| D3 | Propietario puede terminar si el origen es NULL u `OWNER`; con `ADMIN`, 409 | FR-006 / SC-002 |
| D4 | Repetir una acción ya aplicada devuelve 200 sin cambios | Reintentos tras timeout sin error falso |
| D5 | `UPDATE` condicional atómico por acción | Evita pisar una reserva simultánea |
| D6 | 409 informa `current_status` y `maintenance_by` | La pantalla explica qué pasó |
| D7 | Respuesta 200 = embarcación completa; el frontend redibuja sin recargar | Sin modal de éxito (wireframe) |
| D8 | Botón deshabilitado en Reservado / En Navegación, sin tooltip | FR-010 y Detalles FR-024 |
| D9 | Etiqueta del estado: «Mantenimiento» | Decisión de Jose |
| D10 | Sin migración (V7) ni notificaciones | Alcance del Propietario |
| D11 | Solo termina quien inició el mantenimiento; el botón se ve pero deshabilitado para el otro rol | Decisión de Jose (12 de noviembre) |
| D12 | Origen vacío (sistema) lo termina solo el Propietario | Igual que antes; el administrador no lo termina |
| D13 | `AdminMaintenanceService` aparte de `OwnerMaintenanceService`, con consultas `UPDATE` propias (`startMaintenanceAsAdmin`, `finishMaintenanceAsAdmin`) | Cada rol conserva sus reglas sin ramas |
| D14 | Motivo obligatorio (1 a 300) validado antes de buscar la embarcación; viaja en el aviso | FR-015 |
| D15 | Aviso `MAINTENANCE_NOTICE` al enviar y `VESSEL_AVAILABLE` al terminar, creados después del cambio atómico, sin revertir si fallan | Sin duplicados en reintentos |
| D16 | `OwnerNotifier` y `AdminReason` compartidos con Eliminar | Evita duplicar fecha, aviso y validación |
| D17 | `maintenance_by` se agrega a `CompletedBoatResponse` | Deshabilitar el botón sin otra consulta |
| D19 | El Propietario de una embarcación suspendida por el Administrador ve «Solicitar revisión» en lugar de «Terminar» | Decisión de Jose (12 de noviembre) |
| D20 | `RequestMaintenanceReviewService`: un `REVIEW_REQUEST` por administrador, solo la primera vez por mantenimiento (ver D22); no cambia el estado | Historia 4 |
| D21 | `NotificationController` admite OWNER y ADMIN; `NotificationResponse` incluye `vessel_id` | La campana del administrador navega al detalle |
| D22 | «Solicitar revisión» se usa una sola vez por mantenimiento; marca persistida en `vessel.review_requested` (V9) | Decisión de Jose (12 de noviembre) |
| D18 | El nombre de la embarcación en la campana se lee del texto del aviso | Sin cambiar la respuesta de notificaciones; si cambia el texto, hay que actualizar el patrón |

## Contexto técnico

Backend: Java 17, Spring Boot 4.1.1, Clean Architecture (ArchUnit), JPA, Flyway V1–V7, MySQL 8, Testcontainers.
Frontend: JS vanilla ES modules, Vitest + jsdom, ESLint, Prettier.

## Estructura

```
backend
├── domain/model/Vessel.java                   (+ canBeSentToMaintenance, ownerCanFinishMaintenance)
├── domain/exception/BoatMaintenanceConflictException.java
├── application/port/in/SendToMaintenanceUseCase.java, FinishMaintenanceUseCase.java
├── application/usecase/OwnerMaintenanceService.java
├── application/port/out/VesselRepositoryPort.java   (+ startMaintenance, finishMaintenance)
└── infrastructure/
    ├── persistence/VesselJpaRepository.java, VesselPersistenceAdapter.java
    ├── web/BoatController.java, GlobalExceptionHandler.java
    └── configuration/UseCaseConfig.java
frontend/src
├── components/maintenanceFlow.js (+ test)
├── pages/boatDetail.js (+ test)
├── services/fleetService.js (+ test)
├── api/client.js (+ test)      (maintenanceBy en ApiError)
└── ui/boatFormat.js (+ test)   (etiqueta «Mantenimiento»)
```

## Fases

### Fase 1: Backend
- [x] T129 Reglas de dominio en `Vessel` y `BoatMaintenanceConflictException`
- [x] T130 Casos de uso y `OwnerMaintenanceService` (404 → 409 → cambio atómico, idempotencia)
- [x] T131 Puerto `startMaintenance` / `finishMaintenance` y consultas `UPDATE` condicionales
- [x] T132 Endpoints en `BoatController`, `current_status` y `maintenance_by` en el 409, bean en `UseCaseConfig`
- [x] T133 Pruebas: `OwnerMaintenanceServiceTest` (unitarias), `Fakes`, casos en `BoatRegistrationApiIT`

### Fase 2: Frontend
- [x] T134 `fleetService.startMaintenance` / `finishMaintenance` (POST, 15 s) y `maintenanceBy` en `ApiError`
- [x] T135 `maintenanceFlow.js`: confirmación, éxito silencioso, 404 / 409 / admin / técnico
- [x] T136 `boatDetail.js`: botón conectado, deshabilitado si está ocupada, redibujo con la respuesta
- [x] T137 Etiqueta «Mantenimiento» y pruebas (263 en verde)

### Fase 3: Administrador (12 de noviembre de 2026)
- [x] T141 Backend: `Vessel.adminCanFinishMaintenance`, consultas `AsAdmin`, `VesselRepositoryPort`/adaptador
- [x] T142 Backend: `AdminMaintenanceUseCase`/`AdminMaintenanceService` (motivo, 404, 409, idempotencia, avisos), `OwnerNotifier`, `AdminReason`
- [x] T143 Backend: endpoints `start` y `finish` en `AdminFleetController`, bean, `maintenance_by` en `CompletedBoatResponse`
- [x] T144 Backend: `AdminMaintenanceServiceTest` y casos en `BoatRegistrationApiIT`
- [x] T145 Frontend: `startAdminMaintenance`/`finishAdminMaintenance`, `createReasonField`, `maintenanceFlow` con `admin`
- [x] T146 Frontend: botón deshabilitado según el origen (`canFinishMaintenance`, `finishLabel`) en propietario y administrador
- [x] T147 Frontend: campana con «Revisar completamente» y «Detalle de Notificación» (`MAINTENANCE_NOTICE`)
- [x] T148 Frontend: pruebas (359 en verde en `npm run check`)

### Fase 4: Solicitar revisión (12 de noviembre de 2026)
- [x] T149 Backend: `RequestMaintenanceReviewUseCase`/`Service`, `existsUnread` en `NotificationRepositoryPort`, `POST .../maintenance/request-review`, `vessel_id` y acceso ADMIN en notificaciones
- [x] T150 Backend: `RequestMaintenanceReviewServiceTest` y casos en `BoatRegistrationApiIT`
- [x] T151 Frontend: `requestMaintenanceReview`, `startReviewRequestFlow`, botón «Solicitar revisión»
- [x] T152 Frontend: campana del administrador («Notificaciones Administrativas», «Ver detalles»)
- [x] T153 Frontend: pruebas (369 en verde en `npm run check`)

### Fase 4b: Solicitar revisión una sola vez (12 de noviembre de 2026)
- [x] T154 Migración `V9__vessel_review_requested.sql` y campo `reviewRequested` en `Vessel`/`VesselEntity`
- [x] T155 Backend: `markReviewRequested` atómico (puerto, adaptador, JPA); reinicio en inicio/fin de mantenimiento; servicio idempotente sin avisos repetidos; `review_requested` en `CompletedBoatResponse`
- [x] T156 Backend: pruebas (`RequestMaintenanceReviewServiceTest`, `BoatRegistrationApiIT`, `DatabaseMigrationIT` con 9 migraciones)
- [x] T157 Frontend: botón «Solicitar revisión» deshabilitado con `review_requested` y tras enviar; pruebas (372 en verde)

### Fase 5: Verificación (pendiente)
- [ ] T138 `mvn spotless:apply` y `mvn clean verify`
- [ ] T139 Prueba manual: enviar, terminar, cancelar, dos pestañas, embarcación Reservada
- [ ] T139b Prueba manual del administrador: enviar con motivo, ver el aviso del propietario («Revisar completamente»), botón bloqueado cruzado entre roles, terminar
- [ ] T140 Prueba en Postman

## Pendientes

| Tema | Nota |
|---|---|
| Historia 5, escenario 2 | «Reportar nuevamente» del Administrador |
| Avisos al administrador | Solo recibe `REVIEW_REQUEST`; los demás tipos siguen siendo del propietario |
| Fin de alquiler automático | Pertenece a Módulo 2; deberá dejar `maintenance_by` vacío |

## Dependencias

Registro, Listar, Detalles, Editar y Eliminar ya implementados.

## Notas

- Editar y Eliminar siguen permitidos en Mantenimiento.
- Spotless se exige en `verify`.
