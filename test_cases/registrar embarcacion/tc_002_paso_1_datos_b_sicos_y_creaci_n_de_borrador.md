### Test Case Specification: Paso 1 – Datos Básicos, Validaciones y Creación de Borrador
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/registrar-embarcacion/spec.md` | `docs/features/registrar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-002-Paso1DatosBasicos
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el acceso al formulario "Paso 1 de 2: Datos Básicos" cuando el propietario no posee borradores previos, la estructura de campos y placeholders, todas las validaciones en tiempo real (nombre, formato y unicidad de matrícula legal, tipo de embarcación y límites de capacidad), y el guardado exitoso que persiste la embarcación en estado Borrador (`DRAFT`) avanzando al Paso 2.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Registrar Embarcación | Specification Document | `docs/features/registrar-embarcacion/spec.md` |
| Implementation Plan: Registrar Embarcación | Plan Document | `docs/features/registrar-embarcacion/plan.md` |
| Contrato REST: Registrar Embarcación | API Contract | `docs/architecture/contracts/rest/registrar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La resolución de conflictos cuando existe un borrador previo se evalúa en `TC-004`.

##### Dependencies & Assumptions
* El endpoint `GET /fleet/boat-types` debe retornar los tipos preconfigurados: `Lancha` (máx. 10), `Yate` (máx. 20), `Catamarán` (máx. 30) y `Velero` (máx. 12).
* Debe existir en la base de datos una matrícula activa o en borrador de otro usuario (ej. `CP-05-9999-A`) y una matrícula perteneciente a una embarcación previamente eliminada (ej. `CP-05-8888-B`).

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado (rol `OWNER`) ubicado en `ownerHome` **sin ningún borrador** (`status = DRAFT`) activo en la tabla `vessel`.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Acceso y estructura visual del Paso 1 (Escenarios US1-3 y US1-4)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Abrir formulario de registro sin borrador | Hacer clic en **"Registrar Nueva Embarcación"** desde la lista | N/A | Se abre el formulario con título **"Registrar Nueva Embarcación"**, subtítulo **"Paso 1 de 2: Información general y datos legales del vehículo fluvial."** e indicador de progreso con paso 1 **"Datos Básicos"** activo y paso 2 **"Datos Complementarios"** inactivo (no clicable) (FR-007 a FR-011). | [ ] | [ ] |
| 2 | Verificar campos, placeholders y botones | Observar los elementos del formulario | N/A | Existen 4 campos vacíos: **"Nombre de la embarcación"** (`Ej. Río Magdalena II`), **"Matrícula legal"** (`Ej. CP-05-XXXX-A`), **"Tipo de embarcación"** (`Seleccionar tipo (Lancha, Yate, etc.)`), **"Capacidad máxima (pasajeros)"** (`Ej. 10`), y botones **"Cancelar"** y **"Siguiente"** (FR-012 a FR-023). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Validaciones en tiempo real de campos (Escenarios US1-6 a US1-16)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Validar campos obligatorios vacíos | Hacer clic en **"Siguiente"** con todos los campos vacíos (o salir de un campo sin escribir) | Campos vacíos | No avanza al paso 2; debajo de cada campo vacío aparece **"Este campo es obligatorio para continuar con el registro"**. El botón "Siguiente" sigue activo (FR-030, FR-031). | [ ] | [ ] |
| 2 | Validar reglas de Nombre de embarcación | Ingresar valores inválidos en "Nombre de la embarcación" (corto, largo >50, solo números, símbolos especiales) | `"Ab"`, `"12345"`, `"Barco@1"` | Muestra en tiempo real (<1s): **"El nombre debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones"** (FR-014, NFR-004). | [ ] | [ ] |
| 3 | Validar formato de Matrícula legal | Ingresar matrículas que no respetan `CP-NN-NNNN-L` | `"CP-5-1042-A"`, `"cp-05-1042-a"` | Muestra en tiempo real: **"Formato de matrícula legal inválido"** y bloquea el avance al hacer clic en "Siguiente" (FR-018). | [ ] | [ ] |
| 4 | Validar unicidad de Matrícula legal | Ingresar una matrícula con formato válido ya registrada o en borrador de otro propietario | `"CP-05-9999-A"` (existente) | Muestra debajo del campo: **"Matrícula existente en el sistema"** y no permite avanzar (FR-019). | [ ] | [ ] |
| 5 | Validar reutilización de matrícula de embarcación eliminada | Ingresar una matrícula válida de una embarcación eliminada previamente | `"CP-05-8888-B"` (eliminada) | El sistema acepta la matrícula sin mostrar error de duplicidad (FR-020). | [ ] | [ ] |
| 6 | Validar capacidad sin tipo seleccionado | Sin elegir tipo de embarcación, escribir un número en "Capacidad máxima (pasajeros)" | Capacidad: `"5"`, Tipo: vacío | Muestra debajo del selector de tipo: **"Este campo es obligatorio para continuar con el registro"** (FR-027). | [ ] | [ ] |
| 7 | Validar caracteres y valores inválidos en Capacidad | Ingresar letras, decimales/signos y cero en "Capacidad máxima (pasajeros)" | `"5a"`, `"5.5"`, `"0"` | Para `"5a"` deja el carácter y muestra **"Solo se aceptan valores numéricos"**; para `"5.5"` muestra **"Ingrese un número entero sin puntos ni comas"**; para `"0"` muestra **"Ingrese un número entero mayor que 0"** (FR-026). | [ ] | [ ] |
| 8 | Validar límite de capacidad por Tipo y cambio de Tipo | Seleccionar `Catamarán`, escribir `25`, y luego cambiar el tipo a `Lancha` (máx. 10) | Tipo: `Lancha`, Capacidad: `25` | Al cambiar a `Lancha`, revalida automáticamente y muestra debajo de capacidad: **"Este valor excede la capacidad máxima de la embarcación"** (FR-024, FR-025, FR-028). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Guardado exitoso como Borrador (Escenarios US1-5 y US1-17)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Completar Paso 1 con datos válidos y guardar | Ingresar datos válidos (incluyendo espacios laterales y ceros a la izquierda para validar limpieza) y hacer clic en **"Siguiente"** | Nombre: `"  Peñón del Norte II  "`, Matrícula: `" CP-05-1042-A "`, Tipo: `Yate`, Capacidad: `"015"` | Durante el envío, el botón **"Siguiente"** se deshabilita sin spinner y muestra ícono de prohibido al pasar el mouse (FR-033). Al finalizar, guarda en BD (`status = DRAFT`, nombre sin espacios laterales, capacidad `15`) y avanza al **Paso 2 de 2** con Paso 1 marcado como completado (FR-015, FR-026, FR-034). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Concurrencia de matrícula)** : Si dos propietarios envían la misma matrícula libre simultáneamente, el primero avanza al Paso 2 y el segundo recibe **"Matrícula existente en el sistema"** (FR-021).
* **EC-002 (Recarga en Paso 1)** : Si el usuario recarga el navegador en el Paso 1 antes de hacer clic en "Siguiente", los campos se limpian y no se crea ningún registro `DRAFT` en la base de datos.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |