### Test Case Specification: Visualización y Estados de la Lista de Embarcaciones
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/registrar-embarcacion/spec.md` | `docs/features/registrar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-001-VisualizacionLista
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la pantalla principal "Mis Embarcaciones Registradas" (`ownerHome`) tanto cuando el propietario tiene embarcaciones registradas (validando estructura, textos exactos, orden cronológico por `registered_at`, etiqueta "NUEVA" y exclusión de borradores) como cuando su flota está vacía (estado vacío o solo con un borrador).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Registrar Embarcación | Specification Document | `docs/features/registrar-embarcacion/spec.md` |
| Implementation Plan: Registrar Embarcación | Plan Document | `docs/features/registrar-embarcacion/plan.md` |
| Contrato REST: Registrar Embarcación | API Contract | `docs/architecture/contracts/rest/registrar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Paginación de la lista fuera de alcance según especificación.
* El flujo de inicio de sesión y el comportamiento del botón "Ver Detalles" se asumen funcionales y fuera del alcance de esta prueba.

##### Dependencies & Assumptions
* Autenticación JWT operativa para usuarios con rol `OWNER` y `ADMIN`.
* Backend (`SeaShareM1`) y Frontend (`SeaShareM1-Frontend`) desplegados y comunicándose correctamente.
* **Cuenta A (`propietario.flota@seashare.com`)**: debe tener en la tabla `vessel` de MySQL al menos 2 embarcaciones completadas (`AVAILABLE`/`MAINTENANCE`) con distintas fechas en `registered_at`, 1 embarcación en estado `DRAFT` ("Borrador") y deben existir embarcaciones de *otros* propietarios en la base de datos.
* **Cuenta B (`propietario.nuevo@seashare.com`)**: no debe tener embarcaciones completadas (`AVAILABLE`/`MAINTENANCE`), pudiendo tener 0 registros o únicamente 1 registro en estado `DRAFT`.

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado en el sistema con token JWT válido.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500` o entorno Vercel/Render).
* **Test Accounts** : `propietario.flota@seashare.com` (rol `OWNER`), `propietario.nuevo@seashare.com` (rol `OWNER`) y `admin@seashare.com` (rol `ADMIN`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Lista con embarcaciones registradas (Escenario US1-1)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ingresar a la vista de gestión de flota con cuenta con registros | Iniciar sesión con Cuenta A y navegar a `ownerHome` | `propietario.flota@seashare.com` | Se muestra el título **"Mis Embarcaciones Registradas"**, el subtítulo **"Gestiona el estado operativo y consulta los detalles de tus vehículos fluviales"** y el botón **"Registrar Nueva Embarcación"** habilitado (FR-001, FR-005). | [ ] | [ ] |
| 2 | Verificar estructura de columnas | Revisar los encabezados de la tabla | N/A | La tabla presenta exactamente las 5 columnas: **"Nombre de la embarcación"**, **"Matrícula"**, **"Tipo"**, **"Estado operativo"** y **"Acciones"** (FR-002). | [ ] | [ ] |
| 3 | Verificar datos y botones por fila | Inspeccionar las filas visibles en la tabla | Datos semilla Cuenta A | Cada fila muestra nombre, matrícula legal, tipo, estado operativo (ej. "Disponible", "En Mantenimiento") y el botón **"Ver Detalles"** en "Acciones" (FR-002). | [ ] | [ ] |
| 4 | Verificar orden cronológico descendente | Comparar el orden de las filas contra `vessel.registered_at` | Embarcación 1 (ayer) y Embarcación 2 (hoy) | La embarcación registrada más recientemente (hoy) aparece en la primera fila, seguida de las más antiguas (FR-003). | [ ] | [ ] |
| 5 | Verificar exclusión de borradores y aislamiento de datos | Buscar en la lista el registro `DRAFT` propio y registros de terceros | Registro `DRAFT` Cuenta A + registros de otros dueños | No aparece ninguna embarcación en estado Borrador ni vehículos pertenecientes a otros propietarios (FR-001, FR-006, NFR-001). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Lista sin embarcaciones / Estado vacío (Escenario US1-2)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ingresar con propietario sin embarcaciones registradas | Iniciar sesión con Cuenta B (0 registros en `vessel`) y navegar a `ownerHome` | `propietario.nuevo@seashare.com` | Se muestra el encabezado de la vista, el mensaje exacto **"Aún no tienes embarcaciones registradas"** en lugar de filas de tabla, y el botón **"Registrar Nueva Embarcación"** visible (FR-004, FR-005). | [ ] | [ ] |
| 2 | Verificar estado vacío cuando solo existe un borrador | Insertar o tener 1 registro `status = DRAFT` para Cuenta B y recargar `ownerHome` | Cuenta B con 1 registro `DRAFT` | El sistema oculta el borrador (FR-006) y sigue mostrando **"Aún no tienes embarcaciones registradas"** y el botón **"Registrar Nueva Embarcación"**. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Etiqueta NUEVA)** : Si la embarcación más reciente de Cuenta A tiene menos de 168 horas desde su `registered_at`, debe mostrar la etiqueta **"NUEVA"** junto a su nombre; si ya transcurrieron 168 horas exactas o más, la etiqueta no debe verse (FR-060).
* **EC-002 (Seguridad por rol)** : Intentar consultar `GET /fleet/boats` sin token JWT o con un usuario de rol `ADMIN` debe bloquear el acceso (HTTP `401` / `403`) sin exponer datos (NFR-001).
* **EC-003 (Rendimiento)** : Verificar en DevTools (*Network*) que la consulta `GET /fleet/boats` responda en menos de 1 segundo bajo condiciones normales.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |