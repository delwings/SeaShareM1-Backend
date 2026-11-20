### Test Case Specification: Idempotencia, Descarte de Eventos Antiguos y Transiciones Inválidas
**Created** : 2026-11-14  **Spec/Plan Link** : `asignar-estado-operativo-spec.md` / `asignar-estado-operativo-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-002-IdempotenciaOrdenCronologico
**Product Name & Version** : SeaShare - Módulo 1 (Gestión de Flota y Activos P2P) v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica los mecanismos de resiliencia y consistencia de datos del sistema (`US-2`, `SC-002`), asegurando que: (1) los mensajes duplicados por `eventId` sean ignorados (`FR-004`), (2) los eventos desordenados con marca de tiempo anterior al último cambio de estado (`status_changed_at`) sean descartados (`FR-005`), y (3) las transiciones fuera de la matriz permitida o redundantes (mismo estado actual) sean ignoradas y confirmadas sin alterar la embarcación (`FR-009`), registrando el motivo de descarte (`DUPLICATE`, `STALE`, etc.).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Spec - Asignar Estado Operativo | Specification Document | `asignar-estado-operativo-spec.md` |
| Implementation Plan - Asignar Estado Operativo | Plan Document | `asignar-estado-operativo-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Las pruebas de concurrencia entre avisos automáticos y acciones manuales se validan mediante peticiones simultáneas sobre el endpoint REST y los endpoints manuales de mantenimiento (`D6`).

##### Dependencies & Assumptions
* `TC-001-TransicionesAutomaticasEstado` aprobado.
* Tablas `processed_event` y `vessel_status_history` operativas con validación atómica en `VesselPersistenceAdapter.changeStatusAutomatically` (`D6`).

##### Preconditions & Default Setup
* **Initial State** : Embarcación `VESSEL-003` en estado `AVAILABLE` con `status_changed_at = 2026-11-14T15:00:00Z`. Embarcación `VESSEL-004` puesta en estado `MAINTENANCE` de forma manual por un Administrador a las `2026-11-14T15:00:00Z`.
* **Environment** : Entorno local / QA Backend con acceso a logs de aplicación y base de datos (`processed_event`, `vessel`, `vessel_status_history`).
* **Test Accounts** : Acceso público al endpoint simulador.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Idempotencia ante Mensajes Duplicados (`FR-004`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Procesar primer aviso válido | Enviar petición `POST` al endpoint simulador para reservar `VESSEL-003`. | `eventId: "EVT-DUP-01"`, `vesselId: "VESSEL-003"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T15:10:00Z"` | Respuesta HTTP `200 OK`. `VESSEL-003` pasa a `RESERVED`. El identificador `"EVT-DUP-01"` queda registrado en la tabla `processed_event`. | [ ] | [ ] |
| 2 | Reenviar el mismo mensaje (duplicado exacto) | Enviar nuevamente la misma petición `POST` con idéntico `eventId`. | `eventId: "EVT-DUP-01"`, `vesselId: "VESSEL-003"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T15:10:00Z"` | Respuesta HTTP `200 OK` (confirmación silenciosa sin error grave) con motivo de descarte `DUPLICATE` (`D9`). No se duplica el registro en `vessel_status_history` y el estado se mantiene intacto (`FR-004`). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Protección contra Eventos Antiguos / Desordenados (`FR-005`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Enviar aviso con timestamp anterior al último cambio | Con `VESSEL-003` en estado `RESERVED` (`status_changed_at = 15:10:00Z`), enviar un aviso nuevo (`eventId` inédito) cuyo suceso ocurrió a las `14:00:00Z` (antes del último cambio). | `eventId: "EVT-OLD-01"`, `vesselId: "VESSEL-003"`, `status: "AVAILABLE"`, `occurredAt: "2026-11-14T14:00:00Z"` | Respuesta HTTP `200 OK` indicando descarte por evento antiguo (`STALE`, `D9`). El estado de `VESSEL-003` permanece en `RESERVED` (`FR-005`) y el evento descartado queda registrado en el log del sistema. | [ ] | [ ] |
| 2 | Validar comparación contra cambio manual reciente | Con `VESSEL-004` puesta manualmente en `MAINTENANCE` a las `15:00:00Z`, enviar un aviso automático fechado a las `14:59:59Z`. | `eventId: "EVT-OLD-02"`, `vesselId: "VESSEL-004"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T14:59:59Z"` | El sistema compara `occurredAt` contra el `status_changed_at` del cambio manual (`FR-005`), descarta el aviso como `STALE`, registra el evento en el log y mantiene `VESSEL-004` en `MAINTENANCE`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Transiciones No Permitidas y Estado Redundante (`FR-009`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Intentar transición prohibida (`AVAILABLE` → `IN_NAVIGATION`) | Enviar aviso de inicio de navegación directamente sobre una embarcación en estado `AVAILABLE` (sin pasar por `RESERVED`). | `eventId: "EVT-INV-01"`, `vesselId: "VESSEL-005"` (en `AVAILABLE`), `status: "IN_NAVIGATION"`, `occurredAt: "2026-11-14T16:00:00Z"` | Respuesta HTTP `200 OK` confirmando recepción. El sistema ignora la transición no permitida (`FR-009`), mantiene `VESSEL-005` en `AVAILABLE`, registra el descarte en el log y no inserta cambio en `vessel_status_history`. | [ ] | [ ] |
| 2 | Intentar transición sobre barco en `MAINTENANCE` | Con `VESSEL-004` en `MAINTENANCE` (por acción manual), enviar aviso posterior para pasarla a `RESERVED` o `AVAILABLE`. | `eventId: "EVT-INV-02"`, `vesselId: "VESSEL-004"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T16:05:00Z"` | Respuesta HTTP `200 OK`. La transición se ignora (`FR-009`), se registra en el log y `VESSEL-004` continúa bloqueada en `MAINTENANCE`. | [ ] | [ ] |
| 3 | Enviar aviso con el mismo estado que ya tiene el barco | Con `VESSEL-005` en `AVAILABLE`, enviar un aviso válido y reciente solicitando estado `AVAILABLE`. | `eventId: "EVT-SAME-01"`, `vesselId: "VESSEL-005"`, `status: "AVAILABLE"`, `occurredAt: "2026-11-14T16:10:00Z"` | Respuesta HTTP `200 OK`. El sistema no realiza modificaciones sobre la embarcación ni genera entrada redundante de cambio de estado (`FR-009`). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Condición de carrera / Concurrencia atómica - `D6`)** : Ejecutar de forma concurrente (en el mismo milisegundo) una acción manual de "Enviar a Mantenimiento" y un aviso automático de `AVAILABLE` → `RESERVED` sobre la misma embarcación. La sentencia condicional atómica en base de datos (`VesselPersistenceAdapter.changeStatusAutomatically`) debe garantizar que solo una operación prevalezca y que la otra sea descartada limpiamente sin inconsistencia entre `vessel.status` y `vessel_status_history` (`SC-002`).
* **EC-002 (Marca de tiempo exactamente igual a `status_changed_at`)** : Enviar un aviso válido cuya fecha/hora `occurredAt` sea exactamente igual al `status_changed_at` actual del barco (`D6`: sin cambio posterior); verificar que el sistema lo descarte para evitar sobrescrituras en el mismo instante.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |