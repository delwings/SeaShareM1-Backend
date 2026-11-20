### Test Case Specification: Solicitud de Revisión del Propietario y Campana de Notificaciones
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/enviar-mantenimiento/spec.md` | `docs/features/enviar-mantenimiento/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-003-SolicitudRevisionYNotificaciones
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la lectura de avisos de suspensión (`MAINTENANCE_NOTICE`) en la campana del Propietario ("Revisar completamente" y "Detalle de Notificación"), el reemplazo de "Terminar mantenimiento" por "Solicitar revisión" cuando `maintenance_by = ADMIN`, los modales "¿Enviar a revisión?" y "Solicitud enviada", la campana del Administrador ("Notificaciones Administrativas", aviso `REVIEW_REQUEST` y "Ver detalles"), y la regla de uso único por mantenimiento persistida en `vessel.review_requested` (migración `V9`), con reinicio al iniciar/terminar mantenimiento e idempotencia sin avisos repetidos (US4-1, US4-2, FR-005, FR-007, FR-016, FR-017, FR-020 a FR-022, SC-003).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Enviar a Mantenimiento | Specification Document | `docs/features/enviar-mantenimiento/spec.md` |
| Implementation Plan: Enviar a Mantenimiento | Plan Document | `docs/features/enviar-mantenimiento/plan.md` |
| Contrato REST: Enviar a Mantenimiento | API Contract | `docs/architecture/contracts/rest/enviar-mantenimiento.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La acción `"Reportar nuevamente"` del Administrador (`Historia 5, escenario 2`) está fuera del alcance de esta entrega.
* El nombre de la embarcación en la campana se extrae del texto del aviso (Plan, Decisión D18).

##### Dependencies & Assumptions
* Migración `V9__vessel_review_requested.sql` aplicada (`vessel.review_requested` y `CompletedBoatResponse.review_requested`); `RequestMaintenanceReviewService`, `POST .../maintenance/request-review` y `NotificationController` admitiendo `OWNER` y `ADMIN` con `vessel_id` en `NotificationResponse` (Decisiones D19 a D22).
* Embarcación en estado `"Mantenimiento"` con `maintenance_by = ADMIN` y `review_requested = false`.

##### Preconditions & Default Setup
* **Initial State** : Propietario (`OWNER`) con aviso `MAINTENANCE_NOTICE` pendiente y Administrador (`ADMIN`) autenticado.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`) y `admin@seashare.com` (`1010000001`, rol `ADMIN`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Lectura de Aviso por Propietario y Solicitud de Revisión (Escenarios US4-1, US4-2)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Leer aviso de suspensión en la campana del Propietario | Como `OWNER`, abrir campana de notificaciones y pulsar **"Revisar completamente"** en el aviso `MAINTENANCE_NOTICE` | Aviso `MAINTENANCE_NOTICE` activo | En la campana se ve el nombre de la embarcación y `"El administrador ha enviado una notificación sobre el estado de la embarcación."`; al pulsar **"Revisar completamente"** abre **"Detalle de Notificación"** con la embarcación, su estado, el motivo registrado por administración y el botón **"Entendido y cerrar"** (US4-1, FR-016, FR-017). | [ ] | [ ] |
| 2 | Solicitar revisión desde embarcación suspendida por Administrador | Como `OWNER`, abrir "Ver Detalles" de su embarcación con `maintenance_by = ADMIN`, pulsar **"Solicitar revisión"** y confirmar **"Sí, enviar"** | Embarcación con `maintenance_by = ADMIN` | En lugar de `"Terminar mantenimiento"` aparece **"Solicitar revisión"`; al pulsar abre **"¿Enviar a revisión?"** (`"Se enviará una notificación al administrador."`, `"Cancelar"`, `"Sí, enviar"`); al confirmar (`POST .../maintenance/request-review`), la embarcación sigue `"Mantenimiento"`, aparece **"Solicitud enviada"** y se envía un aviso `REVIEW_REQUEST` a cada Administrador (US4-2, FR-020, Decisiones D19, D20). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Campana del Administrador, Uso Único (`V9`) y Reinicio de Marca (FR-020 a FR-022)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Administrador revisa solicitud desde "Notificaciones Administrativas" | Como `ADMIN`, abrir campana (**"Notificaciones Administrativas"**) y pulsar **"Ver detalles"** en el aviso `REVIEW_REQUEST` | Aviso `REVIEW_REQUEST` con `vessel_id` | Muestra el nombre de la embarcación y `"El propietario ha solicitado la revisión de la embarcación."`; al pulsar **"Ver detalles"**, marca el aviso como leído y navega al detalle normal de la embarcación (FR-021, Decisiones D18, D21). | [ ] | [ ] |
| 2 | Verificar uso único de "Solicitar revisión" y persistencia `V9` | Como `OWNER`, recargar la página o abrir otra pestaña sobre esa embarcación y volver a intentar solicitar revisión | `review_requested = true` en `vessel` | El botón **"Solicitar revisión"** queda deshabilitado (cursor de bloqueo, sin tooltip); repetir la llamada al servidor no crea avisos duplicados (FR-020, FR-022, Decisiones D20, D22). | [ ] | [ ] |
| 3 | Verificar reinicio de `review_requested` al iniciar o terminar mantenimiento | Iniciar un nuevo mantenimiento o finalizar el mantenimiento actual y volver a suspender por `ADMIN` | Ciclo de mantenimiento | `vessel.review_requested` se reinicia a `false` al iniciar/terminar mantenimiento, permitiendo una nueva solicitud de revisión en un mantenimiento posterior (Plan `T155`, Decisión D22). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Solicitar revisión cuando `maintenance_by` no es `ADMIN`)** : Si se intenta solicitar revisión sobre una embarcación cuyo mantenimiento no fue iniciado por el Administrador, el servidor responde `409` (FR-022).
* **EC-002 (Idempotencia ante múltiples administradores)** : Si existen varios administradores activos, cada uno recibe su `REVIEW_REQUEST` correspondiente, pero repetir la acción por el propietario no duplica notificaciones (FR-020, Decisión D20).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |