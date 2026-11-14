### Test Case Specification: Gestión de Borradores y Resiliencia ante Errores Técnicos
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/registrar-embarcacion/spec.md` | `docs/features/registrar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-004-BorradoresYErrores
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el ciclo de vida completo de un registro en estado Borrador (`DRAFT`) —detección al intentar crear una nueva embarcación, reanudación en el Paso 2 descartando datos parciales previos, advertencia y eliminación física permanente para iniciar un registro limpio, bloqueo de edición del Paso 1 y persistencia tras expiración de sesión—, así como el comportamiento de todos los modales ante fallos técnicos de red o servidor y la idempotencia en reintentos.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Registrar Embarcación | Specification Document | `docs/features/registrar-embarcacion/spec.md` |
| Implementation Plan: Registrar Embarcación | Plan Document | `docs/features/registrar-embarcacion/plan.md` |
| Contrato REST: Registrar Embarcación | API Contract | `docs/architecture/contracts/rest/registrar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La acción directa del botón "Cancelar" dentro de los formularios se evalúa en su propio spec; aquí se parte de un borrador ya existente en base de datos.

##### Dependencies & Assumptions
* Herramientas de desarrollador del navegador (Chrome DevTools -> *Network* -> modo *Offline* o bloqueo de URLs `/fleet/*`) disponibles para simular fallos de red/servidor sin alterar código.
* La cuenta de prueba debe contar con exactamente 1 embarcación en estado `DRAFT` en la tabla `vessel` (ej. nombre `"Río Magdalena II"`, matrícula `"CP-05-1042-A"`).

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado (rol `OWNER`) en `ownerHome` con 1 borrador activo en base de datos.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Detección, Reanudación y Descarte de Borrador (Escenarios US3-1 a US3-9)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Detectar borrador existente al intentar nuevo registro | Hacer clic en **"Registrar Nueva Embarcación"** en `ownerHome` | Borrador `"Río Magdalena II"` | Se despliega el modal **"Borrador detectado"** con el texto **"Tienes un registro incompleto guardado bajo el nombre:"**, el recuadro con **"Río Magdalena II"**, **"¿Cómo deseas continuar?"**, botones **"Empezar un registro nuevo"**, **"Continuar registro pendiente"** y una **"X"** de cierre (FR-066). | [ ] | [ ] |
| 2 | Verificar bloqueo de cierre externo de modales | Hacer clic fuera del modal (backdrop) y presionar la tecla `Esc` | Clic externo / Tecla `Esc` | El modal **NO** se cierra (regla aplica a todos los modales del flujo) (FR-072). | [ ] | [ ] |
| 3 | Cerrar modal con la "X" y verificar persistencia | Hacer clic en la **"X"** del modal "Borrador detectado" | N/A | Cierra el modal, regresa a `ownerHome` y conserva el borrador intacto en BD (FR-071). | [ ] | [ ] |
| 4 | Continuar registro pendiente | Volver a abrir el modal y hacer clic en **"Continuar registro pendiente"** | N/A | Navega directamente al **Paso 2 de 2** con los campos del Paso 2 vacíos (sin datos a medias previos), conservando los datos del Paso 1 sin permitir editarlos (FR-037, FR-038, FR-067). | [ ] | [ ] |
| 5 | Solicitar inicio de registro nuevo y cancelar advertencia | Regresar a `ownerHome`, abrir modal, pulsar **"Empezar un registro nuevo"** y luego **"Regresar"** | N/A | Primero abre el modal **"¿Iniciar nuevo registro?"** con advertencia resaltada **"se eliminará de forma permanente"** y botones **"Regresar"** y **"Sí, eliminar y empezar"**. Al pulsar **"Regresar"**, vuelve a "Borrador detectado" sin borrar nada (FR-068, FR-069). | [ ] | [ ] |
| 6 | Confirmar eliminación física del borrador | Pulsar **"Empezar un registro nuevo"** -> **"Sí, eliminar y empezar"** | Matrícula previa `"CP-05-1042-A"` | Elimina físicamente el borrador en BD (`DELETE /fleet/boats/draft`), libera inmediatamente su matrícula legal y abre el **Paso 1 de 2** completamente vacío (FR-070, Decisión D2). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Manejo de Errores Técnicos y Resiliencia (Edge Cases y FR-061 a FR-076)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular falla de red al guardar en Paso 1 o Paso 2 | Llenar formulario válido, activar modo *Offline* en DevTools y pulsar **"Siguiente"** (o **"Guardar Registro"**) | Modo *Offline* | Muestra modal **"No se pudo guardar el registro"** con texto **"Ocurrió un problema técnico al guardar la información de la embarcación. Verifique su conexión a internet e inténtelo nuevamente."** y botones **"Cerrar"** y **"Reintentar"**, conservando todos los datos escritos (FR-062, FR-063). | [ ] | [ ] |
| 2 | Verificar botones "Cerrar" y "Reintentar" del modal de error | Pulsar **"Cerrar"**, luego reintentar con red desactivada y finalmente reactivar red y pulsar **"Reintentar"** | Modo *Offline* -> *Online* | **"Cerrar"** cierra el modal manteniendo los datos intactos; **"Reintentar"** vuelve a enviar la petición y, al haber red, completa el flujo sin crear registros duplicados (FR-064, FR-065). | [ ] | [ ] |
| 3 | Simular borrador eliminado desde otra pestaña/sesión | Abrir Paso 2 en Pestaña A; en Pestaña B iniciar nuevo registro eliminando el borrador; en Pestaña A pulsar **"Guardar Registro"** | 2 pestañas simultáneas | Muestra modal con el texto **"El borrador ya no existe"** y botón **"Aceptar"**, el cual redirige a `ownerHome` sin registrar la embarcación (FR-061). | [ ] | [ ] |
| 4 | Simular fallos al eliminar o cargar borrador | Con *Offline* activo, pulsar **"Sí, eliminar y empezar"** o **"Continuar registro pendiente"** | Modo *Offline* | Muestra respectivamente los modales **"No se pudo eliminar el borrador"** o **"No se pudo cargar el registro pendiente"** con botones **"Cerrar"** (vuelve a "Borrador detectado") y **"Reintentar"** (FR-073, FR-074). | [ ] | [ ] |
| 5 | Simular fallo de carga de tipos de embarcación o mapa | Bloquear endpoint `/fleet/boat-types` o teselas del mapa | Bloqueo de red | Para tipos muestra **"No se pudieron cargar los tipos de embarcación. Inténtelo nuevamente."** y reintenta al hacer clic; para mapa muestra **"No se pudo cargar el mapa. Verifique su conexión a internet e inténtelo nuevamente."** con botón **"Reintentar"** (FR-075, FR-076). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Expiración de sesión con borrador)** : Si la sesión JWT expira mientras el usuario está en el Paso 2 o al hacer clic en "Siguiente"/"Guardar Registro", el sistema redirige al inicio de sesión; al volver a entrar y pulsar "Registrar Nueva Embarcación", el borrador sigue intacto y aparece "Borrador detectado" (FR-039).
* **EC-002 (Idempotencia ante doble clic o caída de respuesta)** : Si el servidor procesa el guardado del Paso 2 pero la respuesta HTTP se pierde y el usuario pulsa "Reintentar", el sistema identifica el registro por `boatId` y evita crear embarcaciones duplicadas (FR-065, Decisión D5).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |