### Test Case Specification: Suspensión Administrativa con Motivo Obligatorio y Control Cruzado de Roles
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/enviar-mantenimiento/spec.md` | `docs/features/enviar-mantenimiento/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-002-SuspensionAdministrativaYControlRoles
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la suspensión de una embarcación por parte del Administrador (`ADMIN`) con motivo obligatorio (1 a 300 caracteres), la validación de motivo vacío (`400`), la generación del aviso `MAINTENANCE_NOTICE` al propietario, la regla de que solo termina el mantenimiento quien lo inició (`maintenance_by`), el bloqueo visual/funcional del botón para el otro rol (`409` con `current_status` y `maintenance_by`), la exposición de `maintenance_by` en el detalle y el aviso `VESSEL_AVAILABLE` al finalizar el administrador (US2-2, US3-1 a US3-3, US5-1, FR-004, FR-006, FR-009, FR-012 a FR-016, FR-018, FR-019, SC-002).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Enviar a Mantenimiento | Specification Document | `docs/features/enviar-mantenimiento/spec.md` |
| Implementation Plan: Enviar a Mantenimiento | Plan Document | `docs/features/enviar-mantenimiento/plan.md` |
| Contrato REST: Enviar a Mantenimiento | API Contract | `docs/architecture/contracts/rest/enviar-mantenimiento.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La acción `"Solicitar revisión"` del Propietario y la campana administrativa de solicitudes se prueban en `TC-003` (`FR-020` a `FR-022`).
* El botón `"Reportar nuevamente"` del Administrador (`Historia 5, escenario 2`) está pendiente en el plan y no se evalúa aquí.

##### Dependencies & Assumptions
* `AdminMaintenanceService`, `AdminFleetController`, `OwnerNotifier` y `AdminReason` operativos; `CompletedBoatResponse` expone `maintenance_by` (Decisiones D13 a D17).
* Usuarios semilla (clave `SeaShare2026!`): Administrador `1010000001` (`admin@seashare.com`, rol `ADMIN`) y Propietario `propietario.test@seashare.com` (rol `OWNER`) con embarcaciones en `Disponible` y `Mantenimiento`.

##### Preconditions & Default Setup
* **Initial State** : Sesiones disponibles de Administrador (`ADMIN`) y Propietario (`OWNER`).
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `admin@seashare.com` (`1010000001`, rol `ADMIN`) y `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Suspensión por Administrador y Validación de Motivo (Escenarios US3-1, US3-2)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Intentar suspender sin escribir motivo | Como `ADMIN`, abrir detalle de embarcación `Disponible`, pulsar **"Enviar a Mantenimiento"** y confirmar sin escribir motivo | Motivo vacío (`""` o solo espacios) | Se muestra **"Escribe el motivo del mantenimiento."**, no se envía la solicitud y el servidor responde `400` con campo `reason` (US3-2, FR-004, FR-015, Decisión D14). | [ ] | [ ] |
| 2 | Suspender embarcación como Administrador con motivo válido | Escribir un motivo de 1 a 300 caracteres (ej. `"Daño en guardabarros de babor"`) y confirmar **"Sí, enviar"** | Motivo válido (`1..300` chars), embarcación `Disponible` | El estado cambia a `"Mantenimiento"`, sale del catálogo público, `maintenance_by` queda como `ADMIN`, `maintenance_by` se expone en el detalle y se crea el aviso `MAINTENANCE_NOTICE` para el propietario con nombre, matrícula, fecha y motivo (US3-1, FR-003, FR-016, FR-019, Decisiones D13, D15, D17). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Control Cruzado de Roles y Finalización por Administrador (Escenarios US2-2, US3-3, US5-1)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Administrador observa embarcación en mantenimiento iniciado por Propietario o sistema | Como `ADMIN`, abrir "Ver Detalles" de una embarcación en `"Mantenimiento"` con `maintenance_by = OWNER` o `NULL` | Embarcación con `maintenance_by = OWNER` o `NULL` | El botón **"Terminar Mantenimiento"** aparece visible pero deshabilitado (cursor de bloqueo, sin tooltip); si se invoca el endpoint directamente, el servidor responde `409` con `current_status` y `maintenance_by` (US3-3, FR-009, FR-013, FR-014, Decisiones D6, D11, D12). | [ ] | [ ] |
| 2 | Propietario intenta terminar mantenimiento iniciado por Administrador | Como `OWNER`, intentar terminar mantenimiento de una embarcación con `maintenance_by = ADMIN` (p. ej. desde vista previa) | Embarcación con `maintenance_by = ADMIN` | El sistema bloquea la acción y muestra el modal **"Embarcación suspendida por el administrador"** con `"Esta embarcación fue suspendida por el administrador. Solo él puede habilitarla de nuevo."` (y `409` con `maintenance_by = ADMIN` en API) (US2-2, FR-009, FR-013, Decisión D3). | [ ] | [ ] |
| 3 | Administrador finaliza su propia suspensión | Como `ADMIN`, abrir detalle de embarcación con `maintenance_by = ADMIN` y pulsar **"Terminar mantenimiento"** -> **"Sí, terminar"** | Embarcación con `maintenance_by = ADMIN` | El estado vuelve a `"Disponible"`, `maintenance_by` se limpia y se genera el aviso `VESSEL_AVAILABLE` para el propietario indicando que ya puede recibir reservas (US5-1, FR-013, FR-018, Decisión D15). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Límites de longitud del motivo administrativo)** : Un motivo de 1 carácter es aceptado y uno de más de 300 caracteres es rechazado por validación (`400` con campo `reason`) (FR-015, Decisión D14).
* **EC-002 (Fallo al crear notificación tras cambio atómico)** : Si la creación de `MAINTENANCE_NOTICE` o `VESSEL_AVAILABLE` falla después del `UPDATE` atómico, el cambio de estado se conserva y no se generan avisos duplicados en reintentos (FR-016, Decisiones D15, D16).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |