### Test Case Specification: Consultar Tarifa Base - Embarcaciones No Encontradas y Deduplicación

**Created** : 2026-11-14  **Spec/Plan Link** : `consultar-tarifa-base-spec.md` / `consultar-tarifa-base-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-002-ConsultarTarifaBase-NoEncontradasYDeduplicacion
**Product Name & Version** : SeaShare - Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : Delwin / QA Lead
**Priority** : P1 - Critical

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that `POST /api/v1/boats/base-rates` accurately isolates non-registered vessels (non-existent, soft-deleted, or `Borrador` status) into the `not_found` list without exposing internal reasons or inventing default values (`0`), and that it deduplicates repeated identifiers (case-insensitively) while preserving request arrival order.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Proveer Tarifa Base de Embarcaciones | Specification Document | `consultar-tarifa-base-spec.md` |
| Plan técnico: Consultar tarifa base | Plan Document | `consultar-tarifa-base-plan.md` |

#### Prerequisites & Environment

##### Test Case Limitations
* Endpoint is internal system-to-system (Module 3 consumer); no UI screen is involved (FR-011).

##### Dependencies & Assumptions
* `TC-001-ConsultarTarifaBase-FlujoPrincipal` passes.
* Database table `vessel` contains seeded records representing:
  * Active registered vessels (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`).
  * A vessel in `Borrador` state.
  * A soft-deleted/eliminated vessel.
* Application logs are accessible to verify traceability of `not_found` items (NFR-005).

##### Preconditions & Default Setup
* **Initial State** : Backend service running with `BaseRateController` and `BaseRateQueryService` active; database seeded with active, draft (`Borrador`), and deleted vessels.
* **Environment** : Backend API / Staging Environment (`POST /api/v1/boats/base-rates`)
* **Test Accounts** : None required (Unauthenticated internal endpoint per FR-011 / NFR-001).

#### Test Execution Flow  *(mandatory)*

##### Section 1: Lote Mixto y Totalmente No Encontrado (Scenarios 6, 7 & FR-006, FR-007, NFR-005)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar lote mixto con embarcaciones registradas, en Borrador, eliminadas e inexistentes | Send `POST` request to `/api/v1/boats/base-rates` combining 4 valid registered boats and 3 non-consultable boats | `{"boat_ids": ["ID-REG-01", "ID-BORRADOR-01", "ID-REG-02", "ID-ELIMINADA-01", "ID-REG-03", "ID-INEXISTENTE-99", "ID-REG-04"]}` | HTTP `200 OK`. `rates` contains the 4 registered boats with exact rates; `not_found` contains `["ID-BORRADOR-01", "ID-ELIMINADA-01", "ID-INEXISTENTE-99"]`. Each valid input ID appears in **exactly one** list (SC-007). | [ ] | [ ] |
| 2 | Verificar ausencia de motivos y de valores inventados en `not_found` | Inspect structure of `not_found` array and system logs | Response body and application logs from Step 1 | `not_found` is a flat array of strings (identifiers only), treating `Borrador`, deleted, and non-existent identically with no reason codes, no `0` rates, and no default values (FR-006). System logs record the `not_found` IDs for diagnostics without leaking data of other vessels (NFR-005). | [ ] | [ ] |
| 3 | Consultar lote donde ninguna embarcación es encontrada | Send `POST` request to `/api/v1/boats/base-rates` where all IDs are non-existent, deleted, or `Borrador` | `{"boat_ids": ["ID-BORRADOR-01", "ID-ELIMINADA-01", "ID-INEXISTENTE-99"]}` | HTTP `200 OK`. `rates` is empty `[]`, and `not_found` contains all 3 identifiers `["ID-BORRADOR-01", "ID-ELIMINADA-01", "ID-INEXISTENTE-99"]`. | [ ] | [ ] |

##### Section 2: Deduplicación, Sensibilidad a Mayúsculas/Minúsculas y Orden (Scenario 8 & FR-008)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar lote con identificadores duplicados exactos y con distinta capitalización | Send `POST` request to `/api/v1/boats/base-rates` with repeated IDs in mixed case | `{"boat_ids": ["id-reg-01", "ID-REG-02", "ID-REG-01", "id-inexistente-99", "ID-INEXISTENTE-99"]}` | HTTP `200 OK`. System compares IDs case-insensitively, deduplicates them, and preserves arrival order: `rates` has 2 unique entries (`id-reg-01`, `ID-REG-02`) and `not_found` has 1 unique entry (`id-inexistente-99`). | [ ] | [ ] |

##### Edge Cases & Boundary Conditions
* **EC-001** : A vessel transitions from `Borrador` to `Disponible` between two consecutive queries -> First query returns the ID in `not_found`; second query returns the ID in `rates` with its stored `base_rate`.
* **EC-002** : A registered vessel is deleted after an initial query -> Subsequent query removes it from `rates` and places it in `not_found` without returning `0` or throwing an error.

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |