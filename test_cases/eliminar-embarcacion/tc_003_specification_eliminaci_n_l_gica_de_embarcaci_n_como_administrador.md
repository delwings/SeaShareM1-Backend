### Test Case Specification: Eliminación Lógica de Embarcación como Administrador
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/eliminar-embarcacion/spec.md` | `docs/features/eliminar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-003-EliminacionAdministrador
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el flujo de eliminación lógica de cualquier embarcación de la flota por parte del Administrador desde "Ver Detalles", incluyendo la presencia del botón "Eliminar", el modal de confirmación con campo de motivo obligatorio (hasta 300 caracteres, actualizado el 11 de noviembre de 2026), la cancelación con "Conservar", la validación del motivo, el envío a `DELETE /api/v1/admin/fleet/boats/{boatId}` y el regreso a "Control General de Flota Fluvial" en su estado por defecto sin filtros (US2, FR-001, FR-005 a FR-007, FR-015, FR-016, Historial 11/11/2026).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Eliminar Embarcación | Specification Document | `docs/features/eliminar-embarcacion/spec.md` |
| Implementation Plan: Eliminar Embarcación | Plan Document | `docs/features/eliminar-embarcacion/plan.md` |
| Contrato REST: Eliminar Embarcación | API Contract | `docs/architecture/contracts/rest/eliminar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* El campo de motivo obligatorio (hasta 300 caracteres) en el modal de confirmación aplica únicamente al Administrador; el Propietario no ve ese campo (Historial de cambios del 11 de noviembre de 2026).
* Tras aceptar el modal de éxito, el Administrador regresa a "Control General de Flota Fluvial" sin filtros aplicados (FR-016, Decisión D18).

##### Dependencies & Assumptions
* Migraciones V7 y V8 aplicadas, `AdminDeleteBoatService` y `DELETE /api/v1/admin/fleet/boats/{boatId}` operativos.
* Existen embarcaciones registradas por distintos propietarios en estados `AVAILABLE` y `MAINTENANCE`, y la tabla "Control General de Flota Fluvial" admite filtros activos previos a la consulta de detalle.

##### Preconditions & Default Setup
* **Initial State** : Administrador autenticado con sesión activa en la plataforma.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `admin@seashare.com` (rol `ADMIN`) y `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Confirmación con motivo obligatorio y eliminación administrativa (Escenarios US2-1 a US2-4)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar botón "Eliminar" visible para Administrador | Iniciar sesión como `admin@seashare.com`, abrir "Ver Detalles" de una embarcación perteneciente a `propietario.test@seashare.com` | Embarcación ajena al admin en `AVAILABLE` | Se muestra el botón **"Eliminar"**, habilitado si está en `Disponible` o `En Mantenimiento/Limpieza` (FR-001, FR-002). | [ ] | [ ] |
| 2 | Abrir modal de confirmación y verificar campo de motivo | Pulsar **"Eliminar"** en "Ver Detalles" | Embarcación en `AVAILABLE` | Aparece el modal de confirmación con ícono de `X` en círculo rojo, título **"¿Eliminar embarcación?"**, textos de confirmación y advertencia en rojo, botones **"Conservar"** y **"Sí, eliminar"**, más el campo de motivo obligatorio (hasta 300 caracteres) (FR-005, Historial 11/11/2026). | [ ] | [ ] |
| 3 | Validar motivo obligatorio y cancelar con "Conservar" | Intentar enviar sin motivo o con más de 300 caracteres; luego pulsar **"Conservar"** | Motivo vacío / 301 caracteres; clic en "Conservar" | Sin motivo válido no se completa la eliminación; al pulsar **"Conservar"** se cierra el modal y el Administrador queda en "Ver Detalles" sin cambios (FR-006, Historial 11/11/2026). | [ ] | [ ] |
| 4 | Confirmar eliminación con motivo válido | Ingresar un motivo válido (≤ 300 caracteres) y pulsar **"Sí, eliminar"** | Motivo: `"Documentación vencida verificada en inspección"` | Se ejecuta `DELETE /api/v1/admin/fleet/boats/{boatId}` (`204`) y se muestra el modal **"¡Embarcación eliminada!"** con **"La embarcación ha sido eliminada del sistema."** y botón **"Aceptar"** (FR-007, FR-015). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Regreso a "Control General de Flota Fluvial" sin filtros (Escenario US2-5)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Regresar desde tabla con filtros activos | En "Control General de Flota Fluvial", aplicar filtros, abrir "Ver Detalles", eliminar la embarcación y pulsar **"Aceptar"** | Filtros activos previos en `/admin` | Redirige a **"Control General de Flota Fluvial"** en su estado por defecto **sin filtros**, donde la embarcación eliminada ya no aparece (FR-016, Decisión D18). | [ ] | [ ] |
| 2 | Regresar tras llegar por enlace directo | Abrir "Ver Detalles" de otra embarcación mediante enlace directo, eliminarla y pulsar **"Aceptar"** | URL directa `/admin/boats/:id` | Al pulsar **"Aceptar"** regresa a **"Control General de Flota Fluvial"** sin filtros (FR-016, Decisión D18). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Propietario intentando usar endpoint de administrador)** : Si un usuario con rol `OWNER` intenta invocar `DELETE /api/v1/admin/fleet/boats/{boatId}`, el servidor responde `403` y no elimina la embarcación ni genera notificación (Decisión D15, T108e).
* **EC-002 (Bloqueo por estado en eliminación administrativa)** : Si la embarcación está en `Reservado` o `En Navegación`, el botón "Eliminar" está deshabilitado para el Administrador y el servidor responde `409` con `current_status` si se intenta eliminar (FR-002, FR-023, Decisión D7).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |