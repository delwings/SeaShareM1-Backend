### Test Case Specification: Control de Acceso por Rol y Privacidad de Notificaciones

**Created** : 14 de noviembre de 2026  **Spec/Plan Link** : `notificaciones-spec.md` / `notificaciones-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-004-NotificacionesPropietario-SeguridadYPrivacidad
**Product Name & Version** : SeaShare v1.0 (Módulo 1 – Gestión de Flota)
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba valida el aislamiento estricto de información entre usuarios y la segregación de funciones por rol: garantiza que la campana y los endpoints de notificaciones estén restringidos exclusivamente al rol Propietario (respondiendo HTTP 403 al Administrador), y que un Propietario no pueda visualizar ni marcar como leídas notificaciones pertenecientes a otro Propietario (respondiendo HTTP 404).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Notificaciones del Propietario (campana) | Specification Document | `notificaciones-spec.md` |
| Plan de implementación: Notificaciones del Propietario | Plan Document | `notificaciones-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Las pruebas de manipulación de identificadores (`IDOR`) y acceso directo por rol Administrador requieren ejecutar peticiones HTTP autenticadas (Postman/cURL) contra `NotificationController`.

##### Dependencies & Assumptions
* Existen notificaciones almacenadas en la tabla `notification` asociadas al Propietario A (`1010000002`), con un ID de notificación conocido (ej. `notif_id_A`).
* El Propietario B (`1010000006`) cuenta con sus propias notificaciones o ninguna, distintas a las del Propietario A.

##### Preconditions & Default Setup
* **Initial State** : Base de datos poblada con notificaciones para `1010000002`; tokens JWT obtenibles para Administrador y ambos Propietarios.
* **Environment** : Entorno QA / Staging con Backend Spring Boot (`seasharem1`) y Frontend desplegados.
* **Test Accounts** :
  * Administrador: `1010000001` / `SeaShare2026!`
  * Propietario A (dueño de la notificación): `1010000002` / `SeaShare2026!`
  * Propietario B (atacante / otro usuario): `1010000006` / `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Restricción de Rol Administrador en UI y API (FR-004, FR-009, D3)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar ausencia de campana en UI del Administrador | Iniciar sesión en el navegador como Administrador e inspeccionar la barra superior (`header.js`) | Usuario: `1010000001` / Clave: `SeaShare2026!` | El ícono de la campana **no** se renderiza en ningún lugar de la interfaz del Administrador. | [ ] | [ ] |
| 2 | Consultar listado de notificaciones con token de Administrador | Enviar petición `GET` al endpoint de notificaciones usando el JWT del Administrador | Header: `Authorization: Bearer <JWT_Admin>` | El servidor rechaza la petición con HTTP `403 Forbidden`. | [ ] | [ ] |
| 3 | Intentar marcar notificaciones leídas con token de Administrador | Enviar peticiones `PATCH /{id}/read` y `PATCH /read-all` usando el JWT del Administrador | Header: `Authorization: Bearer <JWT_Admin>` | El servidor rechaza ambas peticiones con HTTP `403 Forbidden`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Aislamiento de Datos entre Propietarios (FR-008, SC-003, D6)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar que Propietario B no ve notificaciones de Propietario A | Iniciar sesión como Propietario B (`1010000006`) y abrir la campana / consultar `GET` de notificaciones | Usuario: `1010000006` / Clave: `SeaShare2026!` | La respuesta lista únicamente las notificaciones cuyo `recipient_id` corresponde a `1010000006`; ningún aviso de `1010000002` es visible ni altera su contador. | [ ] | [ ] |
| 2 | Intentar marcar como leída una notificación ajena (IDOR) | Usando el JWT de Propietario B (`1010000006`), enviar `PATCH /{notif_id_A}/read` con el ID de una notificación de Propietario A | `notif_id_A` perteneciente a `1010000002` | El servidor responde HTTP `404 Not Found` ("no encontrada") sin revelar la existencia del registro y la notificación de Propietario A permanece sin leer en BD. | [ ] | [ ] |
| 3 | Verificar aislamiento en "Marcar todas como leídas" | Con JWT de Propietario B, ejecutar `PATCH /read-all` y luego consultar la cuenta de Propietario A | JWT de `1010000006` | Solo se marcan como leídas las notificaciones de `1010000006`; las notificaciones sin leer de `1010000002` permanecen intactas. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Envío de `PATCH /{id}/read` con un ID inexistente en toda la base de datos por parte de un Propietario -> Debe retornar exactamente el mismo código HTTP `404 Not Found` que al consultar un ID ajeno, evitando enumeración de recursos (D6).
* **EC-002** : Peticiones a endpoints de notificaciones sin token de autenticación o con token malformado -> El servidor rechaza el acceso (HTTP 401/403) sin exponer información del listado ni conteos.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |