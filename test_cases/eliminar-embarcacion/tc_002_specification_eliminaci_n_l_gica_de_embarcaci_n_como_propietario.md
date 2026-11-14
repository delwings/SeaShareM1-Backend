### Test Case Specification: Eliminación Lógica de Embarcación como Propietario
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/eliminar-embarcacion/spec.md` | `docs/features/eliminar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-002-EliminacionPropietario
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el flujo completo de eliminación lógica de una embarcación propia desde "Ver Detalles" por parte del Propietario: apertura y textos exactos del modal de confirmación, cancelación mediante "Conservar", confirmación mediante "Sí, eliminar", modal de éxito, regreso a "Mis Embarcaciones Registradas", invisibilidad en listas/detalle/enlace directo, liberación de matrícula legal (`active_legal_registration` tras migración V7), conservación de fotografía/servicios en el registro eliminado y ausencia de notificación al propietario (US1, FR-005 a FR-016, FR-018, NFR-002, SC-002 a SC-004).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Eliminar Embarcación | Specification Document | `docs/features/eliminar-embarcacion/spec.md` |
| Implementation Plan: Eliminar Embarcación | Plan Document | `docs/features/eliminar-embarcacion/plan.md` |
| Contrato REST: Eliminar Embarcación | API Contract | `docs/architecture/contracts/rest/eliminar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La eliminación es lógica: el registro se conserva en MySQL con `deleted = TRUE`, `deleted_at` y `deleted_by`, pero no existe pantalla de historial ni restauración (FR-009, FR-010, [S-10]).
* No se generan notificaciones cuando el propio propietario elimina su embarcación (FR-018).

##### Dependencies & Assumptions
* Migración V7 aplicada (`deleted_at`, `deleted_by`, columna generada `active_legal_registration` e índice único `uq_vessel_active_legal_registration`) y `DELETE /api/v1/fleet/boats/{boatId}` operativo.
* En base de datos existe al menos una embarcación propia en estado `AVAILABLE` (ej. nombre `"Brisa del Mar"`, matrícula `"CP-05-1042-A"`) y otra en `MAINTENANCE`, con fotografía oficial y servicios asociados.

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado ubicado en "Ver Detalles" de una embarcación propia en `Disponible` o `En Mantenimiento/Limpieza`.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Modal de confirmación, conservación y eliminación exitosa (Escenarios US1-1 a US1-5)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Abrir modal de confirmación desde Ver Detalles | En "Ver Detalles" de embarcación propia en `Disponible`, pulsar **"Eliminar"** | Embarcación propia en `AVAILABLE` | Se despliega el modal de confirmación con ícono de `X` en círculo rojo, título **"¿Eliminar embarcación?"**, texto **"Esta acción eliminará el registro de forma permanente y liberará su matrícula asociada en el sistema."**, línea en rojo **"Esta acción no se puede deshacer."** y botones **"Conservar"** y **"Sí, eliminar"** (FR-005). | [ ] | [ ] |
| 2 | Cancelar eliminación con "Conservar" | Pulsar **"Conservar"** en el modal de confirmación | N/A | El modal se cierra, el propietario permanece en "Ver Detalles" de la misma embarcación y no se modifica ningún dato en BD ni se envía petición `DELETE` (FR-006, SC-002). | [ ] | [ ] |
| 3 | Confirmar eliminación con "Sí, eliminar" | Volver a abrir el modal y pulsar **"Sí, eliminar"** | Embarcación `"Brisa del Mar"` (`"CP-05-1042-A"`) | Se ejecuta `DELETE /api/v1/fleet/boats/{boatId}` (`204`) y aparece el modal **"¡Embarcación eliminada!"** con el texto **"La embarcación ha sido eliminada del sistema."** y botón **"Aceptar"** (FR-007, FR-015). | [ ] | [ ] |
| 4 | Regresar a la lista y verificar desaparición | Pulsar **"Aceptar"** en el modal "¡Embarcación eliminada!" | N/A | Cierra el modal y redirige a **"Mis Embarcaciones Registradas"**, donde la embarcación eliminada ya no aparece (FR-009, FR-016). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Enlace directo, liberación de matrícula y ausencia de notificación (Escenarios US1-6 a US1-8)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar la embarcación eliminada por enlace directo | Abrir la URL directa de "Ver Detalles" (o de "Editar") de la embarcación recién eliminada | URL de `boatId` eliminado | El sistema la trata como inexistente y muestra la pantalla de error de "Ver Detalles" (FR-009, FR-014). | [ ] | [ ] |
| 2 | Reutilizar la matrícula liberada en un registro nuevo | Iniciar "Registrar Nueva Embarcación" e ingresar la matrícula de la embarcación eliminada | Matrícula `"CP-05-1042-A"` | El sistema acepta la matrícula sin error de duplicidad, ya que la unicidad aplica solo a embarcaciones no eliminadas (FR-012, SC-004, Decisión D1/D8). | [ ] | [ ] |
| 3 | Verificar ausencia de notificación al propietario | Revisar la tabla `notification` / campana de avisos tras la eliminación hecha por el propietario | `propietario.test@seashare.com` | No se genera ninguna notificación para el propietario (FR-018, SC-005). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Conservación de datos y marca de eliminada en BD)** : En la tabla `vessel`, el registro permanece con `deleted = TRUE`, `deleted_at`, `deleted_by`, matrícula activa en `NULL` (`active_legal_registration = NULL`) y fotografía/servicios conservados en el registro, sin agregar "Eliminada" al diccionario de estados operativos (FR-009 a FR-013, [S-03], [S-04], [S-13]).
* **EC-002 (Eliminación en estado En Mantenimiento/Limpieza)** : Repetir el flujo con una embarcación propia en `En Mantenimiento/Limpieza` (`MAINTENANCE`) debe comportarse de forma idéntica (`204`, desaparición de lista, liberación de matrícula) (FR-002, FR-007).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |