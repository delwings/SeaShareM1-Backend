### Test Case Specification: Habilitación del Botón Eliminar por Rol y Estado Operativo
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/eliminar-embarcacion/spec.md` | `docs/features/eliminar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-001-HabilitacionBotonEliminar
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el comportamiento del botón "Eliminar" en la pantalla "Ver Detalles" de una embarcación para los roles Propietario (`OWNER`) y Administrador (`ADMIN`) según el estado operativo (`Disponible`, `En Mantenimiento/Limpieza`, `Reservado`, `En Navegación`), incluyendo la ausencia de tooltips, la presencia del ícono de prohibido al pasar el mouse, el bloqueo de clics en estados no permitidos y la accesibilidad en dispositivos táctiles y lectores de pantalla (US3, FR-001 a FR-004, NFR-004, NFR-005, SC-001).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Eliminar Embarcación | Specification Document | `docs/features/eliminar-embarcacion/spec.md` |
| Implementation Plan: Eliminar Embarcación | Plan Document | `docs/features/eliminar-embarcacion/plan.md` |
| Contrato REST: Eliminar Embarcación | API Contract | `docs/architecture/contracts/rest/eliminar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* No se muestran tooltips en ningún estado del botón "Eliminar" (FR-003).
* El centro de notificaciones (campana, lista, indicador de no leídas y marcado como leída) y el historial de eliminadas están fuera de alcance de este spec (FR-019 visual, [S-10], [S-11], [S-20]).

##### Dependencies & Assumptions
* Backend (`SeaShareM1`) y Frontend (`SeaShareM1-Frontend`) operativos con autenticación JWT y migraciones V7 y V8 aplicadas.
* Existen embarcaciones registradas en los cuatro estados operativos (`AVAILABLE`, `MAINTENANCE`, `RESERVED`, `NAVIGATION`), visibles tanto para el Propietario dueño como para el Administrador en "Control General de Flota Fluvial".

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado con sesión activa en la plataforma.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`) y `admin@seashare.com` (rol `ADMIN`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Botón "Eliminar" habilitado en estados permitidos (Escenarios US3-1)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar botón "Eliminar" como Propietario en estados editables/eliminables | Iniciar sesión como `propietario.test@seashare.com`, abrir "Ver Detalles" de embarcaciones propias en `Disponible` y `En Mantenimiento/Limpieza` | Embarcaciones propias en `AVAILABLE` y `MAINTENANCE` | El botón **"Eliminar"** está habilitado en ambos estados y no muestra tooltips (FR-001, FR-002, FR-003). | [ ] | [ ] |
| 2 | Verificar botón "Eliminar" como Administrador en estados permitidos | Iniciar sesión como `admin@seashare.com`, abrir "Ver Detalles" de embarcaciones de cualquier propietario en `Disponible` y `En Mantenimiento/Limpieza` | Embarcaciones en `AVAILABLE` y `MAINTENANCE` | El botón **"Eliminar"** está habilitado para el Administrador en ambos estados, sin tooltips (FR-001, FR-002, FR-003). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Botón "Eliminar" deshabilitado, sin tooltip y accesibilidad (Escenarios US3-2 a US3-4)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar estado deshabilitado en Reservado y En Navegación | Abrir "Ver Detalles" (como `OWNER` y como `ADMIN`) para embarcaciones en `Reservado` y `En Navegación` | Embarcaciones en `RESERVED` y `NAVIGATION` | El botón **"Eliminar"** aparece deshabilitado para ambos roles; no muestra tooltips al pasar el mouse ni al enfocar (FR-002, FR-003). | [ ] | [ ] |
| 2 | Verificar cursor de prohibido y bloqueo de clic | Pasar el mouse sobre el botón **"Eliminar"** deshabilitado y hacer clic sobre él | Embarcación en `RESERVED` o `NAVIGATION` | Al pasar el mouse aparece el ícono de prohibido (círculo rojo con línea diagonal); al hacer clic no ocurre ninguna acción y no se abre ningún modal (FR-003, FR-004). | [ ] | [ ] |
| 3 | Verificar vista táctil (360 px) y lectores de pantalla | Ajustar viewport a `360 px` de ancho y navegar con teclado / lector de pantalla | Viewport `360 px`, navegación por teclado | El botón se ve claramente deshabilitado, mantiene área táctil mínima de `44 px`, su advertencia/estado no depende solo del color rojo y los lectores de pantalla lo anuncian como no disponible (FR-003, NFR-004, NFR-005, [S-16]). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Transición de estado visible tras recarga interna)** : Si una embarcación pasa de `Disponible` a `Reservado` y se vuelve a consultar "Ver Detalles" (por ejemplo mediante "Recargar página" en el flujo de error), el botón "Eliminar" pasa a estado deshabilitado para ambos roles (FR-002, Decisión D12).
* **EC-002 (Control de acceso en servidor independiente de UI)** : Aunque el botón esté oculto o deshabilitado en la interfaz, una petición directa `DELETE /api/v1/fleet/boats/{boatId}` en estado `RESERVED` o `NAVIGATION` debe ser rechazada por el servidor (409) sin mutar el registro (NFR-001, Decisión D2/D3).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |