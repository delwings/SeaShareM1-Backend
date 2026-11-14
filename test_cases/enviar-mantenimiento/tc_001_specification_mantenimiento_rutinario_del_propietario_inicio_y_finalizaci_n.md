### Test Case Specification: Mantenimiento Rutinario del Propietario (Inicio y Finalización)
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/enviar-mantenimiento/spec.md` | `docs/features/enviar-mantenimiento/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-001-MantenimientoRutinarioPropietario
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el flujo de mantenimiento rutinario ejecutado por el Propietario (`OWNER`) desde la pantalla "Ver Detalles": enviar una embarcación en estado `Disponible` a `"Mantenimiento"` sin pedir motivo, cancelar la confirmación, devolverla a `"Disponible"` mediante `"Terminar mantenimiento"`, comprobar la ocultación en el catálogo público, verificar el redibujo inmediato sin modal de éxito, validar el bloqueo del botón cuando la embarcación está en `"Reservado"` o `"En Navegación"` y comprobar la idempotencia ante repeticiones o reintentos (US1-1 a US1-3, US2-1, US2-3, FR-001 a FR-003, FR-006, FR-008 a FR-012, SC-001).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Enviar a Mantenimiento | Specification Document | `docs/features/enviar-mantenimiento/spec.md` |
| Implementation Plan: Enviar a Mantenimiento | Plan Document | `docs/features/enviar-mantenimiento/plan.md` |
| Contrato REST: Enviar a Mantenimiento | API Contract | `docs/architecture/contracts/rest/enviar-mantenimiento.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La suspensión administrativa con motivo obligatorio (`US3`, `FR-015`), la solicitud de revisión (`US4`, `FR-020`) y el rechazo `"Reportar nuevamente"` (`Historia 5, escenario 2`) se cubren en otros casos o quedan fuera de alcance de este flujo rutinario del Propietario.
* El fin de alquiler automático que deja `maintenance_by` vacío (`NULL`) corresponde al Módulo 2 (Plan, Decisión D2).

##### Dependencies & Assumptions
* Backend (`SeaShareM1`) y Frontend (`SeaShareM1-Frontend`) desplegados con JWT activo; endpoints `POST /api/v1/fleet/boats/{id}/maintenance/start` y `POST /api/v1/fleet/boats/{id}/maintenance/finish` operativos con `UPDATE` condicional atómico (Decisiones D1, D5).
* Embarcaciones semilla propiedad de `propietario.test@seashare.com` en estados `Disponible`, `Mantenimiento` (con `maintenance_by = OWNER` y otro con `maintenance_by = NULL` de cierre automático), `Reservado` y `En Navegación`.

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado (`OWNER`) ubicado en la pantalla "Ver Detalles" de una embarcación de su flota.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`) y sesión sin autenticar/catalogo público para verificar visibilidad.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Envío a Mantenimiento Rutinario y Cancelación (Escenarios US1-1, US1-2, US1-3)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Cancelar confirmación de envío a mantenimiento | En "Ver Detalles" de una embarcación `Disponible`, pulsar **"Enviar a Mantenimiento"** y luego pulsar **"Cancelar"** | Embarcación `Disponible` propia | Se abre el modal **"¿Enviar a Mantenimiento?"** con el texto **"La embarcación no recibirá reservas temporalmente."** y botones **"Cancelar"** y **"Sí, enviar"**; al pulsar **"Cancelar"**, el modal cierra y la embarcación sigue `Disponible` (US1-2, FR-001). | [ ] | [ ] |
| 2 | Confirmar envío a mantenimiento desde `Disponible` | Pulsar **"Enviar a Mantenimiento"** -> **"Sí, enviar"** | Embarcación `Disponible` propia | Se invoca `POST /{id}/maintenance/start` sin cuerpo; la etiqueta del estado redibuja de inmediato a **"Mantenimiento"** sin modal de éxito; el botón cambia a **"Terminar mantenimiento"**; `maintenance_by` queda registrado como `OWNER` (US1-1, FR-002, FR-006, Decisiones D1, D2, D7). | [ ] | [ ] |
| 3 | Verificar ocultación en catálogo público durante Mantenimiento | Consultar el catálogo público / listado de búsqueda de clientes con esa embarcación | `boatId` en `"Mantenimiento"` | La embarcación no aparece disponible en el catálogo público ni acepta nuevas reservas (FR-003, SC-001). | [ ] | [ ] |
| 4 | Verificar botón deshabilitado en `Reservado` y `En Navegación` | Abrir "Ver Detalles" de embarcaciones en estado `"Reservado"` y `"En Navegación"` | Embarcaciones en `Reservado` y `En Navegación` | El botón de mantenimiento aparece deshabilitado, sin tooltip, con icono de prohibido al pasar el cursor (US1-3, FR-010, Decisión D8). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Finalización de Mantenimiento Rutinario e Idempotencia (Escenarios US2-1, US2-3)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Finalizar mantenimiento iniciado por el Propietario | En "Ver Detalles" de una embarcación en `"Mantenimiento"` (`maintenance_by = OWNER`), pulsar **"Terminar mantenimiento"** -> **"Sí, terminar"** | Embarcación en `Mantenimiento` (`OWNER`) | Se abre **"¿Terminar Mantenimiento?"** con `"La embarcación podrá recibir reservas nuevamente."`, `"Cancelar"` y `"Sí, terminar"`; al confirmar (`POST /{id}/maintenance/finish`), el estado vuelve a `"Disponible"` sin modal de éxito y `maintenance_by` se limpia (US2-1, FR-006, FR-013, Decisiones D7, D11). | [ ] | [ ] |
| 2 | Finalizar mantenimiento de origen vacío (fin de alquiler automático) | Abrir "Ver Detalles" de una embarcación en `"Mantenimiento"` con `maintenance_by` vacío (`NULL`) y pulsar **"Terminar mantenimiento"** -> **"Sí, terminar"** | Embarcación en `Mantenimiento` (`maintenance_by = NULL`) | El Propietario puede terminar el mantenimiento y devolver la embarcación a `"Disponible"` (FR-006, FR-012, Decisiones D2, D12). | [ ] | [ ] |
| 3 | Verificar idempotencia al repetir una acción ya aplicada | Repetir `POST /{id}/maintenance/start` cuando ya está en `Mantenimiento` o `POST /{id}/maintenance/finish` cuando ya está `Disponible` (p. ej. tras timeout o segunda pestaña) | Acción ya aplicada en BD | El servidor responde `200` con la embarcación completa sin alterar el estado ni sobrescribir `maintenance_by` (US2-3, Edge Cases, Decisión D4). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Conservación de propiedad y acceso durante Mantenimiento)** : Mientras la embarcación está en `"Mantenimiento"`, el Propietario conserva acceso a "Ver Detalles", a su información de propiedad y a las acciones permitidas del módulo (Edge Cases).
* **EC-002 (Orden de verificaciones en servidor)** : El backend valida en orden: sesión y rol `OWNER`, existencia y pertenencia (`404`), estado y origen (`409`), y por último el `UPDATE` condicional atómico para no pisar una reserva simultánea (FR-008, Decisión D5).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |