### Test Case Specification: Trazabilidad y Auditoría del Historial de Cambios de Estado
**Created** : 2026-11-14  **Spec/Plan Link** : `asignar-estado-operativo-spec.md` / `asignar-estado-operativo-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-004-BitacoraHistorialEstados
**Product Name & Version** : SeaShare - Módulo 1 (Gestión de Flota y Activos P2P) v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica que el sistema registre una bitácora detallada e inmutable en `vessel_status_history` (`FR-006`) cada vez que el estado operativo de una embarcación cambia efectivamente, abarcando tanto los cambios automáticos por avisos aplicados (incluyendo el `eventId` asociado) como las acciones manuales de mantenimiento ("Enviar a Mantenimiento" y "Terminar mantenimiento") ejecutadas por los roles Propietario y Administrador (`D8`), excluyendo las transiciones iniciales de registro/publicación.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Spec - Asignar Estado Operativo | Specification Document | `asignar-estado-operativo-spec.md` |
| Implementation Plan - Asignar Estado Operativo | Plan Document | `asignar-estado-operativo-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Ninguna; la auditoría se verifica directamente contra la tabla `vessel_status_history` creada en la migración `V10__operational_status_events.sql`.

##### Dependencies & Assumptions
* `TC-001-TransicionesAutomaticasEstado` aprobado.
* Funcionalidades manuales de gestión de mantenimiento para roles Propietario y Administrador implementadas con registro de historial (`T178`).

##### Preconditions & Default Setup
* **Initial State** : Embarcación `VESSEL-HIST-01` recién publicada en estado `AVAILABLE` (sin registros previos de cambio operativo en `vessel_status_history`).
* **Environment** : Entorno local / QA Backend con cliente HTTP (Postman) y acceso a base de datos (`vessel_status_history`, `vessel`).
* **Test Accounts** : Usuario con rol `OWNER` (propietario de `VESSEL-HIST-01`) y usuario con rol `ADMIN`.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Registro de Cambios Automáticos y Manuales en `vessel_status_history` (`FR-006`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar ausencia de historial por publicación inicial | Consultar la tabla `vessel_status_history` para `VESSEL-HIST-01` tras pasar de borrador a `AVAILABLE`. | `vesselId: "VESSEL-HIST-01"` | No existen registros en `vessel_status_history` por la transición de registro (borrador → Disponible). | [ ] | [ ] |
| 2 | Registrar cambio automático en historial | Enviar aviso automático válido para pasar `VESSEL-HIST-01` de `AVAILABLE` a `RESERVED`, y consultar `vessel_status_history`. | `eventId: "EVT-HIST-01"`, `vesselId: "VESSEL-HIST-01"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T09:00:00Z"` | Se inserta 1 registro en `vessel_status_history` en la misma transacción (`D6`) con: estado anterior `AVAILABLE`, estado nuevo `RESERVED`, momento exacto, origen automático y referencia al identificador `"EVT-HIST-01"` (`FR-006`). | [ ] | [ ] |
| 3 | Registrar cambio manual de Propietario ("Terminar mantenimiento") | Tras llevar `VESSEL-HIST-01` a `MAINTENANCE` por aviso automático (`EVT-HIST-02`), ejecutar la acción manual "Terminar mantenimiento" autenticado como `OWNER`. | Usuario `OWNER`, `vesselId: "VESSEL-HIST-01"` | `VESSEL-HIST-01` pasa a `AVAILABLE`. Se inserta registro en `vessel_status_history` con: estado anterior `MAINTENANCE`, estado nuevo `AVAILABLE`, momento del cambio y origen manual ejecutado por el rol Propietario (`FR-006`, `D8`). | [ ] | [ ] |
| 4 | Registrar acciones manuales de Administrador ("Enviar" y "Terminar mantenimiento") | Autenticado como `ADMIN`, ejecutar "Enviar a Mantenimiento" sobre `VESSEL-HIST-01` (`AVAILABLE` → `MAINTENANCE`) y luego ejecutar "Terminar mantenimiento" (`MAINTENANCE` → `AVAILABLE`). | Usuario `ADMIN`, `vesselId: "VESSEL-HIST-01"` | Se insertan 2 registros consecutivos en `vessel_status_history` reflejando: (1) `AVAILABLE` → `MAINTENANCE` manual por Administrador, y (2) `MAINTENANCE` → `AVAILABLE` manual por Administrador, con sus respectivas marcas de tiempo (`FR-006`, `D8`). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Avisos ignorados no contaminan el historial - `FR-006` / `FR-009`)** : Enviar un aviso duplicado (`DUPLICATE`), un aviso antiguo (`STALE`) o una transición inválida sobre `VESSEL-HIST-01`; verificar que el conteo total de registros en `vessel_status_history` no aumente cuando no haya cambio efectivo de estado.
* **EC-002 (Atomicidad entre actualización de estado e historial - `D6`)** : Verificar que ante cualquier fallo en la inserción del historial, la transacción haga rollback y el estado en la tabla `vessel` no quede actualizado sin su correspondiente respaldo en `vessel_status_history`.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |