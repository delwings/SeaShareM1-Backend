### Test Case Specification: Validaciones del Motivo en el Modal de Eliminación

**Created** : 14 de noviembre de 2026  **Spec/Plan Link** : `notificaciones-spec.md` / `notificaciones-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-002-NotificacionesPropietario-ValidacionesModalEliminar
**Product Name & Version** : SeaShare v1.0 (Módulo 1 – Gestión de Flota)
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica las reglas de validación de interfaz de usuario y de contrato API (`AdminDeleteBoatUseCase`) sobre el campo de motivo al eliminar una embarcación: obligatoriedad, restricción de longitud máxima de 300 caracteres con contador dinámico, recorte de espacios en blanco (`trim`), rechazo HTTP 400 sin eliminación de datos y ausencia del campo para el rol Propietario.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Notificaciones del Propietario (campana) | Specification Document | `notificaciones-spec.md` |
| Plan de implementación: Notificaciones del Propietario | Plan Document | `notificaciones-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Para validar el rechazo del servidor (HTTP 400) con más de 300 caracteres, se requiere enviar la petición directamente al endpoint `DELETE` de administrador, ya que el componente `deleteBoatFlow.js` bloquea el exceso de caracteres en el cliente.

##### Dependencies & Assumptions
* `TC-001-NotificacionesPropietario-EliminacionYMotivo` debe estar comprendido como flujo base.
* Se dispone de una herramienta de pruebas de API (Postman/cURL) con token JWT válido de Administrador para validar las reglas de backend.

##### Preconditions & Default Setup
* **Initial State** : Base de datos con embarcaciones activas disponibles para pruebas de validación; sesión no iniciada en el navegador.
* **Environment** : Entorno QA / Staging con Backend Spring Boot (`seasharem1`) y Frontend desplegados.
* **Test Accounts** :
  * Administrador: `1010000001` / `SeaShare2026!`
  * Propietario: `1010000002` / `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Validaciones en UI del Administrador (Historia 2, FR-002)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Iniciar sesión como Administrador y abrir detalle de embarcación | Navegar al login, autenticarse y abrir el detalle de una embarcación activa (`adminBoatDetail.js`) | Usuario: `1010000001` / Clave: `SeaShare2026!` | Se muestra el detalle de la embarcación con la opción para eliminarla. | [ ] | [ ] |
| 2 | Abrir modal de eliminación | Hacer clic en el botón de eliminar embarcación | N/A | Se despliega el modal "¿Eliminar embarcación?" con el campo "Motivo de la eliminación" vacío y el contador en `0/300`. | [ ] | [ ] |
| 3 | Intentar confirmar eliminación con campo vacío | Hacer clic en "Sí, eliminar" sin ingresar texto | `""` (vacío) | Se muestra el mensaje de validación "Escribe el motivo de la eliminación." y la embarcación **no** se elimina. | [ ] | [ ] |
| 4 | Intentar confirmar con solo espacios en blanco | Escribir únicamente espacios en el campo y presionar "Sí, eliminar" | `"     "` (5 espacios) | El sistema recorta espacios, detecta longitud 0, muestra "Escribe el motivo de la eliminación." y no envía petición destructiva. | [ ] | [ ] |
| 5 | Verificar límite máximo de 300 caracteres y contador | Escribir o pegar una cadena de 310 caracteres en el campo "Motivo de la eliminación" | Cadena de texto de 310 caracteres | El campo trunca o impide ingresar más de 300 caracteres y el contador visual muestra exactamente `300/300`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Validaciones en Contrato API Backend (FR-002, SC-002, D2)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Enviar `DELETE` sin cuerpo o con `reason` vacío/nulo | Ejecutar petición `DELETE` al endpoint administrativo de eliminación con motivo vacío | `{"reason": "   "}` | El backend responde HTTP `400 Bad Request` con estructura `errors[{field: "reason"}]` antes de buscar/eliminar la embarcación; la embarcación permanece activa en BD. | [ ] | [ ] |
| 2 | Enviar `DELETE` con `reason` de 301 caracteres | Ejecutar petición `DELETE` al endpoint administrativo con 301 caracteres válidos | `{"reason": "<cadena_301_caracteres>"}` | El backend rechaza la solicitud con HTTP `400 Bad Request` (`errors[{field: "reason"}]`) y la embarcación no se elimina. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Ausencia de Campo Motivo para el Propietario (Historia 2 - Escenario 3, FR-003)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Iniciar sesión como Propietario y abrir modal de eliminar | Cerrar sesión de admin, ingresar como propietario, ir a una de sus embarcaciones y presionar eliminar | Usuario: `1010000002` / Clave: `SeaShare2026!` | Se abre el modal de confirmación de eliminación del propietario **sin** el campo "Motivo de la eliminación" ni contador `n/300`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Ingresar un motivo de exactamente 1 carácter válido rodeado de espacios (ej. `"  A  "`) -> El sistema debe aceptarlo tras aplicar `trim()`, eliminando la embarcación y guardando `"Motivo: A"`.
* **EC-002** : Ingresar exactamente 300 caracteres válidos -> El contador marca `300/300`, el backend acepta el payload (HTTP 200/204) y almacena el motivo completo sin truncamiento adicional.
* **EC-003** : Si ocurre un error recuperable al confirmar la eliminación en el modal (`deleteBoatFlow.js`), el texto escrito en el campo de motivo debe conservarse al reintentar sin obligar al administrador a redactarlo de nuevo.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |