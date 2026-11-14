### Test Case Specification: Cancelación de Registro desde Paso 1 (Datos Básicos)
**Created** : 2026-11-14  **Spec/Plan Link** : `cancelar-registro-spec.md` / `cancelar-registro-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
<!--
IMPORTANT: Each test case must clearly state its scope, target system version,
and ownership details. Keep metadata clear and technology-agnostic where appropriate.
-->

**Test Case ID** : TC-001-CancelarRegistro-Paso1
**Product Name & Version** : Sistema de Registro de Embarcaciones v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*
<!--
ACTION REQUIRED: Describe the primary verification objective and system scope.
-->

##### Test Objectives
This test case verifies that el flujo de cancelación de registro desde el Paso 1 (Datos Básicos) funciona bajo las condiciones especificadas: visualización del modal "¿Cancelar registro?" (con formulario vacío o con datos), continuación del registro conservando datos y limpiando errores visibles, confirmación de cancelación descartando información sin crear registros ni alterar embarcaciones previas, y bloqueo de cierre del modal por medios externos a sus botones.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Especificación Funcional: Cancelar registro | Specification Document | `cancelar-registro-spec.md` (HU-1, FR-001 a FR-008, FR-018, FR-019, FR-022, NFR-001, NFR-002) |
| Plan Técnico: Cancelar registro | Plan Document | `cancelar-registro-plan.md` (Fase 1 & Fase 2 - Solo frontend) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Pruebas enfocadas en el cliente frontend; no requiere reinicio de backend ni ejecución de migraciones.
* Los tiempos de respuesta de modales y redirecciones (< 1 segundo) deben validarse en condiciones normales de red.

##### Dependencies & Assumptions
* El propietario cuenta con credenciales válidas y acceso al módulo de registro de embarcaciones.
* El propietario posee al menos una embarcación previamente registrada en su lista para verificar integridad (FR-022).

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado con rol **Propietario**, ubicado en el formulario de Paso 1 de 2 (Datos Básicos) del registro de embarcación.
* **Environment** : Staging / QA Frontend Environment (navegador web moderno con soporte estándar de DOM).
* **Test Accounts** : Propietario activo (`propietario1@test.com`) con embarcación preexistente ("La Perla").

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*
<!--
ACTION REQUIRED: Fill out the step-by-step process flow table below.
Add or remove sections and steps as needed for the scenario.
-->

##### Section 1: Modal de Cancelación con Formulario Vacío y Bloqueo de Cierre (FR-001 a FR-005, FR-018, NFR-001, NFR-002)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar presencia del botón Cancelar en Paso 1 | Acceder al formulario Paso 1 de 2 (Datos Básicos) con campos vacíos | Formulario vacío | El botón "Cancelar" es visible y habilitado en el formulario (FR-001). | [ ] | [ ] |
| 2 | Disparar modal de cancelación con formulario vacío | Hacer clic en el botón "Cancelar" | N/A | En menos de 1 segundo (NFR-001) se despliega un modal con ícono de advertencia, título "¿Cancelar registro?", descripción exacta: *"Los datos ingresados hasta el momento se perderán y no se guardará ninguna embarcación."* y botones "Continuar registro" y "Sí, cancelar" (FR-002, FR-003, FR-004, NFR-002). | [ ] | [ ] |
| 3 | Intentar cerrar modal haciendo clic fuera (backdrop) | Hacer clic en el área oscura fuera del contenedor del modal | Clic fuera del modal | El modal permanece abierto; no se cierra ni ejecuta acción alguna (FR-018). | [ ] | [ ] |
| 4 | Intentar cerrar modal presionando tecla Esc | Presionar la tecla `Escape` en el teclado | Tecla `Esc` | El modal permanece abierto; solo responde a sus botones internos (FR-018). | [ ] | [ ] |
| 5 | Continuar registro con formulario vacío | Hacer clic en el botón "Continuar registro" | N/A | El modal se cierra y el propietario permanece en el Paso 1 con todos los campos vacíos tal como estaban (FR-005). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Continuar Registro con Datos Ingresados y Limpieza de Errores (FR-002, FR-005, FR-006)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ingresar datos parciales e inválidos para provocar errores de validación | Completar parcialmente el formulario del Paso 1 dejando campos obligatorios inválidos y disparar validación | Nombre: `"Brisa Marina"`, Matrícula: `""` (inválida) | Se muestran mensajes de error de validación visibles en el formulario del Paso 1. | [ ] | [ ] |
| 2 | Abrir modal de cancelación con datos y errores activos | Hacer clic en el botón "Cancelar" | N/A | Se muestra el mismo modal "¿Cancelar registro?" con su descripción y botones estándar (FR-002). | [ ] | [ ] |
| 3 | Continuar registro y verificar limpieza de errores | Hacer clic en "Continuar registro" | N/A | El modal se cierra, los mensajes de error visibles en el formulario se limpian y el valor ingresado (`"Brisa Marina"`) se conserva intacto (FR-005, FR-006). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Confirmación de Cancelación y Descarte de Datos (FR-007, FR-008, FR-019, FR-022, SC-001, SC-003, SC-004, SC-006)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Completar campos del Paso 1 | Ingresar datos válidos en los campos de Datos Básicos | Nombre: `"Nautilus II"`, Matrícula: `"MAT-9988"`, Tipo: `"Lancha"`, Capacidad: `12` | Los campos reflejan los datos ingresados sin guardar aún en el sistema. | [ ] | [ ] |
| 2 | Confirmar cancelación en máximo 2 clics | Clic 1: "Cancelar" en formulario. Clic 2: "Sí, cancelar" en modal | N/A | En menos de 1 segundo (SC-004) el sistema redirige a la lista de embarcaciones del propietario (pantalla inicial post-login) (FR-007, SC-001). | [ ] | [ ] |
| 3 | Verificar ausencia de notificaciones y descarte de registro | Inspeccionar la pantalla de lista de embarcaciones y recargar la vista | N/A | No se muestra ningún mensaje/toast adicional al llegar a la lista (FR-019). La embarcación `"Nautilus II"` no existe ni como borrador ni como activa (FR-008, SC-003). La embarcación previa `"La Perla"` permanece intacta (FR-022, SC-006). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Hacer clic en "Cancelar" en el Paso 1 cuando el formulario está completamente vacío no omite el modal de confirmación; debe mostrarse exactamente igual que cuando contiene datos.
* **EC-002** : Al cancelar desde el Paso 1, ningún dato residual debe quedar almacenado ni visible si el propietario vuelve a iniciar un nuevo registro desde cero.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
