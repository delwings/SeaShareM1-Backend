### Test Case Specification: Suspensión de Registro desde Paso 2 y Conservación de Borrador
**Created** : 2026-11-14  **Spec/Plan Link** : `cancelar-registro-spec.md` / `cancelar-registro-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
<!--
IMPORTANT: Each test case must clearly state its scope, target system version,
and ownership details. Keep metadata clear and technology-agnostic where appropriate.
-->

**Test Case ID** : TC-002-CancelarRegistro-SuspenderPaso2
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
This test case verifies that el flujo de suspensión desde el Paso 2 (Datos Complementarios) funciona según la especificación: despliegue del modal "¿Suspender registro?", continuación conservando datos y limpiando errores, salida conservando los datos del Paso 1 en estado Borrador y los datos textuales/geográficos del Paso 2 (puerto, coordenadas, tarifa base y servicios adicionales) excluyendo la fotografía, y bloqueo de cierre externo del modal.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Especificación Funcional: Cancelar registro | Specification Document | `cancelar-registro-spec.md` (HU-2 Escenarios 1-6, 7, FR-009 a FR-019, FR-022, NFR-001, NFR-002) |
| Plan Técnico: Cancelar registro | Plan Document | `cancelar-registro-plan.md` (D1, D2, D4, D5; `src/pages/registerBoatDetails.js`, `src/state/draftProgress.js`) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La fotografía adjuntada en el Paso 2 no se conserva por limitación técnica del almacenamiento en navegador (`localStorage`).
* El avance del Paso 2 es local al navegador/dispositivo donde se suspende el registro.

##### Dependencies & Assumptions
* El propietario debe haber completado válidamente el Paso 1 para avanzar al Paso 2, generando el registro base en estado Borrador.

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado con rol **Propietario**, tras completar el Paso 1 (`Nombre: "Sol del Caribe"`, `Matrícula: "COL-4521"`, `Tipo: "Velero"`, `Capacidad: 8`) y ubicado en el formulario de Paso 2 de 2 (`registerBoatDetails.js`).
* **Environment** : Staging / QA Frontend Environment con `localStorage` habilitado.
* **Test Accounts** : Propietario activo (`propietario1@test.com`) con al menos una embarcación previa registrada.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*
<!--
ACTION REQUIRED: Fill out the step-by-step process flow table below.
Add or remove sections and steps as needed for the scenario.
-->

##### Section 1: Modal de Suspensión en Paso 2 y Continuar Registro (FR-009 a FR-014, FR-018, NFR-001, NFR-002)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar botón Cancelar y abrir modal con Paso 2 vacío | En el Paso 2 vacío, hacer clic en el botón "Cancelar" | Formulario Paso 2 vacío | En menos de 1 segundo (NFR-001) se muestra el modal con ícono de advertencia, título "¿Suspender registro?", descripción exacta: *"Los datos de identificación básica (Paso 1) se almacenarán de forma temporal en estado borrador, permitiendo reanudar el proceso de registro en futuras sesiones."* y botones "Continuar registro" y "Salir" (FR-009 a FR-012, NFR-002). | [ ] | [ ] |
| 2 | Validar bloqueo de cierre por clic externo y tecla Esc | Hacer clic fuera del modal y luego presionar la tecla `Escape` | Clic en backdrop / Tecla `Esc` | El modal permanece visible y no se cierra por ninguna de estas acciones (FR-018). | [ ] | [ ] |
| 3 | Continuar registro con Paso 2 vacío | Hacer clic en "Continuar registro" | N/A | El modal se cierra y el propietario permanece en el Paso 2 con el formulario vacío (FR-013). | [ ] | [ ] |
| 4 | Provocar errores de validación con datos ingresados y abrir modal | Ingresar nombre de puerto, dejar tarifa base inválida para disparar error de validación y hacer clic en "Cancelar" | Puerto: `"Muelle Norte"`, Tarifa: `""` (inválida) | Se despliega el modal "¿Suspender registro?" con el mismo texto y botones (FR-010). | [ ] | [ ] |
| 5 | Continuar registro y verificar limpieza de errores | Hacer clic en "Continuar registro" | N/A | El modal se cierra, los mensajes de error visibles se limpian y el dato `"Muelle Norte"` permanece intacto (FR-013, FR-014, SC-005). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Suspender Registro con Datos en Paso 2 y Conservación en Borrador (FR-015 a FR-017, FR-019, FR-022, SC-001, SC-002, SC-004)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Completar todos los campos del Paso 2 incluyendo fotografía | Llenar puerto, seleccionar coordenadas en el mapa, ingresar tarifa base, marcar servicios adicionales y cargar una imagen | Puerto: `"Puerto Velero"`, Coordenadas: `10.9685, -74.7813`, Tarifa: `250000`, Servicios: `["Chalecos", "GPS"]`, Foto: `embarcacion.jpg` | Todos los campos del formulario y la vista previa de la foto se muestran correctamente. | [ ] | [ ] |
| 2 | Suspender registro mediante Cancelar -> Salir | Hacer clic en "Cancelar" y luego clic en "Salir" dentro del modal | N/A | En menos de 1 segundo (SC-004) el usuario es redirigido a la lista de embarcaciones del propietario sin mensajes adicionales (FR-015, FR-019, SC-001). | [ ] | [ ] |
| 3 | Verificar estado Borrador e integridad de lista | Revisar la lista de embarcaciones del propietario | N/A | La embarcación `"Sol del Caribe"` aparece en estado **Borrador** con los datos del Paso 1 conservados (FR-016). Las embarcaciones previas permanecen sin cambios (FR-022). | [ ] | [ ] |
| 4 | Verificar persistencia de avance en navegador (excluyendo foto) | Inspeccionar almacenamiento local (`localStorage` / clave de borrador en `draftProgress.js`) | Clave asociada a usuario y borrador | Los datos de puerto, coordenadas, tarifa base y servicios adicionales están almacenados; la fotografía no está guardada (FR-017). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Suspender desde el Paso 2 sin haber ingresado ningún dato en ese paso conserva igualmente el Paso 1 en estado Borrador sin errores al retomar.
* **EC-002** : La fotografía seleccionada antes de pulsar "Salir" nunca persiste en el navegador, evitando errores por límite de cuota o serialización de archivos.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
