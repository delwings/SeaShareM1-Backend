### Test Case Specification: Consulta de Información del Propietario (Solo Lectura y Formatos)
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-informacion-propietario/spec.md` | `docs/features/consultar-informacion-propietario/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-002-ConsultaInformacionPropietario
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la apertura y el contenido de la pantalla "Información del Propietario" (`/admin/owners/:ownerId`) desde el botón "Ver Detalles" de la ventana de búsqueda, el carácter estrictamente de solo lectura (sin campos ni botones de edición, únicamente "Volver"), la presentación uniforme de propietarios con o sin embarcaciones, los formatos de teléfono, documento e iniciales del avatar, la representación de campos vacíos con `"—"` y la exclusión de datos no persistidos en `users` (US2, FR-015 a FR-023, NFR-005, NFR-006, SC-005).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Información de Propietario | Specification Document | `docs/features/consultar-informacion-propietario/spec.md` |
| Implementation Plan: Consultar Información de Propietario | Plan Document | `docs/features/consultar-informacion-propietario/plan.md` |
| Contrato REST: Consultar Información de Propietario | API Contract | `docs/architecture/contracts/rest/consultar-informacion-propietario.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La tabla `users` no almacena tipo de documento (`C.C.`), fecha de registro ni estado de cuenta; por decisión de diseño (`D1`) esos datos no se muestran ni tienen badge en esta pantalla.
* El nombre en la columna `"Propietario"` de la tabla "Control General de Flota Fluvial" no es un enlace ni acceso a "Información del Propietario" (FR-015, [S-17]).

##### Dependencies & Assumptions
* `AdminOwnersService`, `AdminOwnersController`, `ownerFormat.js` y `adminOwner.js` operativos; ruta `/admin/owners/:ownerId` registrada para rol `ADMIN`.
* Usuarios semilla (clave `SeaShare2026!`) disponibles (`1010000002` a `1010000006`), incluyendo propietarios con teléfono de 10 dígitos (ej. `3104567890`), propietarios con teléfono de longitud distinta a 10 dígitos, propietarios sin embarcaciones, nombres de 1, 2 y 3+ palabras, y un registro con teléfono o dato opcional vacío.

##### Preconditions & Default Setup
* **Initial State** : Administrador autenticado ubicado en "Control General de Flota Fluvial" (`/admin`).
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `admin@seashare.com` (usuario `1010000001`, rol `ADMIN`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Acceso, estructura de pantalla y solo lectura (Escenarios US2-1 a US2-4)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Acceder a "Información del Propietario" desde un resultado | Buscar un propietario en `/admin` y pulsar **"Ver Detalles"** en su resultado | Propietario `1010000002` | Navega a `/admin/owners/:ownerId` y muestra la pantalla **"Información del Propietario"** con los datos de ese propietario (FR-015, FR-016). | [ ] | [ ] |
| 2 | Verificar encabezado y bloques de información | Inspeccionar el encabezado y el cuerpo de la pantalla | Propietario precargado | Muestra avatar con iniciales, nombre completo, subtítulo fijo **"Propietario de Flota Fluvial"** y los bloques **"Correo electrónico"**, **"Teléfono de contacto"** y **"Documento de identidad"**; no expone contraseña ni campos internos (FR-017, FR-018). | [ ] | [ ] |
| 3 | Verificar modo de solo lectura y propietario sin embarcaciones | Revisar controles interactivos; abrir un propietario sin embarcaciones registradas | Propietario sin embarcaciones | No existen botones ni campos para editar datos; el único botón de acción es **"Volver"**; un propietario sin embarcaciones muestra el mismo subtítulo **"Propietario de Flota Fluvial"** y la misma estructura (FR-019, FR-017, SC-005). | [ ] | [ ] |
| 4 | Verificar que la columna "Propietario" de la tabla no abre esta pantalla | En la tabla de "Control General de Flota Fluvial", intentar hacer clic sobre el nombre del propietario | Columna "Propietario" en `/admin` | El nombre en la tabla no abre "Información del Propietario" (FR-015, [S-17]). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Formatos de teléfono, documento, iniciales y valores vacíos (Escenarios US2-5 y US2-6)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar formato de teléfono y documento de identidad | Consultar propietario con teléfono `3104567890` y documento `1042389104`; consultar otro con teléfono de `!= 10` dígitos | Teléfono `3104567890`, Doc `1042389104`; teléfono corto/largo | El teléfono de 10 dígitos se muestra como **`+57 310 456 7890`**; si no tiene 10 dígitos se muestra tal como está guardado; el documento se muestra como **`1042389104`** sin puntos de miles (FR-021, Decisión D1). | [ ] | [ ] |
| 2 | Verificar iniciales del avatar en búsqueda y detalle | Revisar avatar para nombres de 1 palabra, 2 palabras y 3 o más palabras | `"Fernando"`, `"Fernando Fernández"`, `"María José de la Cruz"` | Iniciales en mayúscula: 1 palabra -> 1 inicial (`"F"`); 2 palabras -> primera y última (`"FF"`); 3+ palabras -> primera de la primera y primera de la última (`"MC"`) (FR-020, [S-10]). | [ ] | [ ] |
| 3 | Verificar dato vacío mostrado como `"—"` | Consultar propietario donde falte un dato opcional (ej. teléfono vacío) | Propietario con teléfono vacío | El bloque afectado muestra **"—"** y el resto de la pantalla se renderiza con normalidad (FR-022, [S-21]). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Ausencia de datos no persistidos en `users`)** : La pantalla no muestra tipo de documento (`C.C.`), fecha de registro ni estado de cuenta/restricciones, manteniendo solo los atributos guardados del usuario (Decisión D1, Historial 11/11/2026).
* **EC-002 (Administrador abierto por identificador)** : Si se intenta abrir `/admin/owners/:adminId` correspondiente a un usuario con rol `ADMIN`, el servidor responde recurso no encontrado (`404`) y la pantalla lo trata como recurso no encontrado (FR-023, Decisión D2).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |