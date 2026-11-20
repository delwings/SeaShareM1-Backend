### Test Case Specification: Retomar Borrador, Salida Implícita y Aislamiento de Datos
**Created** : 2026-11-14  **Spec/Plan Link** : `cancelar-registro-spec.md` / `cancelar-registro-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
<!--
IMPORTANT: Each test case must clearly state its scope, target system version,
and ownership details. Keep metadata clear and technology-agnostic where appropriate.
-->

**Test Case ID** : TC-003-CancelarRegistro-RetomarYAislamiento
**Product Name & Version** : Sistema de Registro de Embarcaciones v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*
<!--
ACTION REQUIRED: Describe the primary verification objective and system scope.
-->

##### Test Objectives
This test case verifies that la restauración exacta del avance del Paso 2 al retomar un borrador (incluyendo marcador en mapa y exclusión de fotografía), el guardado automático ante salidas sin usar el botón "Cancelar" (cambio de pantalla/navegación o cierre de pestaña), el aislamiento estricto de datos por usuario y por borrador, y la limpieza automática del avance guardado al completar el registro.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Especificación Funcional: Cancelar registro | Specification Document | `cancelar-registro-spec.md` (HU-2 Escenarios 6b & 6c, FR-017, FR-023, FR-024, FR-025, SC-002) |
| Plan Técnico: Cancelar registro | Plan Document | `cancelar-registro-plan.md` (D2, D3, T159, T160, T162, T163; `hashchange`, `pagehide`, `draftProgress.js`) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La restauración de datos es válida únicamente dentro del mismo navegador y dispositivo donde se inició el borrador.

##### Dependencies & Assumptions
* Debe haber pasado previamente `TC-002-CancelarRegistro-SuspenderPaso2` o existir un borrador suspendido en el Paso 2.
* Se requieren dos cuentas de Propietario distintas para validar el aislamiento de datos entre usuarios.

##### Preconditions & Default Setup
* **Initial State** : Propietario A (`propietarioA@test.com`) posee un borrador ("Borrador Alpha") con datos guardados en el Paso 2 en el navegador actual.
* **Environment** : Staging / QA Frontend Environment.
* **Test Accounts** : Propietario A (`propietarioA@test.com`) y Propietario B (`propietarioB@test.com`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*
<!--
ACTION REQUIRED: Fill out the step-by-step process flow table below.
Add or remove sections and steps as needed for the scenario.
-->

##### Section 1: Restauración Exacta al Retomar Borrador (FR-017, FR-023, SC-002)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Retomar embarcación en estado Borrador | Desde la lista de embarcaciones del Propietario A, seleccionar `"Borrador Alpha"` y avanzar al Paso 2 | Borrador con: Puerto `"Puerto Velero"`, Coords `10.9685, -74.7813`, Tarifa `250000`, Servicios `["Chalecos", "GPS"]` | El formulario del Paso 2 carga con el nombre del puerto, coordenadas, tarifa base y servicios adicionales exactamente como se dejaron (FR-023, SC-002). | [ ] | [ ] |
| 2 | Verificar marcador en mapa, ausencia de errores y foto vacía | Inspeccionar el componente de mapa, mensajes de validación y campo de fotografía | N/A | El marcador en el mapa está posicionado en las coordenadas guardadas (`10.9685, -74.7813`), no se muestra ningún mensaje de error y el campo de fotografía aparece vacío listo para seleccionarse de nuevo (FR-017, FR-023). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Conservación al Salir sin Pulsar "Cancelar" (FR-024, Plan D2 / T160)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Modificar datos en Paso 2 y navegar mediante otro menú (`hashchange`) | Actualizar tarifa base y puerto en Paso 2, y sin pulsar "Cancelar", hacer clic en otra opción del menú de navegación | Nuevo Puerto: `"Marina Santa Marta"`, Nueva Tarifa: `320000` | El sistema navega a la pantalla seleccionada y guarda automáticamente el nuevo avance del Paso 2 en `localStorage` (FR-024). | [ ] | [ ] |
| 2 | Retomar borrador y confirmar actualización | Volver a la lista de embarcaciones y abrir el Paso 2 de `"Borrador Alpha"` | N/A | Los campos muestran `"Marina Santa Marta"` y `320000` (FR-024). | [ ] | [ ] |
| 3 | Cerrar pestaña del navegador (`pagehide`) y reabrir | Modificar servicios adicionales en Paso 2, cerrar la pestaña del navegador sin pulsar "Cancelar", abrir una nueva pestaña y retomar `"Borrador Alpha"` | Servicios: `["Chalecos", "GPS", "Radio VHF"]` | Al llegar al Paso 2, se recuperan los últimos datos modificados antes del cierre de pestaña (FR-024). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Aislamiento por Usuario/Borrador y Limpieza al Completar Registro (FR-025, Plan D3 / T163)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar aislamiento entre borradores del mismo usuario | Crear una segunda embarcación nueva ("Borrador Beta") con Propietario A y avanzar al Paso 2 | Datos Paso 1 de `"Borrador Beta"` | El Paso 2 de `"Borrador Beta"` aparece limpio; no hereda ni mezcla los datos guardados de `"Borrador Alpha"` (FR-025). | [ ] | [ ] |
| 2 | Verificar aislamiento entre distintos usuarios en el mismo navegador | Cerrar sesión con Propietario A, iniciar sesión con Propietario B en el mismo navegador y abrir/crear un borrador en Paso 2 | Credenciales `propietarioB@test.com` | El Paso 2 de Propietario B no muestra ningún dato del avance guardado por Propietario A (FR-025). | [ ] | [ ] |
| 3 | Verificar limpieza de avance al completar el registro | Volver con Propietario A, retomar `"Borrador Alpha"`, adjuntar fotografía, completar el registro y verificar `localStorage` | Foto: `foto_final.jpg` | La embarcación pasa a estado registrado y la entrada de avance del Paso 2 en `localStorage` para ese borrador se elimina por completo (FR-025). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Si el usuario cambia de navegador o dispositivo para retomar el borrador, el Paso 1 se carga desde el servidor pero el Paso 2 inicia vacío sin errores (el avance local no viaja entre dispositivos).
* **EC-002** : Si el usuario recarga la página (`F5` / Refresh) estando en el Paso 2, los datos ingresados se conservan y restauran al volver a montar el componente.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
