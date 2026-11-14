### Test Case Specification: Consultar Tarifa Base - Flujo Principal y Estados Operativos
**Created** : 2026-11-14  **Spec/Plan Link** : `consultar-tarifa-base-spec.md` / `consultar-tarifa-base-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-001-ConsultarTarifaBase-FlujoPrincipal
**Product Name & Version** : SeaShare - Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that the internal batch base-rate query endpoint (`POST /api/v1/boats/base-rates`) functions successfully for registered vessels across all operational states, returning exact stored values without transformations, cache staleness, or extra vessel attributes, and without requiring authentication.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Proveer Tarifa Base de Embarcaciones | Specification Document | `consultar-tarifa-base-spec.md` |
| Plan técnico: Consultar tarifa base | Plan Document | `consultar-tarifa-base-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Endpoint is internal system-to-system (Module 3 consumer); no UI screen is involved (FR-011).
* Performance threshold (< 1 second for P95 with 100 boats, NFR-003) requires stable local/staging network conditions.

##### Dependencies & Assumptions
* Base vessels must already be registered or edited via "Registrar embarcación" and "Editar información de embarcación" flows.
* Database table `vessel` contains seeded records with mandatory positive integer `base_rate` values in COP.
* Endpoint `POST /api/v1/boats/base-rates` is publicly accessible (`permitAll`) without JWT or API Key (FR-011, D1).

##### Preconditions & Default Setup
* **Initial State** : Backend service running with `BaseRateController` and `BaseRateQueryService` active; `vessel` table seeded with boats in statuses: `Disponible`, `Reservado`, `En Navegación`, and `En Mantenimiento/Limpieza`.
* **Environment** : Backend API / Staging Environment (`POST /api/v1/boats/base-rates`)
* **Test Accounts** : None required (Unauthenticated internal endpoint per FR-011 / NFR-001).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Consulta Exitosa de Lote y Formato de Tarifa (Scenarios 1, 2, 5 & FR-001, FR-002, FR-003, FR-011)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar lote de múltiples embarcaciones registradas sin autenticación | Send `POST` request without `Authorization` header or API key to `/api/v1/boats/base-rates` | `{"boat_ids": ["ID-DISP-01", "ID-DISP-02", "ID-DISP-03"]}` (with stored `base_rate` = `350000`, `180000`, `500000`) | HTTP `200 OK`. Response contains `rates` with 3 items and `not_found: []`. Read-only operation modifies no DB records. | [ ] | [ ] |
| 2 | Verificar formato exacto de tarifa y ausencia de campos extra | Inspect JSON structure and field values of each object in `rates` | Response body from Step 1 | Each element has ONLY `boat_id` and `base_rate`. `base_rate` is string with two decimals (`"350000.00"`, `"180000.00"`, `"500000.00"`), no thousands separators, no currency symbol (`$`, `COP`), no surcharges, no periodicity, and no extra fields (status, capacity, owner, etc.). | [ ] | [ ] |
| 3 | Consultar lote con una sola embarcación registrada | Send `POST` request to `/api/v1/boats/base-rates` with 1 valid ID | `{"boat_ids": ["ID-DISP-02"]}` | HTTP `200 OK`. Response maintains identical contract: `{"rates": [{"boat_id": "ID-DISP-02", "base_rate": "180000.00"}], "not_found": []}`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Independecia del Estado Operativo y Lectura en Tiempo Real sin Caché (Scenarios 3, 4, 11 & FR-004, FR-005, NFR-004)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar embarcaciones en los 4 estados operativos | Send `POST` request to `/api/v1/boats/base-rates` including one boat in each valid operational state | `{"boat_ids": ["ID-DISPONIBLE", "ID-RESERVADO", "ID-NAVEGACION", "ID-MANTENIMIENTO"]}` | HTTP `200 OK`. All 4 boats are returned in `rates` with their exact stored `base_rate`; `not_found` is empty. Operational status neither alters nor excludes any rate. | [ ] | [ ] |
| 2 | Verificar actualización inmediata tras edición de tarifa (sin caché) | Query boat rate, update `base_rate` from `150000` to `180000` via "Editar información de embarcación", and immediately re-query `/api/v1/boats/base-rates` | `{"boat_ids": ["ID-EDIT-01"]}` (Initial `base_rate`: `150000` -> Updated `base_rate`: `180000`) | First query returns `"150000.00"`. Second query immediately returns `"180000.00"` matching the newly saved value in DB and UI ("Ver Detalles" / "Editar"), confirming zero stale cache. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Owner updates `base_rate` concurrently while Module 3 queries the endpoint -> Module 1 always returns the committed `vessel.base_rate` value at the exact instant of the query.
* **EC-002** : Querying a batch containing a single vessel vs. multiple vessels -> Both return the exact same JSON schema (`rates` array and `not_found` array).
* **EC-003** : UI visibility check -> Confirm no link, button, or view for `POST /api/v1/boats/base-rates` is exposed in Owner or Administrator screens (FR-011).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |