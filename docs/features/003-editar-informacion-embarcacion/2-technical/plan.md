# Plan técnico: Editar información de embarcación

**Fecha**: 9 de noviembre de 2026
**Especificación**: `editar-embarcacion-spec.md`
**Contrato**: `editar-embarcacion.md`
**Estado**: Implementado — pendiente de prueba manual en navegador y `mvn verify`

## Resumen

El Propietario edita nombre, capacidad, puerto (nombre + coordenadas), tarifa, foto y servicios de una embarcación Disponible o En Mantenimiento. Tipo y matrícula son de solo lectura. Un único `PUT` multipart; el botón «Editar» vive en «Ver Detalles».

## Decisiones

| # | Decisión | Motivo |
|---|---|---|
| D1 | Sin control de versión; gana el último guardado | Solo el propietario titular edita; evita columna nueva |
| D2 | Puerto compartido por nombre: coordenadas distintas con nombre existente actualizan ese puerto | Acordado con Jose |
| D3 | Orden: 404 → 409 (con `current_status`) → 400 → persistencia | Spec FR-025 |
| D4 | `UPDATE` condicional atómico (id, dueño, no eliminada, estado editable) | Cierra la carrera contra una reserva simultánea |
| D5 | Puerto + embarcación en una transacción (`TransactionTemplate`, rollback si 0 filas, 1 reintento por unicidad) | Consistencia |
| D6 | Foto opcional; servicios tri-estado | Conservar lo guardado sin reenviar |
| D7 | Embarcación ajena/eliminada/borrador/inexistente → 404 idéntico | Anti-enumeración |
| D8 | Tope de tarifa 9.999.999.999 | Igual que el registro y la BD |
| D9 | Botón Editar deshabilitado sin tooltip | FR-024 de Detalles |
| D10 | Guardia de navegación en el router + `beforeunload` | Descartar cambios sin guardar |
| D11 | Sin migración (V7) | Nada nuevo en el modelo físico |

## Contexto técnico

Backend: Java 17, Spring Boot 4.1.1, Clean Architecture (ArchUnit), JPA + MapStruct, Flyway V1–V7, MySQL 8, Cloudinary, Testcontainers.
Frontend: JS vanilla ES modules, router por hash, Vitest + jsdom, ESLint, Prettier, Leaflet.

## Estructura

```
backend
├── domain/model/Vessel.java            (+ canBeEdited, edited)
├── domain/model/Port.java              (+ sameLocationAs)
├── domain/exception/BoatNotEditableException.java
├── domain/rules/RegistrationRules.java (+ normalizeName, parseCapacity, EDIT_REQUIRED)
├── application/port/in/EditBoatCommand.java, EditBoatUseCase.java
├── application/usecase/EditBoatService.java
├── application/port/out/VesselRepositoryPort.java (+ applyEdit)
└── infrastructure/
    ├── persistence/VesselJpaRepository.java (+ updateEditable)
    ├── persistence/VesselPersistenceAdapter.java (+ applyEdit)
    ├── web/BoatController.java (+ PUT /{boatId})
    ├── web/GlobalExceptionHandler.java (+ current_status)
    └── config/UseCaseConfig.java
frontend/src
├── pages/editBoat.js (+ test)
├── pages/boatDetail.js (botón Editar)
├── components/mapPicker.js (punto inicial)
├── routes/router.js, routes.js (guardia, ruta /owner/boats/:id/edit)
├── services/fleetService.js (editBoat)
└── styles/main.css
```

## Fases

### Fase 1: Dominio y aplicación
- [x] T107 `Vessel.canBeEdited` / `edited` y estados editables
- [x] T108 `BoatNotEditableException` con `currentStatus`
- [x] T109 `RegistrationRules`: normalizar nombre, parsear capacidad por tipo, mensaje obligatorio
- [x] T110 `EditBoatCommand`, `EditBoatUseCase`, `EditBoatService` (orden 404/409/400)
- [x] T111 Puerto saliente `applyEdit` y `Port.sameLocationAs`

### Fase 2: Infraestructura
- [x] T112 `updateEditable` (UPDATE nativo condicional)
- [x] T113 `applyEdit` transaccional con reintento por unicidad
- [x] T114 `PUT /api/v1/fleet/boats/{boatId}` multipart
- [x] T115 `current_status` en el 409 y bean del caso de uso

### Fase 3: Pruebas backend
- [x] T116 `EditBoatServiceTest`, `VesselEditTest`, `RegistrationRulesTest`
- [x] T117 `Fakes` ampliados
- [x] T118 `BoatRegistrationApiIT`: éxito, foto conservada, servicios, puerto compartido/nuevo, 400, 403, 404, 409, idempotencia

### Fase 4: Frontend
- [x] T119 `fleetService.editBoat` (multipart, timeout 15 s)
- [x] T120 Guardia de navegación en el router (`confirmLeave`, `dispose`, restauración de hash)
- [x] T121 Ruta `/owner/boats/:id/edit` y botón Editar en Detalles
- [x] T122 `editBoatPage`: carga, bloqueo por estado, errores de carga
- [x] T123 Formulario: campos bloqueados, normalización, validación en vivo, dirty, foto, mapa, servicios
- [x] T124 Guardado: spinner, modales (éxito, descartar, técnico, 403/404/409), errores 400 con foco
- [x] T125 Estilos y pruebas (247 tests, `npm run check` verde)

### Fase 5: Verificación (pendiente)
- [ ] T126 `mvn spotless:apply` y `mvn clean verify` (no se pudo correr Maven en el sandbox)
- [ ] T127 Prueba manual en navegador (flujo completo, dos pestañas, estado cambiado en medio)
- [ ] T128 Prueba en Postman con la colección entregada

## Pendientes

| Tema | Nota |
|---|---|
| Imagen huérfana en Cloudinary | Se sube antes de la transacción; limpieza diferida |
| Edición por Administrador | Fuera de alcance; se hará con la vista Admin |
| `instance` en 401/403 | Tarea #16 pendiente |
| Plan general | Actualizar: sin V8 ni cambios al modelo físico |

## Dependencias

Registro (borrador/completo), Listar flota, Ver Detalles y Eliminar ya implementados.

## Notas

- Los servicios Capitán y Combustible van siempre incluidos y no se editan.
- Spotless se exige en `verify`.
