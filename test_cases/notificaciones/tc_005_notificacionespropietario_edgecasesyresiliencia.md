### Test Case Specification: Edge Cases, Interacción de Panel y Resiliencia Técnica

**Created** : 14 de noviembre de 2026  **Spec/Plan Link** : `notificaciones-spec.md` / `notificaciones-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-005-NotificacionesPropietario-EdgeCasesYResiliencia
**Product Name & Version** : SeaShare v1.0 (Módulo 1 – Gestión de Flota)
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica exclusivamente los **Edge Cases**, condiciones límite y escenarios de resiliencia definidos en la especificación: comportamiento de cierre y no-navegación del panel, tope de visualización de 50 registros frente al conteo global, tolerancia a fallos de persistencia de avisos durante la eliminación administrativa, recuperación de errores de red mediante el botón "Reintentar" y redirección limpia ante expiración de sesión.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Notificaciones del Propietario (campana) | Specification Document | `notificaciones-spec.md` |
| Plan de implementación: Notificaciones del Propietario | Plan Document | `notificaciones-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Para simular fallos de red con el panel abierto y expiración de token JWT, se requiere el uso de herramientas de desarrollo del navegador (DevTools Network throttling/offline o borrado de token en almacenamiento local).
* La simulación de fallo al crear el aviso en backend requiere inyección de fallo controlada en entorno de pruebas o verificación de logs del servidor.

##### Dependencies & Assumptions
* El Propietario `1010000002` tiene cargados en la tabla `notification` un total de 65 registros sin leer (para probar el límite de 50 elementos en lista vs. 65 en contador).

##### Preconditions & Default Setup
* **Initial State** : Propietario `1010000002` con 65 notificaciones sin leer en base de datos; sesión no iniciada.
* **Environment** : Entorno QA / Staging con Backend Spring Boot (`seasharem1`) y Frontend desplegados.
* **Test Accounts** :
  * Administrador: `1010000001` / `SeaShare2026!`
  * Propietario (con >50 notificaciones): `1010000002` / `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Interacción del Panel y Tope de 50 Notificaciones (Edge Cases, FR-005, D4, D7)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar contador global vs. límite de 50 en lista | Iniciar sesión como Propietario `1010000002` y abrir el panel de la campana | Usuario: `1010000002` / Clave: `SeaShare2026!` (65 no leídas en BD) | El badge de la campana muestra `65` (total real de no leídas), mientras que la lista desplegable renderiza exactamente las `50` notificaciones más recientes. | [ ] | [ ] |
| 2 | Verificar ausencia de navegación al hacer clic en una notificación | Con el panel abierto, hacer clic sobre una notificación de embarcación eliminada | N/A | La notificación se marca como leída, pero la aplicación **no** navega a ninguna otra URL ni intenta abrir el detalle de la embarcación eliminada. | [ ] | [ ] |
| 3 | Cerrar panel presionando la tecla `Esc` | Con el panel abierto, presionar la tecla `Escape` (`Esc`) en el teclado | Tecla: `Esc` | El panel de notificaciones se cierra inmediatamente. | [ ] | [ ] |
| 4 | Cerrar panel haciendo clic fuera del contenedor | Volver a abrir la campana y hacer clic en cualquier área externa del header o cuerpo de la página | Clic fuera del panel | El panel de notificaciones se cierra inmediatamente. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Resiliencia ante Errores de Red, Sesión Vencida y Fallo de Creación (Historia 3 - Escenario 4, Edge Cases)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular fallo de red con el panel abierto y probar "Reintentar" | Activar modo *Offline* en DevTools, abrir la campana (o forzar refresco), observar el error, restaurar red y hacer clic en "Reintentar" | Estado de red: Offline -> Online | Mientras está offline, el panel muestra "No se pudieron cargar las notificaciones." junto con el botón "Reintentar". Al restaurar red y pulsar "Reintentar", se recarga la lista correctamente. | [ ] | [ ] |
| 2 | Simular sesión vencida al consultar notificaciones | Invalidar/expirar el token de sesión del propietario y abrir la campana (o esperar la consulta periódica) | Token JWT expirado / inválido (HTTP 401) | El cliente redirige directamente a la pantalla de inicio de sesión **sin** mostrar el mensaje de error interno de la campana ("No se pudieron cargar las notificaciones."). | [ ] | [ ] |
| 3 | Verificar resiliencia cuando falla la creación de la notificación al eliminar | Como Administrador (`1010000001`), eliminar una embarcación activa simulando un fallo en el servicio/persistencia de notificaciones | Motivo: `"Incumplimiento de normativa"` | La eliminación de la embarcación se completa y persiste exitosamente; el aviso no llega al propietario y el error queda registrado en el log del servidor sin revertir la baja de la embarcación. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Si el propietario tiene 65 notificaciones sin leer y marca una de las 50 visibles como leída, el contador global debe bajar a `64` y la notificación #51 más antigua debe pasar a ocupar el puesto #50 al recargar la lista.
* **EC-002** : Abrir el panel y presionar `Esc` repetidamente o hacer clic rápido sobre el ícono de la campana (toggle) -> El estado abierto/cerrado del componente `notificationBell.js` responde sin quedar bloqueado ni duplicar peticiones concurrentes.
* **EC-003** : Caracteres especiales, tildes y signos de puntuación en el motivo (ej. `"Matrícula vencida; revisión técnica #4 & inspección"`) -> Se renderizan como texto plano seguro sin romper el HTML del panel ni ejecutar scripts (prevención XSS).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |