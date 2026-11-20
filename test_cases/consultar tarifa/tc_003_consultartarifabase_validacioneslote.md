### Test Case Specification: Consultar Tarifa Base - Validaciones de Lote y Estructura de Entrada

**Created** : 2026-11-14  **Spec/Plan Link** : `consultar-tarifa-base-spec.md` / `consultar-tarifa-base-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-003-ConsultarTarifaBase-ValidacionesLote
**Product Name & Version** : SeaShare - Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : Delwin / QA Lead
**Priority** : P1 - Critical

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that `POST /api/v1/boats/base-rates` enforces strict input batch validation rules—rejecting the entire request with HTTP `400 Bad Request` and standard field-level error reporting (`boat_ids` or `boat_ids[i]`) whenever the batch is empty, exceeds 100 identifiers, or contains malformed identifiers, without returning partial rates.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Proveer Tarifa Base de Embarcaciones | Specification Document | `consultar-tarifa-base-spec.md` |
| Plan técnico: Consultar tarifa base | Plan Document | `consultar-tarifa-base-plan.md` |

#### Prerequisites & Environment

##### Test Case Limitations
* Endpoint is internal system-to-system (Module 3 consumer); validation responses follow the common REST API error schema.

##### Dependencies & Assumptions
* `BaseRateQueryService` and `BaseRatesRequest` DTO validation constraints are deployed.
* Valid identifier format rules defined in the system are active.

##### Preconditions & Default Setup
* **Initial State** : Backend service running with `BaseRateController` active; `vessel` table contains valid registered vessels to confirm no partial data is leaked on invalid batches.
* **Environment** : Backend API / Staging Environment (`POST /api/v1/boats/base-rates`)
* **Test Accounts** : None required (Unauthenticated internal endpoint per FR-011 / NFR-001).

#### Test Execution Flow  *(mandatory)*

##### Section 1: Validaciones de Tamaño del Lote (Scenario 9 & FR-009, NFR-002)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Enviar solicitud con lista `boat_ids` vacía | Send `POST` request to `/api/v1/boats/base-rates` with empty array | `{"boat_ids": []}` | HTTP `400 Bad Request`. Entire request is rejected with standard error format indicating field `boat_ids`. No `rates` array or partial data is returned. | [ ] | [ ] |
| 2 | Enviar solicitud con cuerpo nulo o campo `boat_ids` ausente | Send `POST` request to `/api/v1/boats/base-rates` with `{}` or `{"boat_ids": null}` | `{}` and `{"boat_ids": null}` | HTTP `400 Bad Request`. Request rejected indicating field `boat_ids` in the common error response. | [ ] | [ ] |
| 3 | Enviar solicitud con más de 100 embarcaciones (101 elementos) | Send `POST` request to `/api/v1/boats/base-rates` with 101 identifiers | `{"boat_ids": ["ID-001", ..., "ID-101"]}` (101 items) | HTTP `400 Bad Request`. Entire batch is rejected without processing any rate, reporting validation error on field `boat_ids` (exceeds max limit of 100 per NFR-002). | [ ] | [ ] |

##### Section 2: Validaciones de Formato de Identificador (Scenario 9 & FR-009)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Enviar lote con un identificador inválido mezclado con identificadores válidos | Send `POST` request to `/api/v1/boats/base-rates` where index `[1]` has an invalid format (e.g., blank string or invalid characters) while `[0]` and `[2]` are valid | `{"boat_ids": ["ID-REG-01", "   ", "ID-REG-02"]}` | HTTP `400 Bad Request`. Entire batch is rejected (zero partial rates delivered for `ID-REG-01` or `ID-REG-02`), and error response explicitly identifies `boat_ids[1]`. | [ ] | [ ] |
| 2 | Enviar lote con elemento nulo dentro del arreglo | Send `POST` request to `/api/v1/boats/base-rates` with `null` at index `[0]` | `{"boat_ids": [null, "ID-REG-01"]}` | HTTP `400 Bad Request`. Entire batch is rejected, reporting field `boat_ids[0]` in standard error format. | [ ] | [ ] |

##### Edge Cases & Boundary Conditions
* **EC-001** : Batch with 101 items where duplicates would reduce unique count to $\le 100$ -> Validation checks input batch size before deduplication and rejects the 101-item payload with HTTP `400` on `boat_ids`.
* **EC-002** : Batch with 100 valid items and 1 invalid-format item at index `[99]` -> Entire batch fails atomically with HTTP `400` pointing to `boat_ids[99]`.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |