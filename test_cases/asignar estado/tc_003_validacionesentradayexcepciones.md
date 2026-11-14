### Test Case Specification: Validaciones de Contrato de Entrada y Manejo de Embarcaciones Inexistentes
**Created** : 2026-11-14  **Spec/Plan Link** : `asignar-estado-operativo-spec.md` / `asignar-estado-operativo-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-003-ValidacionesEntradaYExcepciones
**Product Name & Version** : SeaShare - Módulo 1 (Gestión de Flota y Activos P2P) v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica que el sistema valide estrictamente la estructura y valores permitidos de los avisos entrantes (`FR-003`, `FR-008`), rechazando con error HTTP `400 Bad Request` e identificando el campo inválido cuando el mensaje esté incompleto o contenga estados desconocidos; y que gestione adecuadamente avisos dirigidos a embarcaciones inexistentes, eliminadas o no publicadas (`Edge Cases`).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Spec - Asignar Estado Operativo | Specification Document | `asignar-estado-operativo-spec.md` |
| Implementation Plan - Asignar Estado Operativo | Plan Document | `asignar-estado-operativo-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Las validaciones de estructura se prueban mediante el DTO `StatusEventRequest` expuesto en `StatusEventController`.

##### Dependencies & Assumptions
* `StatusEventController` y `AssignOperationalStatusService` activos con validación de campos obligatorios y catálogo de estados.

##### Preconditions & Default Setup
* **Initial State** : Base de datos sin registro para `VESSEL-999` (inexistente), con `VESSEL-DRAFT` en estado borrador (no publicada) y `VESSEL-DELETED` marcada como eliminada (`soft-deleted`).
* **Environment** : Entorno local / QA Backend con acceso a logs de aplicación y base de datos.
* **Test Accounts** : Ruta pública del simulador habilitada.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Validación de Campos Obligatorios y Valores de Estado (`FR-003`, `FR-008`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Enviar aviso sin `eventId` (identificador único) | Enviar petición `POST` al endpoint simulador omitiendo o enviando vacío el campo `eventId`. | `eventId: ""`, `vesselId: "VESSEL-001"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `400 Bad Request` indicando explícitamente que el campo `eventId` es obligatorio/inválido (`FR-003`, `FR-008`). | [ ] | [ ] |
| 2 | Enviar aviso sin `vesselId` (barco afectado) | Enviar petición `POST` al endpoint simulador omitiendo el campo `vesselId`. | `eventId: "EVT-VAL-01"`, `vesselId: null`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `400 Bad Request` indicando el error de validación en el campo `vesselId` (`FR-003`, `FR-008`). | [ ] | [ ] |
| 3 | Enviar aviso sin `occurredAt` (fecha y hora del suceso) | Enviar petición `POST` al endpoint simulador omitiendo o con formato inválido en `occurredAt`. | `eventId: "EVT-VAL-02"`, `vesselId: "VESSEL-001"`, `status: "RESERVED"`, `occurredAt: null` | Respuesta HTTP `400 Bad Request` indicando el error de validación en el campo `occurredAt` (`FR-003`, `FR-008`). | [ ] | [ ] |
| 4 | Enviar aviso sin `status` o con valor no reconocido | Enviar petición `POST` al endpoint simulador con un estado fuera de la lista permitida (`AVAILABLE`, `RESERVED`, `IN_NAVIGATION`, `NAVIGATION`, `MAINTENANCE_CLEANING`, `MAINTENANCE`). | `eventId: "EVT-VAL-03"`, `vesselId: "VESSEL-001"`, `status: "CANCELLED_BY_USER"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `400 Bad Request` indicando que el campo `status` tiene un valor inválido (`FR-008`). No se registra el evento en `processed_event`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Manejo de Embarcación Inexistente, No Publicada o Eliminada (`Edge Cases`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Enviar aviso para una embarcación que no existe | Enviar petición `POST` válida con un `vesselId` inexistente en la base de datos. | `eventId: "EVT-NOEXIST-01"`, `vesselId: "VESSEL-999"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `200 OK` (confirma recepción para evitar reintentos infinitos en cola). El sistema ignora el cambio y guarda un reporte/registro del error en el log técnico. | [ ] | [ ] |
| 2 | Enviar aviso para embarcación en borrador o eliminada | Enviar petición `POST` válida apuntando a `VESSEL-DRAFT` (no publicada) y otra apuntando a `VESSEL-DELETED` (eliminada). | `eventId: "EVT-DRAFT-01"`, `vesselId: "VESSEL-DRAFT"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `200 OK` confirmando recepción. El sistema ignora el cambio (`D6`), registra el evento en el log técnico y no altera el estado de la embarcación. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Validación de todos los alias de estado válidos - `FR-008`)** : Verificar que los 6 literales exactos (`AVAILABLE`, `RESERVED`, `IN_NAVIGATION`, `NAVIGATION`, `MAINTENANCE_CLEANING`, `MAINTENANCE`) superen la validación de entrada sin arrojar error `400`.
* **EC-002 (Payload JSON malformado o cuerpo vacío)** : Enviar un cuerpo vacío `{}` o JSON sintácticamente inválido al endpoint simulador; verificar que el sistema responda con `400 Bad Request` sin exponer trazas internas ni afectar la disponibilidad del servicio.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |