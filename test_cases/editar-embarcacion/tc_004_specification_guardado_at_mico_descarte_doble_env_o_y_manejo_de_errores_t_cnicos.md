### Test Case Specification: Guardado Atómico, Descarte, Doble Envío y Manejo de Errores Técnicos
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/editar-embarcacion/spec.md` | `docs/features/editar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-004-GuardadoDescarteErrores
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el guardado atómico de la edición y la prevención de doble envío (clics repetidos y `Enter`), el flujo de descarte con confirmación al salir con cambios sin guardar, la navegación directa sin cambios, el comportamiento multi-pestaña sin control de versión y el manejo de errores técnicos (red, 5xx, timeout de 15 s), cambio de estado concurrente (`409`), pérdida de permisos (`403`), embarcación eliminada (`404`), expiración de sesión (`401`) y bloqueo de cierre externo de modales.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Editar Embarcación | Specification Document | `docs/features/editar-embarcacion/spec.md` |
| Implementation Plan: Editar Embarcación | Plan Document | `docs/features/editar-embarcacion/plan.md` |
| Contrato REST: Editar Embarcación | API Contract | `docs/architecture/contracts/rest/editar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* No existe control de versión optimista; entre sesiones del mismo propietario prevalece el último guardado (FR-033, Decisión D1).
* Ningún modal del flujo de edición incluye botón "X" ni se cierra con `Esc` o clic fuera (FR-034).

##### Dependencies & Assumptions
* Backend, base de datos MySQL y almacenamiento de imágenes operativos; guardia de navegación en router y `beforeunload` activas.
* DevTools disponible para simular modo *Offline*, latencia > 15 s, respuestas HTTP (`400`, `401`, `403`, `404`, `409`, `500`) y dos pestañas del navegador.

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado ubicado en `/owner/boats/:id/edit` con formulario precargado.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Guardado exitoso, prevención de doble envío y descarte (Escenarios US4-1 a US4-3)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Guardar cambios y verificar prevención de doble envío | Modificar un campo válido, presionar **"Guardar cambios"**, realizar 20 clics rápidos y presionar `Enter` | Cambios válidos + 20 clics rápidos + `Enter` | **"Guardar cambios"** se deshabilita de inmediato con spinner; `Enter` no dispara otro envío; el servidor procesa una sola petición atómica (FR-028, FR-029, NFR-002, SC-002). | [ ] | [ ] |
| 2 | Verificar modal de guardado exitoso y redirección | Esperar respuesta `200` y pulsar **"Aceptar"** en el modal | Respuesta exitosa del servidor | Aparece el modal **"¡Cambios guardados!"** con el texto **"La información de tu embarcación ha sido actualizada exitosamente en el sistema."** y botón **"Aceptar"**; al pulsarlo redirige a "Ver Detalles" con los datos actualizados (FR-029). | [ ] | [ ] |
| 3 | Salir con cambios sin guardar y sin cambios | Probar Descartar/Volver/cambio de ruta/cierre de pestaña con cambios pendientes y luego sin cambios | Formulario con cambios vs. sin cambios | Sin cambios: navega directo a "Ver Detalles". Con cambios: muestra **"¿Descartar cambios?"** con **"Seguir editando"** (mantiene el formulario intacto) y **"Sí, descartar"** (vuelve a "Ver Detalles" sin guardar) (FR-031, FR-032). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Errores técnicos, estado concurrente, permisos, sesión y multi-pestaña (Escenarios US4-4 a US4-9)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular fallo de red, error 5xx o timeout de 15 s | Activar modo *Offline*, forzar `500` o demora `> 15 s` al pulsar **"Guardar cambios"** | Fallo de red / `500` / timeout `15 s` | Se cancela la petición y muestra **"No se pudo guardar los cambios"** con **"Cerrar"** y **"Reintentar"**, conservando intactos todos los datos digitados (FR-028, FR-030, NFR-003, SC-003). | [ ] | [ ] |
| 2 | Simular cambio de estado a Reservado o En Navegación | Cambiar la embarcación a `RESERVED` o `NAVIGATION` en otra sesión y pulsar **"Guardar cambios"** | Estado concurrente `RESERVED` / `NAVIGATION` | Bloquea el guardado y muestra **"No se pudieron guardar los cambios"** con **"La embarcación pasó a estado [Estado] mientras editabas. No es posible guardar los cambios."** y botón **"Recargar página"** (FR-027, Decisión D3/D7). | [ ] | [ ] |
| 3 | Simular embarcación eliminada, pérdida de rol o sesión expirada | Guardar cuando la embarcación fue eliminada (`404`), perdió rol `OWNER` (`403`) o expiró la sesión (`401`) | `404` / `403` / `401` | `404`: **"Esta embarcación ya no existe."** -> **"Aceptar"** lleva a la lista; `403`: **"No tienes permiso para editar esta embarcación."** -> **"Volver a Ver Detalles"**; `401`: redirige a inicio de sesión perdiendo cambios no guardados (FR-025, FR-026, FR-035). | [ ] | [ ] |
| 4 | Editar desde dos pestañas del mismo propietario | Abrir el mismo `boatId` en Pestaña A y Pestaña B, modificar y guardar en ambas | Dos pestañas activas de `propietario.test@seashare.com` | Ambas guardan sin mensaje de conflicto y en la base de datos prevalece el último guardado (FR-033, Decisión D1). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Bloqueo de cierre externo en todos los modales)** : Al intentar cerrar cualquiera de los modales del flujo (`¡Cambios guardados!`, `¿Descartar cambios?`, modales de error) mediante clic fuera (backdrop), tecla `Esc` o botón "X", el modal permanece abierto y solo responde a sus botones explícitos (FR-034).
* **EC-002 (Atomicidad ante fallo en subida de imagen)** : Si el guardado incluye una fotografía nueva y falla la subida/persistencia en el servidor, se revierte toda la operación (sin cambios parciales en BD ni imágenes huérfanas) y se muestra el modal de fallo técnico (FR-028, FR-030, NFR-002).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |