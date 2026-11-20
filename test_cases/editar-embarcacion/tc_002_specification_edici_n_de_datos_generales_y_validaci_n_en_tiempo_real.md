### Test Case Specification: Edición de Datos Generales y Validación en Tiempo Real
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/editar-embarcacion/spec.md` | `docs/features/editar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-002-DatosGeneralesValidacion
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la autoselección de texto al enfocar campos precargados, la normalización de espacios y ceros a la izquierda, las validaciones en tiempo real (`< 1 s`) al escribir o pegar en "Nombre de la embarcación", "Capacidad máxima (pasajeros)" y "Tarifa base (COP)", la reversión del estado del botón "Guardar cambios" al restaurar valores originales y el manejo de errores de validación del servidor (`HTTP 400`) con foco en el primer campo con error.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Editar Embarcación | Specification Document | `docs/features/editar-embarcacion/spec.md` |
| Implementation Plan: Editar Embarcación | Plan Document | `docs/features/editar-embarcacion/plan.md` |
| Contrato REST: Editar Embarcación | API Contract | `docs/architecture/contracts/rest/editar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Topes máximos de capacidad por tipo: `Lancha` (10), `Yate` (20), `Catamarán` (30) y `Velero` (12) (FR-011).
* Tope máximo de tarifa base: `$9.999.999.999` COP (FR-012).
* Ningún texto de la pantalla debe mencionar periodicidad por hora ni por día (FR-012).

##### Dependencies & Assumptions
* El endpoint `GET /fleet/boats/:id` entrega una embarcación editable (`AVAILABLE` o `MAINTENANCE`) del Propietario autenticado con sus datos precargados.

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado ubicado en `/owner/boats/:id/edit` con el formulario precargado y "Guardar cambios" deshabilitado.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Autoselección al enfocar y edición válida de datos generales (Escenarios US2-1 a US2-4)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar autoselección al enfocar campos precargados | Hacer clic/toque o presionar `Tab` sobre un campo editable precargado, y luego hacer un segundo clic dentro del texto | Campo precargado (ej. `"Brisa del Mar"`), segundo clic interno | Al enfocar por clic, toque o `Tab`, todo el texto queda seleccionado automáticamente; el segundo clic ubica el cursor en el punto exacto clicado (FR-008). | [ ] | [ ] |
| 2 | Editar nombre de embarcación con formato válido | Escribir un nombre válido (3 a 50 caracteres, con al menos una letra, letras/números/espacios/tildes/guiones) y disparar `blur` | `"  Brisa   del  Mar 2  "` | Se normalizan espacios extremos y múltiples espacios internos a un solo espacio; no hay mensajes de error; **"Guardar cambios"** se habilita si el valor final difiere del inicial (FR-009, FR-010). | [ ] | [ ] |
| 3 | Editar capacidad máxima y tarifa base con valores válidos | Ingresar capacidad válida dentro del límite del tipo y tarifa entre `1` y `9999999999` | Capacidad: `"007"`, Tarifa: `"500000"` | `"007"` se normaliza a `7`; la tarifa se acepta sin mencionar periodicidad por hora ni por día; **"Guardar cambios"** queda habilitado si hay cambio real (FR-011, FR-012, FR-023). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Validaciones en tiempo real, pegado, reversión y errores 400 (Escenarios US2-5 a US2-7)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Escribir o pegar nombres de embarcación inválidos | Probar campo vacío, solo números/guiones o longitud fuera de rango (pegado y escritura) | `""`, `"123"`, `"12-3"`, texto de 51 caracteres | En menos de `1 s` aparece el mensaje de error bajo el campo siguiendo la cascada de prioridad; **"Guardar cambios"** permanece deshabilitado (FR-010, FR-013, NFR-004). | [ ] | [ ] |
| 2 | Escribir o pegar capacidades inválidas | Probar letras, decimales, signos, cero, valores `>= 100` o mayores al tope del tipo de embarcación | `"12a"`, `"1.5"`, `"-5"`, `"0"`, `"100"`, capacidad superior al tipo | En menos de `1 s` aparece el error bajo el campo según prioridad (ej. caracteres no numéricos, entero sin puntos/comas, mayor que 0, tope por tipo); **"Guardar cambios"** permanece deshabilitado (FR-011, FR-013, NFR-014). | [ ] | [ ] |
| 3 | Escribir o pegar tarifas base inválidas | Probar letras, puntos, comas, signos, cero o valores mayores a `9.999.999.999` | `"450.000"`, `"450,000"`, `"-1"`, `"0"`, `"10000000000"` | En menos de `1 s` aparece el error bajo el campo según prioridad; **"Guardar cambios"** permanece deshabilitado (FR-012, FR-013, NFR-014). | [ ] | [ ] |
| 4 | Revertir cambios al valor original precargado | Restaurar manualmente los valores originales de los campos editados | Valores iniciales de la embarcación | El sistema detecta que no hay diferencias respecto al estado inicial y **"Guardar cambios"** vuelve a deshabilitarse (FR-023). | [ ] | [ ] |
| 5 | Recibir errores de validación del servidor (HTTP 400) | Simular respuesta `400 Bad Request` con errores de campo al enviar | Respuesta `400` con errores por campo | Se muestra cada mensaje bajo su campo correspondiente, se rehabilita **"Guardar cambios"** y el foco se mueve automáticamente al primer campo con error (FR-026, Decisión D3). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Normalización de espacios pegados)** : Al pegar texto con espacios extremos, tabulaciones o espacios no estándar en "Nombre de la embarcación" o "Puerto", se normalizan al perder el foco o al guardar (FR-009).
* **EC-002 (Nombre repetido en flota propia y ceros en capacidad)** : Se permite que el nombre de la embarcación coincida con el de otra embarcación propia; valores como `"000"` en capacidad se evalúan como `0` y muestran error de validación (FR-010, FR-011).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |