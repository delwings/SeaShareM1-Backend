# Plan de implementación: Eliminar embarcación (Propietario y Administrador)

**Fecha**: 7 de noviembre de 2026 (actualizado el 11 de noviembre de 2026 con la parte del Administrador)
**Especificación**: `docs/features/eliminar-embarcacion/spec.md` (spec_eliminar_embarcacion.md, creada el 3 de noviembre de 2026)
**Contrato REST**: `docs/features/eliminar-embarcacion/contract.md`
**Estado**: Propietario y Administrador implementados y entregados. Frontend: 187 pruebas en verde y revisión visual. Backend: pruebas unitarias del caso de uso en verde; falta que el usuario confirme `mvn spotless:apply` y `mvn clean verify` (incluye las pruebas de integración con MySQL y la migración V7).

> Lo común a todo el módulo (stack, seguridad, errores, despliegue, modelo físico) está en el plan general, que se actualizó con la migración V7. Esta funcionalidad se apoya en Consultar detalles (`docs/features/consultar-detalles/plan.md`): el botón «Eliminar» vive en la pantalla «Ver Detalles».

## Resumen

El propietario pulsa «Eliminar» en «Ver Detalles», confirma en un modal y la embarcación se elimina de forma lógica: deja de aparecer y de poder consultarse, su matrícula queda libre y el registro se conserva con fecha y autor.

**Alcance**: la entrega del 7 de noviembre fue solo del Propietario. El 11 de noviembre se agregó el Administrador (historia 2) y la notificación al propietario (historia 4); el regreso a la tabla es **sin filtros** (decisión del usuario, igual que «Volver»).

Enfoque técnico: `DELETE /api/v1/fleet/boats/{boatId}` marca el registro con `deleted = TRUE`, `deleted_at` y `deleted_by` en **una sola sentencia condicional** (solo si sigue sin eliminar y en `AVAILABLE` o `MAINTENANCE`). Así dos eliminaciones simultáneas, o un cambio de estado en medio, no producen duplicados ni eliminan por error (FR-029, NFR-002). La matrícula se libera con una columna generada y un índice único nuevo.

### Decisiones de diseño

| # | Decisión | Motivo |
|---|---|---|
| D1 | Migración V7: `deleted_at`, `deleted_by` (FK a `users`) y columna generada `active_legal_registration` con índice único; se elimina `uq_vessel_legal_registration` | Excepción al modelo físico autorizada el 7 de noviembre de 2026. MySQL no tiene índices parciales; los NULL no chocan en un índice único |
| D2 | La eliminación es una sentencia `UPDATE ... WHERE id = ? AND deleted = FALSE AND status IN ('AVAILABLE','MAINTENANCE')` | Atómica: la fecha, el autor y la marca se guardan juntos o no se guardan (NFR-002) |
| D3 | Si el `UPDATE` no afecta filas, el servicio vuelve a leer: si ya no existe, 404; si cambió de estado, 409 con el estado actual | FR-023 y FR-024 sin carreras |
| D4 | La fecha y el autor viven solo en la base de datos; el modelo de dominio `Vessel` y la entidad JPA no los exponen | Nadie los consulta todavía (historial fuera de alcance, [S-10]); evita tocar el mapper y todas las pruebas |
| D5 | El orden de verificación es: existe y es del propietario (404), estado permite (409) y elimina | FR-022 |
| D6 | Embarcación ajena, inexistente, eliminada o borrador responden el mismo 404 | FR-025; no revelar que existe |
| D7 | El 409 trae la propiedad `current_status` (por ejemplo `RESERVED`) | El modal debe decir a qué estado pasó |
| D8 | `existsByLegalRegistration` ahora ignora las eliminadas | La verificación de matrícula y el registro aceptan una matrícula liberada |
| D9 | Las listas y el detalle ya filtraban `deleted = FALSE`; no cambian | FR-014 |
| D10 | Frontend: tiempo límite de 15 s con `AbortController` en `apiRequest` (opción `timeoutMs`) | FR-026; un tiempo agotado cuenta como fallo de conexión |
| D11 | Frontend: todo el flujo vive en `components/deleteBoatFlow.js`; cada botón de modal queda deshabilitado al pulsarse | FR-008: una sola solicitud aunque haya doble clic o Enter |
| D12 | «Recargar página» vuelve a consultar el detalle (no recarga el navegador) | Muestra el estado nuevo y el botón «Eliminar» deshabilitado sin perder la sesión en memoria |
| D13 | Sesión vencida (401): se cierran los modales y el cliente redirige al inicio de sesión | Versión mínima de FR-027, igual que en «Ver Detalles» |
| D14 | Migración V8: se agrega `VESSEL_DELETED` al ENUM `notification.type` | Autorizado el 11 de noviembre de 2026; la campana futura podrá distinguir el aviso |
| D15 | `AdminDeleteBoatService` aparte de `DeleteBoatService`: el administrador no filtra por propietario y notifica | Cada rol conserva sus reglas sin ramas internas |
| D16 | La notificación se crea después del `UPDATE` atómico y dentro de un `try/catch`; si falla, solo se registra en el log | FR-020 y FR-029: sin duplicados y sin revertir |
| D17 | Fecha del texto en `America/Bogota`; la fecha guardada sigue en UTC | Lo que lee el propietario coincide con su día |
| D18 | Frontend: `startDeleteBoat` recibe `remove` y `listPath`; el administrador usa `deleteAdminBoat` y `/admin` | Mismos modales, un solo flujo |

## Contexto técnico

**Lenguaje/Versión**: Java 17 (backend); HTML, CSS y JavaScript puro con módulos ES (frontend).
**Dependencias principales**: ninguna nueva.
**Almacenamiento**: MySQL 8; migraciones V7 y V8.
**Pruebas**: JUnit 5 y Testcontainers (backend); Vitest con jsdom (frontend).
**Plataforma objetivo**: Render (backend) y Vercel (frontend).
**Tipo de proyecto**: web, dos repositorios.
**Metas de rendimiento**: confirmación visible en menos de 2 segundos en el 95% de los casos; la solicitud se cancela a los 15 segundos (NFR-003).
**Restricciones**: textos exactos de la spec; modales que solo se cierran con sus botones; sin spinners ni tooltips.
**Escala/Alcance**: 2 endpoints, 2 migraciones, 1 flujo de 5 modales; historias 1 a 5 de la spec (sin la campana).

## Estructura del proyecto

### Documentación (esta funcionalidad)

```text
docs/features/eliminar-embarcacion/
├── plan.md        # este archivo
├── contract.md    # contrato REST
└── spec.md        # especificación
```

### Código fuente (raíz del repositorio)

```text
SeaShareM1/  (backend)
├── src/main/resources/db/migration/V7__vessel_logical_deletion.sql   # nuevo
├── src/main/java/com/seashare/seasharem1/
│   ├── domain/
│   │   ├── model/Vessel.java                          # modificado: canBeDeleted(), asDeleted()
│   │   └── exception/BoatNotDeletableException.java   # nuevo: 409 con el estado actual
│   ├── application/
│   │   ├── port/in/DeleteBoatUseCase.java             # nuevo
│   │   ├── port/out/VesselRepositoryPort.java         # modificado: markDeleted
│   │   └── usecase/DeleteBoatService.java             # nuevo
│   ├── infrastructure/adapter/
│   │   ├── in/web/BoatController.java                 # modificado: DELETE /{boatId}
│   │   ├── in/web/GlobalExceptionHandler.java         # modificado: current_status en el 409
│   │   └── out/persistence/
│   │       ├── VesselPersistenceAdapter.java          # modificado: markDeleted, matrícula
│   │       └── repository/VesselJpaRepository.java    # modificado: UPDATE nativo
│   └── configuration/UseCaseConfig.java               # modificado: bean deleteBoatUseCase
└── src/test/java/com/seashare/seasharem1/
    ├── unit/application/DeleteBoatServiceTest.java    # nuevo
    ├── unit/application/Fakes.java                    # modificado
    └── integration/{BoatRegistrationApiIT, DatabaseMigrationIT}.java   # modificados

SeaShareM1-Frontend/
└── src/
    ├── api/client.js (+test)               # timeoutMs y currentStatus en ApiError
    ├── services/fleetService.js (+test)    # deleteBoat, DELETE_TIMEOUT_MS
    ├── components/deleteBoatFlow.js (+test)  # nuevo: confirmación y modales de resultado
    ├── pages/boatDetail.js (+test)         # botón «Eliminar» habilitado según el estado
    ├── ui/modal.js                         # icono de eliminar
    └── styles/main.css                     # botón y tono rojos, advertencia del modal
```

---

## Fase 1: Setup (infraestructura compartida)

Completada en el plan general (T001 a T009). Sin dependencias ni herramientas nuevas.

---

## Fase 2: Foundational (prerrequisitos bloqueantes)

**Objetivo**: preparar la base de datos para la eliminación lógica.

- [x] T084 Backend: migración `V7__vessel_logical_deletion.sql` con `deleted_at`, `deleted_by`, `active_legal_registration` (columna generada) y el índice único `uq_vessel_active_legal_registration`, que reemplaza a `uq_vessel_legal_registration`.
- [x] T085 Backend: `VesselPersistenceAdapter` reconoce el nuevo índice al convertir el choque de matrícula en 409, y `existsByLegalRegistrationAndDeletedFalse` ignora las eliminadas.
- [x] T086 `DatabaseMigrationIT` espera 7 migraciones.
- [x] T087 Documentación: plan general, tablas del modelo físico y diagrama `modelo-fisico-modulo1.svg` y `.png` actualizados (V7).

**Punto de control**: Flyway aplica V7 y Hibernate valida la entidad.

---

## Fase 3: Historia 1 - Eliminar como Propietario (Prioridad: P1)

**Objetivo**: confirmar y eliminar una embarcación propia en estado `Disponible` o `En Mantenimiento/Limpieza`.

**Prueba independiente**: con un propietario, abrir «Ver Detalles» de una embarcación `Disponible`, pulsar «Eliminar», probar «Conservar» y luego «Sí, eliminar»; comprobar el mensaje, la lista y que la matrícula quede libre.

### Pruebas de la historia 1

- [x] T088 [P] Backend: `DeleteBoatServiceTest`: elimina en `AVAILABLE` y `MAINTENANCE` y registra el autor; `RESERVED` y `NAVIGATION` no se eliminan y reportan su estado; ajena, desconocida y borrador dan `ResourceNotFoundException`; eliminar dos veces da 404 sin duplicar; la matrícula queda libre.
- [x] T089 [P] Backend: pruebas de integración: 204 y desaparece de la lista y del detalle; el registro se conserva con `deleted_at` y `deleted_by`; la matrícula se puede reusar; segunda eliminación 404; ajena 404 sin cambios; `RESERVED` 409 con `current_status`; `MAINTENANCE` 204; administrador 403.
- [x] T090 [P] Frontend: `deleteBoatFlow.test.js`: textos exactos, «Conservar», cierre solo con botones, éxito, doble clic.

### Implementación de la historia 1

- [x] T091 Backend: `Vessel.canBeDeleted()` y `asDeleted()`; puerto `markDeleted`; caso de uso `DeleteBoatService` (orden de verificación de FR-022).
- [x] T092 Backend: `UPDATE` nativo y transaccional en `VesselJpaRepository`; `DELETE /fleet/boats/{boatId}` en `BoatController` (204); bean en `UseCaseConfig`.
- [x] T093 Frontend: `deleteBoat(boatId)` en `fleetService`; modal «¿Eliminar embarcación?» con icono de X roja, texto, advertencia en rojo y botones «Conservar» y «Sí, eliminar»; modal «¡Embarcación eliminada!» y «Aceptar» a la lista.

**Punto de control**: el propietario elimina una embarcación y su matrícula queda libre.

---

## Fase 4: Historia 3 - Estados en los que se puede eliminar (Prioridad: P2)

- [x] T094 Frontend: «Eliminar» habilitado en `AVAILABLE` y `MAINTENANCE`; deshabilitado en `RESERVED` y `NAVIGATION`, sin tooltips y con el cursor de prohibido; el botón deshabilitado no abre nada.
- [x] T095 [P] Frontend: pruebas de `boatDetail` para los cuatro estados.

---

## Fase 5: Historia 5 - Errores, cambios simultáneos y sesión (Prioridad: P2)

- [x] T096 Backend: `BoatNotDeletableException` (409) y `current_status` en la respuesta.
- [x] T097 Frontend: `apiRequest` con `timeoutMs` (15 s) y `currentStatus` en `ApiError`.
- [x] T098 Frontend: modal «No es posible eliminar la embarcación» con «Recargar página» (409); «Esta embarcación ya no existe» con «Aceptar» (404); «No se pudo eliminar la embarcación» con «Cerrar» y «Reintentar» (red, 5xx o 15 s).
- [x] T099 Frontend: doble clic y Enter envían una sola solicitud (botones deshabilitados al pulsarse); 401 cierra los modales y el cliente redirige al inicio de sesión.
- [x] T100 Frontend: revisión visual del modal en escritorio y a 360 px.
- [ ] T101 Backend: confirmar `mvn spotless:apply` y `mvn clean verify`.
- [ ] T102 Prueba manual en el navegador: eliminar en `Disponible`, intentar en `Reservado` (cambiando el estado en la base de datos), registrar de nuevo con la matrícula liberada.

---

## Pendientes y trabajo diferido

- [x] T103 Historia 2: eliminar como Administrador (`DELETE /admin/fleet/boats/{boatId}`) y regreso a «Control General de Flota Fluvial» sin filtros.
- [x] T104 Historia 4: notificación `VESSEL_DELETED` al propietario cuando elimina un administrador (V8, `NotificationRepositoryPort`, `AdminDeleteBoatService`).
- [x] T104b Centro de notificaciones (campana): hecho el 11 de noviembre de 2026, ver `notificaciones-plan.md`.
- [ ] T105 FR-027: guardar el destino de retorno al vencer la sesión (junto con T081 del plan de Consultar detalles).
- [ ] T106 Si la spec define un texto de apoyo para el modal «Esta embarcación ya no existe», agregarlo (hoy solo lleva el título y «Aceptar»).

---

## Fase 6: Administrador y notificación (11 de noviembre de 2026)

- [x] T107e Backend: V8, `NotificationType.VESSEL_DELETED`, `NotificationRepositoryPort` y adaptador JPA, `AdminDeleteBoatUseCase`/`AdminDeleteBoatService`, `DELETE` en `AdminFleetController`, bean en `UseCaseConfig`.
- [x] T108e Backend: `AdminDeleteBoatServiceTest` (elimina, notifica una vez con el texto y la fecha de Colombia, `RESERVED`/`NAVIGATION` no eliminan ni notifican, 404, fallo del aviso no revierte); pruebas de integración (204 con fila en `notification`, doble eliminación sin segunda notificación, 409, propietario 403 y sin notificación); `DatabaseMigrationIT` espera 8 migraciones.
- [x] T109e Frontend: `deleteAdminBoat`, `startDeleteBoat` con `remove` y `listPath`, `adminBoatDetail` con «Eliminar» funcional; pruebas (322 en verde en `npm run check`).
- [x] T112e Motivo obligatorio del administrador (campo en el modal, `reason` en el cuerpo, 400 si falta) y campana de notificaciones: ver `notificaciones-plan.md`.
- [ ] T110e Backend: confirmar `mvn spotless:apply` y `mvn clean verify`.
- [ ] T111e Prueba manual: eliminar como admin, comprobar la fila `VESSEL_DELETED` en `notification` y que el propietario ya no vea la embarcación.

---

## Dependencias y orden de ejecución

- Fase 2 (V7) debe aplicarse antes de usar el endpoint: sin la migración, el `UPDATE` falla.
- Fases 3 a 5 dependen de «Ver Detalles» (Consultar detalles).
- La fase 6 depende de la vista del administrador (Consultar detalles) y de V8; la campana depende del spec de notificaciones.

## Notas

- **Spec de eliminar**: se aplicó tal como está para el Propietario. Los textos son los de FR-005, FR-015, FR-023, FR-024 y FR-026. Cuando se haga la parte del administrador se revisan los [S-nn] que aún están marcados como pendientes.
- **Contrato del 409**: `current_status` usa los nombres internos (`RESERVED`, `NAVIGATION`); el frontend los muestra con el diccionario único de estados.
- **Reinicio**: tras copiar los archivos hay que reiniciar el backend para que Flyway aplique V7 y V8.
- **Spec**: en la versión del 11 de noviembre «Aceptar» del administrador vuelve sin filtros (FR-016, historia 2).
- **Registros ya eliminados**: antes de V7 no había eliminación, así que no hay datos que migrar.
