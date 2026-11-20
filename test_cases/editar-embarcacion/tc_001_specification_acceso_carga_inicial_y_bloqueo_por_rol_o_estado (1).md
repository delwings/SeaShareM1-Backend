### Test Case Specification: Acceso, Carga Inicial y Bloqueo por Rol o Estado
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/editar-embarcacion/spec.md` | `docs/features/editar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-001-AccesoCargaBloqueo
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el acceso al formulario de edición de embarcación (`Editar Información de Embarcación`) desde la vista "Ver Detalles" y mediante URL directa, validando la habilitación/deshabilitación del botón "Editar" según rol (`OWNER` vs. `ADMIN`) y estado operativo (`Disponible`, `En Mantenimiento/Limpieza`, `Reservado`, `En Navegación`), el estado de carga inicial sin formulario vacío y la precarga de datos con campos bloqueados (`Matrícula legal` y `Tipo de embarcación`) y botón "Guardar cambios" deshabilitado por defecto.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Editar Embarcación | Specification Document | `docs/features/editar-embarcacion/spec.md` |
| Implementation Plan: Editar Embarcación | Plan Document | `docs/features/editar-embarcacion/plan.md` |
| Contrato REST: Editar Embarcación | API Contract | `docs/architecture/contracts/rest/editar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* El botón "Editar" deshabilitado no muestra tooltips en ningún caso (FR-003).
* Los servicios adicionales llegan dentro de la misma consulta de detalle/edición de la embarcación; no existe carga aislada ni fallo aislado de servicios (FR-022 eliminado).

##### Dependencies & Assumptions
* Backend (`SeaShareM1`) y Frontend (`SeaShareM1-Frontend`) operativos y autenticados con JWT.
* En la base de datos existen embarcaciones del Propietario titular en estados `Disponible` (`AVAILABLE`), `En Mantenimiento/Limpieza` (`MAINTENANCE`), `Reservado` (`RESERVED`) y `En Navegación` (`NAVIGATION`), además de una embarcación ajena, una eliminada (`deleted = TRUE`) y un `boatId` inexistente.

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado en la plataforma con sesión activa.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`), `propietario.otro@seashare.com` (rol `OWNER`) y `admin@seashare.com` (rol `ADMIN`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Habilitación del botón Editar, carga inicial y precarga bloqueada (Escenarios US1-1, US1-6 y US1-7)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar botón "Editar" habilitado en estados permitidos | Iniciar sesión como Propietario, abrir "Ver Detalles" de embarcaciones en "Disponible" y "En Mantenimiento/Limpieza", y presionar "Editar" | Embarcaciones propias en `AVAILABLE` y `MAINTENANCE` | El botón **"Editar"** está habilitado en ambos estados; al presionarlo navega al formulario de edición precargado (FR-001, FR-002). | [ ] | [ ] |
| 2 | Verificar estado de carga inicial | Navegar a `/owner/boats/:id/edit` simulando latencia en la consulta inicial | `boatId` válido del Propietario | Se muestra indicador de carga (skeleton o spinner) mientras se consultan los datos, sin desplegar un formulario vacío ni valores por defecto falsos (FR-005). | [ ] | [ ] |
| 3 | Verificar precarga, campos bloqueados y botón Guardar | Inspeccionar el formulario una vez finalizada la carga inicial | Embarcación precargada (ej. `"Brisa del Mar"`, `"CP-05-1042-A"`, `Yate`) | Todos los campos muestran los valores vigentes de la embarcación; **"Matrícula legal"** y **"Tipo de embarcación"** aparecen bloqueados con ícono de candado y no se pueden editar; **"Guardar cambios"** inicia deshabilitado (FR-006, FR-007). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Bloqueo por estado o rol, acceso por URL directa y fallo de carga (Escenarios US1-2 a US1-5)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar botón "Editar" deshabilitado en estados no editables | Abrir "Ver Detalles" como Propietario para embarcaciones en "Reservado" y "En Navegación" | Embarcaciones propias en `RESERVED` y `NAVIGATION` | El botón **"Editar"** aparece deshabilitado, sin tooltip, muestra ícono de prohibido (círculo rojo con línea diagonal) al pasar el mouse y no abre el formulario al presionarlo (FR-002, FR-003). | [ ] | [ ] |
| 2 | Verificar ocultación del botón "Editar" para Administrador | Iniciar sesión como `admin@seashare.com` y abrir "Ver Detalles" de cualquier embarcación | Cuenta `ADMIN`, `boatId` existente | El botón **"Editar"** no se muestra en la vista de detalle para el rol Administrador (FR-001). | [ ] | [ ] |
| 3 | Acceder por URL directa a embarcación en estado no editable | Abrir directamente `/owner/boats/:id/edit` de una embarcación propia en `RESERVED` o `NAVIGATION` | URL directa de embarcación en `RESERVED` / `NAVIGATION` | Muestra pantalla de bloqueo con el texto exacto **"Esta embarcación no se puede editar en su estado actual."** y el botón **"Volver a Ver Detalles"** (FR-004). | [ ] | [ ] |
| 4 | Acceder por URL directa sin permisos, inexistente o con otro rol | Abrir directamente `/owner/boats/:id/edit` con embarcación ajena, eliminada, inexistente o con rol `ADMIN` | `boatId` de otro dueño, `deleted = TRUE`, inexistente; sesión `ADMIN` | Para embarcación ajena, eliminada o inexistente muestra anti-enumeración con **"Esta embarcación no existe o no tienes permiso para editarla."** y **"Volver a Ver Detalles"**; para rol `ADMIN` muestra pantalla de acceso no autorizado sin exponer datos de la embarcación (FR-004, FR-025). | [ ] | [ ] |
| 5 | Simular fallo en consulta inicial de datos | Bloquear o forzar error en el `GET` inicial de la embarcación al entrar a `/owner/boats/:id/edit` | `boatId` válido con error de red/servidor | Muestra **"No se pudo cargar la información de la embarcación"** con los botones **"Reintentar"** y **"Volver a Ver Detalles"**, sin mostrar el formulario vacío (FR-005). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Orden de verificación y respuesta anti-enumeración en servidor)** : En el backend (`GET` / `PUT`), si una embarcación es ajena, eliminada, inexistente o borrador (`DRAFT`), responde con `404` idéntico; el orden de verificación se detiene en el primer fallo: (1) sesión activa, (2) permisos y existencia, (3) estado operativo, (4) validación de campos y archivos, (5) guardado (FR-025, Decisión D3/D7).
* **EC-002 (Cambio de estado concurrente antes de guardar)** : Si la embarcación pasa a `Reservado` o `En Navegación` mientras el propietario tiene abierto el formulario, al intentar guardar el servidor responde `409` con `current_status` y se muestra el modal **"No se pudieron guardar los cambios"** con **"La embarcación pasó a estado [Estado] mientras editabas. No es posible guardar los cambios."** y el botón **"Recargar página"** (FR-027, Decisión D3/D7).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |