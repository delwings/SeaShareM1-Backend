### Test Case Specification: Flujo Principal de Transiciones Automáticas de Estado Operativo
**Created** : 2026-11-14  **Spec/Plan Link** : `asignar-estado-operativo-spec.md` / `asignar-estado-operativo-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-001-TransicionesAutomaticasEstado
**Product Name & Version** : SeaShare - Módulo 1 (Gestión de Flota y Activos P2P) v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica que el sistema reciba, interprete y aplique correctamente las 5 transiciones automáticas permitidas de estado físico de una embarcación (`AVAILABLE`, `RESERVED`, `IN_NAVIGATION`/`NAVIGATION`, `MAINTENANCE_CLEANING`/`MAINTENANCE`) a partir de los avisos provenientes del Módulo 2 (Reservas), cumpliendo con los tiempos de respuesta menores a 500 ms (`SC-001`) y dejando sin origen de mantenimiento (`maintenance_by` nulo) los bloqueos automáticos (`FR-010`).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Spec - Asignar Estado Operativo | Specification Document | `asignar-estado-operativo-spec.md` |
| Implementation Plan - Asignar Estado Operativo | Plan Document | `asignar-estado-operativo-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* En la fase actual, la recepción desde RabbitMQ está reemplazada por el endpoint REST simulador (`StatusEventController`) que invoca directamente el caso de uso `AssignOperationalStatusUseCase` (`D1`, `D2`). El consumo asíncrono real de colas AMQP queda fuera del alcance de esta ejecución.

##### Dependencies & Assumptions
* La migración de base de datos `V10__operational_status_events.sql` debe estar ejecutada exitosamente (`vessel.status_changed_at`, `processed_event`, `vessel_status_history`).
* El caso de uso `AssignOperationalStatusService` y el controlador `StatusEventController` deben estar desplegados y activos.

##### Preconditions & Default Setup
* **Initial State** : Base de datos con al menos dos embarcaciones publicadas (no eliminadas, fuera de estado borrador): `VESSEL-001` en estado `AVAILABLE` y `VESSEL-002` en estado `AVAILABLE`.
* **Environment** : Entorno local / QA Backend (Spring Boot) con cliente HTTP (Postman) y acceso de lectura a la base de datos relacional (`vessel`, `processed_event`, `vessel_status_history`).
* **Test Accounts** : Endpoint simulador configurado como ruta pública (`SecurityConfig`); usuario `OWNER` propietario de `VESSEL-001` y `VESSEL-002` para verificación posterior de cierre de mantenimiento.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Ciclo Completo de Reserva, Navegación y Limpieza (Transiciones 1, 2 y 3)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Confirmación de pago (Disponible → Reservado) | Enviar petición `POST` al endpoint simulador (`StatusEventController`) con aviso de pago confirmado para `VESSEL-001`. | `eventId: "EVT-101"`, `vesselId: "VESSEL-001"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `200 OK` en < 500 ms (`SC-001`). El estado de `VESSEL-001` en tabla `vessel` cambia a `RESERVED` y `status_changed_at` se actualiza a `2026-11-14T10:00:00Z`. | [ ] | [ ] |
| 2 | Inicio de viaje con nomenclatura M2 (Reservado → En Navegación) | Enviar petición `POST` al endpoint simulador indicando abordaje con el nombre de estado enviado por M2 (`IN_NAVIGATION`). | `eventId: "EVT-102"`, `vesselId: "VESSEL-001"`, `status: "IN_NAVIGATION"`, `occurredAt: "2026-11-14T11:00:00Z"` | Respuesta HTTP `200 OK` en < 500 ms. El sistema reconoce la equivalencia (`FR-008`) y actualiza `VESSEL-001` a En Navegación (`NAVIGATION` / `IN_NAVIGATION`). | [ ] | [ ] |
| 3 | Fin de viaje con nomenclatura M2 (En Navegación → Mantenimiento) | Enviar petición `POST` al endpoint simulador indicando finalización del servicio con nombre M2 (`MAINTENANCE_CLEANING`). | `eventId: "EVT-103"`, `vesselId: "VESSEL-001"`, `status: "MAINTENANCE_CLEANING"`, `occurredAt: "2026-11-14T14:00:00Z"` | Respuesta HTTP `200 OK` en < 500 ms. El sistema reconoce la equivalencia (`FR-008`) y actualiza `VESSEL-001` a Mantenimiento (`MAINTENANCE`), bloqueándola en catálogo (`FR-001`). | [ ] | [ ] |
| 4 | Verificar ausencia de origen de mantenimiento (`maintenance_by`) | Consultar el registro de `VESSEL-001` en la base de datos e intentar finalizar el mantenimiento como Administrador vs. Propietario. | `vesselId: "VESSEL-001"` | El campo `maintenance_by` es `NULL` (`D7`, `FR-010`). El sistema impide que el Administrador cierre el mantenimiento y permite únicamente al Propietario terminarlo. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Cancelación por Daño y Cancelación Normal (Transiciones 4 y 5)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar embarcación en estado Reservado | Enviar petición `POST` al endpoint simulador para pasar `VESSEL-002` de `AVAILABLE` a `RESERVED`. | `eventId: "EVT-201"`, `vesselId: "VESSEL-002"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `200 OK`. `VESSEL-002` pasa a estado `RESERVED`. | [ ] | [ ] |
| 2 | Cancelación normal del cliente (Reservado → Disponible) | Enviar petición `POST` al endpoint simulador indicando cancelación normal del viaje (`AVAILABLE`). | `eventId: "EVT-202"`, `vesselId: "VESSEL-002"`, `status: "AVAILABLE"`, `occurredAt: "2026-11-14T10:30:00Z"` | Respuesta HTTP `200 OK` en < 500 ms. `VESSEL-002` vuelve a estado `AVAILABLE` y queda visible para alquiler en catálogo (`FR-009`). | [ ] | [ ] |
| 3 | Re-reservar y cancelar por daño (Reservado → Mantenimiento) | Enviar aviso a `RESERVED` (`EVT-203`) y posteriormente enviar aviso de cancelación por daño usando nomenclatura M1 (`MAINTENANCE`). | `eventId: "EVT-204"`, `vesselId: "VESSEL-002"`, `status: "MAINTENANCE"`, `occurredAt: "2026-11-14T12:00:00Z"` | Respuesta HTTP `200 OK` en < 500 ms. `VESSEL-002` pasa directamente de `RESERVED` a `MAINTENANCE` con `maintenance_by` nulo (`FR-008`, `FR-009`, `FR-010`). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Aislamiento de datos financieros - `FR-007` / `SC-003`)** : Enviar un payload válido de cambio de estado (ej. `AVAILABLE` → `RESERVED`) incluyendo campos adicionales no solicitados con montos de pago, tarifas o multas (`amount: 1500.00`, `currency: "USD"`, `penaltyFee: 200`). El sistema debe ignorar silenciosamente los datos financieros, aplicar únicamente el cambio de estado operativo y garantizar que cero información financiera se persista en la base de datos de Gestión de Flota.
* **EC-002 (Persistencia en cola ante caída del servicio)** : Una vez integrado el broker RabbitMQ (`seashare-rabbit`), detener el servicio backend de M1, publicar mensajes de cambio de estado en la cola y reiniciar el servicio; verificar que los mensajes se consumen en orden sin pérdida.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |