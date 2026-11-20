### Test Case Specification: Navegación (Volver), Carga, Errores, Sesión y Control de Acceso
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-informacion-propietario/spec.md` | `docs/features/consultar-informacion-propietario/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-003-NavegacionCargaErroresAcceso
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el regreso mediante el botón "Volver" al estado por defecto de "Control General de Flota Fluvial" (sin filtros, barra vacía y ventana cerrada), el esqueleto de carga inicial, la consulta fresca al servidor sin reutilizar datos previos, el manejo de fallos de red/servidor ("Reintentar" y "Volver"), el tratamiento uniforme como no encontrado (`404`, incluyendo `400` por UUID inválido) solo con "Volver", la redirección por sesión no válida y el bloqueo de acceso (`403`) para usuarios sin rol `ADMIN` (US3, FR-023, FR-025 a FR-031, NFR-001, NFR-003, SC-006 a SC-008).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Información de Propietario | Specification Document | `docs/features/consultar-informacion-propietario/spec.md` |
| Implementation Plan: Consultar Información de Propietario | Plan Document | `docs/features/consultar-informacion-propietario/plan.md` |
| Contrato REST: Consultar Información de Propietario | API Contract | `docs/architecture/contracts/rest/consultar-informacion-propietario.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* El aviso específico `"Tu sesión ha expirado. Por favor, ingresa de nuevo."` y el guardado del destino de retorno al vencer la sesión (`FR-030`, `T081`) están pendientes como mejora transversal común a todas las pantallas; actualmente el sistema redirige al inicio de sesión sin mostrar el error de red.
* La auditoría de consultas (`FR-032`) está sin implementar (Decisión D8).

##### Dependencies & Assumptions
* Backend (`SeaShareM1`) y Frontend (`SeaShareM1-Frontend`) operativos; `GET /api/v1/admin/owners/{ownerId}` protegido con `@PreAuthorize("hasRole('ADMIN')")` y respuestas de error RFC 9457.
* DevTools disponible para simular modo *Offline* / error `500`, latencia y expiración o ausencia de token JWT.

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado (`ADMIN` o `OWNER`) con sesión activa.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `admin@seashare.com` (usuario `1010000001`, rol `ADMIN`) y `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Regreso con "Volver", esqueleto de carga y consulta fresca al servidor (Escenarios US3-1 a US3-3)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Volver a "Control General de Flota Fluvial" sin filtros | En `/admin`, aplicar filtros a la tabla, buscar un propietario, abrir "Ver Detalles" y pulsar **"Volver"** | Filtros activos en `/admin`, `ownerId` válido | Regresa a **"Control General de Flota Fluvial"** en su estado por defecto: barra de búsqueda vacía, ventana de resultados cerrada y tabla sin filtros (FR-025, SC-006, Decisión D7). | [ ] | [ ] |
| 2 | Verificar esqueleto de carga inicial | Pulsar **"Ver Detalles"** en un resultado simulando latencia en la consulta | `ownerId` válido con latencia | Muestra estado de carga tipo esqueleto (sin spinners ni indicadores llamativos) antes de pintar los datos (FR-026, NFR-002). | [ ] | [ ] |
| 3 | Verificar consulta fresca al servidor | Abrir "Información del Propietario", modificar un dato del propietario en BD y volver a abrir su pantalla | `ownerId` modificado en BD | La pantalla realiza una nueva consulta `GET /api/v1/admin/owners/{ownerId}` y muestra los datos actualizados del servidor, no datos cacheados ni de la ventana de búsqueda (FR-027). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Fallos de red/servidor, 404/UUID inválido, sesión y acceso no autorizado (Escenarios US3-4 a US3-8)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular fallo de red o servidor (5xx) al cargar | Activar modo *Offline* o forzar `500` al abrir `/admin/owners/:ownerId`; pulsar **"Reintentar"** y luego **"Volver"** | Modo *Offline* / `500` | Muestra en texto plano **"No se pudo cargar la información del propietario"** con botones **"Reintentar"** y **"Volver"**; **"Reintentar"** vuelve al esqueleto de carga y repite la consulta sin límite de reintentos (FR-028, [S-14]). | [ ] | [ ] |
| 2 | Abrir propietario inexistente, UUID inválido o ID de administrador | Navegar a `/admin/owners/:ownerId` con `ownerId` inexistente (`404`), UUID mal formado (`400` en backend) o ID de `admin@seashare.com` | UUID inexistente, `"no-es-uuid"`, `1010000001` | El servidor responde `404` (tratando `400` por UUID inválido como `404` en la UI) y se muestra **"No se pudo cargar la información del propietario"** únicamente con el botón **"Volver"** (FR-023, FR-029, Decisión D6). | [ ] | [ ] |
| 3 | Simular sesión expirada antes, durante o con la pantalla abierta | Forzar `401` al cargar, al reintentar o al pulsar **"Volver"** en `/admin/owners/:ownerId` | `401 Unauthorized` | Redirige al inicio de sesión sin mostrar el mensaje de error de red (FR-030, SC-008). | [ ] | [ ] |
| 4 | Intentar acceder sin rol de Administrador | Iniciar sesión como `propietario.test@seashare.com` y abrir directamente `/admin/owners/:ownerId` | Sesión `OWNER`, `ownerId` válido | El servidor responde `403`, no se carga ningún dato del propietario y se muestra la pantalla de acceso no autorizado con su botón hacia la pantalla principal del usuario (FR-031, NFR-001, SC-007, [S-15]). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Administrador buscando o abriéndose a sí mismo)** : Buscar el nombre o documento de `1010000001` no devuelve resultados y abrir `/admin/owners/1010000001` muestra el estado de no encontrado con único botón `"Volver"` (FR-005, FR-023, Edge Cases).
* **EC-002 (Cambio concurrente de datos mientras la pantalla está abierta)** : Si los datos del propietario cambian en BD mientras el administrador tiene abierta `/admin/owners/:ownerId`, la pantalla no se actualiza sola y conserva los datos consultados hasta que se vuelva a abrir (FR-027, Edge Cases).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |