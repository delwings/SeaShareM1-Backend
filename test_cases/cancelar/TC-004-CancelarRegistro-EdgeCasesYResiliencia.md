### Test Case Specification: Edge Cases, Resiliencia Técnica y Fallos de Entorno
**Created** : 2026-11-14  **Spec/Plan Link** : `cancelar-registro-spec.md` / `cancelar-registro-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
<!--
IMPORTANT: Each test case must clearly state its scope, target system version,
and ownership details. Keep metadata clear and technology-agnostic where appropriate.
-->

**Test Case ID** : TC-004-CancelarRegistro-EdgeCasesYResiliencia
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
This test case verifies that la resiliencia del sistema y el manejo de casos borde críticos durante la cancelación o suspensión del registro: expiración de sesión al salir del Paso 2, borrador eliminado concurrentemente desde otra sesión, pérdida de conectividad de red, bloqueo o cuota llena del almacenamiento del navegador (`localStorage`), y datos corruptos almacenados localmente.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Especificación Funcional: Cancelar registro | Specification Document | `cancelar-registro-spec.md` (Casos borde, FR-020, FR-021, FR-025, FR-026) |
| Plan Técnico: Cancelar registro | Plan Document | `cancelar-registro-plan.md` (T158 validación de forma en `draftProgress.js`, T160 limpieza si borrador no existe) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Requiere herramientas de desarrollador del navegador (DevTools) para simular modo Offline, expiración de token/sesión, bloqueo de `localStorage` y manipulación de claves locales.
* Requiere una segunda ventana/sesión (ej. modo incógnito) para simular la eliminación concurrente de un borrador.

##### Dependencies & Assumptions
* El sistema maneja autenticación basada en sesión/token en el cliente.

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado con rol **Propietario**, ubicado en el Paso 2 del registro de un borrador activo con datos ingresados.
* **Environment** : Staging / QA Frontend Environment con acceso a DevTools (Application / Network).
* **Test Accounts** : Propietario activo (`propietario1@test.com`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*
<!--
ACTION REQUIRED: Fill out the step-by-step process flow table below.
Add or remove sections and steps as needed for the scenario.
-->

##### Section 1: Expiración de Sesión y Borrador Inexistente al Salir (FR-020, FR-021, FR-025)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular expiración de sesión antes de confirmar "Salir" | Estando en el Paso 2 con datos ingresados, invalidar/expirar la sesión del usuario, hacer clic en "Cancelar" y luego en "Salir" | Sesión expirada; Puerto: `"Puerto Azul"`, Tarifa: `180000` | El sistema redirige al usuario a la pantalla de inicio de sesión sin mostrar ningún aviso especial, y el borrador junto con el avance local se conservan (FR-020). | [ ] | [ ] |
| 2 | Iniciar sesión nuevamente y validar conservación del borrador | Autenticarse nuevamente con `propietario1@test.com` y retomar el borrador en el Paso 2 | Credenciales válidas | El borrador sigue disponible y el Paso 2 recupera `"Puerto Azul"` y `180000` intactos (FR-020). | [ ] | [ ] |
| 3 | Simular eliminación concurrente del borrador desde otra sesión | Con el modal "¿Suspender registro?" abierto en Sesión A, eliminar el borrador desde Sesión B (incógnito); luego hacer clic en "Salir" en Sesión A | Borrador eliminado externamente | El sistema redirige a la lista de embarcaciones sin mostrar ningún aviso o error especial, y limpia los datos locales asociados al borrador inexistente (FR-021, FR-025). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Pérdida de Red y Resiliencia ante Fallos de Almacenamiento del Navegador (FR-021, FR-026, Plan T158)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular pérdida de conexión al hacer clic en "Salir" | Estando en el Paso 2, activar modo `Offline` en DevTools -> Network, hacer clic en "Cancelar" y luego en "Salir" | Network: Offline | El sistema redirige al propietario a la lista de embarcaciones sin mostrar ningún aviso de error especial (FR-021). | [ ] | [ ] |
| 2 | Bloquear acceso a `localStorage` (modo restringido / cuota excedida) | Deshabilitar almacenamiento local o simular excepción `SecurityError`/`QuotaExceededError` en el navegador, ingresar datos en el Paso 2 y hacer clic en "Cancelar" -> "Salir" | `localStorage` bloqueado / lleno | El registro y la suspensión continúan funcionando con normalidad sin bloqueos, caídas ni avisos de error al usuario (FR-026). | [ ] | [ ] |
| 3 | Inyectar JSON malformado en la clave de progreso del borrador | Escribir una cadena corrupta (`"{corrupt_json:"`) en la clave de `localStorage` del borrador y abrir el Paso 2 | Payload corrupto en `localStorage` | La validación de forma (`draftProgress.js`) descarta silenciosamente los datos inválidos y carga el Paso 2 limpio sin romper la pantalla (FR-026, Plan T158). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Si el propietario hace clic en "Cancelar" en el Paso 1 o Paso 2 con el formulario completamente vacío y pierde conexión o tiene el almacenamiento bloqueado, la navegación hacia la lista de embarcaciones ocurre sin bloquear la interfaz.
* **EC-002** : Ningún fallo técnico en el almacenamiento local o en la red al cancelar/suspender debe alterar o eliminar las embarcaciones previamente registradas por el propietario (FR-022).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
