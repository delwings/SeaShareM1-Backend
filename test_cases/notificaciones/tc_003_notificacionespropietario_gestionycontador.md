### Test Case Specification: Gestión de Notificaciones y Actualización de Contador en Campana

**Created** : 14 de noviembre de 2026  **Spec/Plan Link** : `notificaciones-spec.md` / `notificaciones-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-003-NotificacionesPropietario-GestionYContador
**Product Name & Version** : SeaShare v1.0 (Módulo 1 – Gestión de Flota)
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el ciclo de vida visual y funcional de la campana de notificaciones del Propietario: renderizado exclusivo en el header del propietario, orden cronológico inverso, resaltado de no leídas, marcado individual y masivo como leídas (`PATCH /{id}/read` y `PATCH /read-all`), comportamiento de estado vacío y sincronización del contador (al cargar pantalla, al abrir panel y cada 60 segundos).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Notificaciones del Propietario (campana) | Specification Document | `notificaciones-spec.md` |
| Plan de implementación: Notificaciones del Propietario | Plan Document | `notificaciones-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Para verificar el sondeo automático de 60 segundos sin esperar múltiples ciclos, se recomienda mantener abierta la consola de red (DevTools Network) o acelerar el reloj del navegador en pruebas automatizadas.

##### Dependencies & Assumptions
* Existen al menos 3 notificaciones sin leer en la tabla `notification` asociadas al Propietario `1010000004`, creadas en distintos momentos.
* Existe una cuenta de Propietario `1010000005` con 0 registros en la tabla `notification`.

##### Preconditions & Default Setup
* **Initial State** : Usuario sin sesión iniciada; datos semilla preparados en base de datos.
* **Environment** : Entorno QA / Staging con Backend Spring Boot (`seasharem1`) y Frontend desplegados.
* **Test Accounts** :
  * Propietario con notificaciones: `1010000004` / `SeaShare2026!`
  * Propietario sin notificaciones: `1010000005` / `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Visualización del Panel, Orden y Marcado Individual (Historia 3, FR-004, FR-005, FR-007, SC-004)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Iniciar sesión con Propietario con avisos pendientes | Autenticarse en la plataforma y observar el header superior (`header.js`) | Usuario: `1010000004` / Clave: `SeaShare2026!` | Se muestra el ícono de campana en la barra superior con el badge contador en `3`. | [ ] | [ ] |
| 2 | Abrir panel de notificaciones | Hacer clic sobre el ícono de la campana | N/A | Se despliega el panel listando las 3 notificaciones ordenadas de la más reciente a la más antigua, mostrando mensaje completo y fecha/hora. Las 3 aparecen visualmente resaltadas como no leídas. | [ ] | [ ] |
| 3 | Marcar una notificación individual como leída | Hacer clic sobre la primera notificación no leída de la lista | N/A | Se invoca `PATCH /{id}/read`, la notificación pierde el resaltado visual de "no leída" y el contador de la campana baja al instante de `3` a `2`. | [ ] | [ ] |
| 4 | Verificar idempotencia al volver a tocar una ya leída | Hacer clic nuevamente sobre la misma notificación ya leída | N/A | No ocurre ningún error, el estado visual permanece como leída y el contador se mantiene en `2` sin decrementar de más. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Marcado Masivo y Estado Vacío (Historia 3, FR-007)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Marcar todas las notificaciones como leídas | Con el panel abierto (2 notificaciones sin leer), hacer clic en "Marcar todas como leídas" | N/A | Se invoca `PATCH /read-all`, todas las notificaciones pierden el resaltado y el badge contador desaparece por completo de la campana. | [ ] | [ ] |
| 2 | Verificar idempotencia de "Marcar todas como leídas" | Presionar nuevamente "Marcar todas como leídas" cuando ya no hay pendientes | N/A | La acción no altera el estado ni genera errores en consola/UI; el contador sigue oculto. | [ ] | [ ] |
| 3 | Verificar estado vacío en cuenta sin notificaciones | Cerrar sesión, iniciar con el propietario sin registros y abrir la campana | Usuario: `1010000005` / Clave: `SeaShare2026!` | La campana no muestra badge numérico y al abrir el panel se visualiza el mensaje exacto: "No tienes notificaciones.". | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Disparadores de Actualización del Contador (FR-006, D5)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar actualización al navegar entre pantallas | Con sesión iniciada como `1010000004` y DevTools abierto, navegar a otra vista/pantalla de la aplicación | N/A | Se dispara una petición `GET` de consulta de notificaciones al cargar la nueva pantalla. | [ ] | [ ] |
| 2 | Verificar actualización por intervalo de 60 segundos | Permanecer inactivo en la misma pantalla durante 65 segundos observando la pestaña Network | Tiempo de espera: 65 s | El cliente ejecuta automáticamente una consulta periódica a los 60 segundos para refrescar el contador. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Tope visual del contador (`99+`): Si un propietario posee 100 o más notificaciones sin leer en base de datos, el badge de la campana debe mostrar exactamente `"99+"` (FR-004).
* **EC-002** : Generación de una nueva notificación mientras el propietario tiene la sesión abierta -> Al abrir el panel de la campana o cumplirse el ciclo de 60 segundos, el contador y la lista se actualizan reflejando el nuevo aviso sin necesidad de recargar toda la página.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |