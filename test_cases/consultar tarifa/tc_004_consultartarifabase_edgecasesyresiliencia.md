### Test Case Specification: Consultar Tarifa Base - Edge Cases, Rendimiento y Resiliencia

**Created** : 2026-11-14  **Spec/Plan Link** : `consultar-tarifa-base-spec.md` / `consultar-tarifa-base-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-004-ConsultarTarifaBase-EdgeCasesYResiliencia
**Product Name & Version** : SeaShare - Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : Delwin / QA Lead
**Priority** : P2 - High

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies boundary conditions, technical failure resilience (atomic error handling without partial or invented rates), SLA performance compliance ($< 1\text{ s}$ at P95 for 100-boat batches), and architectural isolation boundaries (no Module 2 direct access or UI exposure).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Proveer Tarifa Base de Embarcaciones | Specification Document | `consultar-tarifa-base-spec.md` |
| Plan técnico: Consultar tarifa base | Plan Document | `consultar-tarifa-base-plan.md` |

#### Prerequisites & Environment

##### Test Case Limitations
* Technical fault injection (e.g., database connection drop) requires controlled staging/test environment access or fault-injection proxy.
* Latency benchmark (NFR-003) must be executed under normal network conditions without artificial throttling.

##### Dependencies & Assumptions
* Database table `vessel` contains at least 100 registered vessels for maximum-boundary and performance testing.
* Application error logging is enabled for NFR-005 verification.

##### Preconditions & Default Setup
* **Initial State** : Backend service running; 100 registered vessels seeded in `vessel` table; performance testing client (e.g., Postman Runner / k6 / JMeter) configured.
* **Environment** : Backend API / Staging Environment (`POST /api/v1/boats/base-rates`)
* **Test Accounts** : None required (Unauthenticated internal endpoint per FR-011 / NFR-001).

#### Test Execution Flow  *(mandatory)*

##### Section 1: Condiciones Límite de Tamaño de Lote y Rendimiento SLA (NFR-002, NFR-003, SC-001)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar lote en el límite máximo permitido (exactamente 100 embarcaciones) | Send `POST` request to `/api/v1/boats/base-rates` with 100 valid registered IDs | `{"boat_ids": ["ID-001", ..., "ID-100"]}` (100 unique registered IDs) | HTTP `200 OK`. All 100 rates are returned accurately in `rates` with `not_found: []`. | [ ] | [ ] |
| 2 | Validar cumplimiento de SLA de rendimiento (P95 < 1 segundo con 100 embarcaciones) | Execute 100 consecutive/concurrent batch requests of 100 vessels each to `/api/v1/boats/base-rates` under normal network conditions | 100 requests x 100 `boat_ids` payload | At least 95 out of 100 requests (P95) complete with HTTP `200 OK` in strictly less than $1\text{ second}$ ($< 1000\text{ ms}$) (NFR-003, SC-001). | [ ] | [ ] |

##### Section 2: Resiliencia ante Fallas Técnicas y Atomicidad (Scenario 10 & FR-010, NFR-005)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular falla técnica de base de datos durante consulta de lote válido | Simulate DB connection timeout / repository exception in `VesselRepositoryPort.findRegisteredByIds` while sending a valid batch | `{"boat_ids": ["ID-REG-01", "ID-REG-02", "ID-REG-03"]}` | HTTP `5xx` error returned for the **entire batch**. Zero partial rates and zero invented/default values are delivered (FR-010). | [ ] | [ ] |
| 2 | Verificar trazabilidad del error técnico en logs del sistema | Inspect backend diagnostic logs following Step 1 failure | System logs generated during Step 1 | Technical error is logged for diagnostics without exposing sensitive data of unrelated vessels (NFR-005). | [ ] | [ ] |

##### Edge Cases & Boundary Conditions
* **EC-001** : Concurrent rate update by Owner during high-frequency batch queries -> Queries never deadlock or fail; each response reflects the exact committed `vessel.base_rate` at query execution time.
* **EC-002** : Boundary transition between 100 items (valid) and 101 items (invalid) -> 100 items returns HTTP `200 OK`; 101 items returns HTTP `400 Bad Request`.
* **EC-003** : Batch containing 100 identical duplicates of a single registered boat -> Accepted (size $\le 100$), deduplicated, and returns a single item in `rates` and `not_found: []`.
* **EC-004** : Malformed JSON payload (e.g., syntax error or wrong data type like `"boat_ids": "ID-01"`) -> Rejected cleanly with HTTP `400 Bad Request` without unhandled 500 stack traces.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |