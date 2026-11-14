### Test Case Specification: Eliminación de Embarcación con Motivo y Generación de Notificación
**Created** : 14 de noviembre de 2026  **Spec/Plan Link** : `notificaciones-spec.md` / `notificaciones-plan.md`  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-001-NotificacionesPropietario-EliminacionYMotivo
**Product Name & Version** : SeaShare v1.0 (Módulo 1 – Gestión de Flota)
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica que el flujo de eliminación de embarcaciones por parte de un Administrador exija un motivo obligatorio (1 a 300 caracteres con recorte de espacios), persista y genere una notificación (`VESSEL_DELETED`) en la campana del Propietario correspondiente con el formato de fecha/hora de Colombia y el motivo exacto, y que la eliminación realizada por el propio Propietario no solicite motivo ni genere notificaciones.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Notificaciones del Propietario (campana) | Specification Document | `notificaciones-spec.md` |
| Plan de implementación: Notificaciones del Propietario | Plan Document | `notificaciones-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La actualización del contador de la campana no es en tiempo real (sin WebSockets/push); depende de la carga inicial de pantalla, la apertura del panel o el ciclo de sondeo de 60 segundos.

##### Dependencies & Assumptions
* El esquema de base de datos MySQL cuenta con la tabla `notification` y el tipo `VESSEL_DELETED` (migración V8 aplicada).
* Existen embarcaciones activas asociadas a los propietarios semilla para ejecutar las eliminaciones.

##### Preconditions & Default Setup
* **Initial State** : Usuario sin sesión iniciada; base de datos con datos semilla cargados (embarcaciones activas como "Río Magdalena I" con matrícula `CP-05-1042-A` asignada al propietario `1010000002`, y una segunda embarcación asignada al propietario `1010000003`).
* **Environment** : Entorno QA / Staging con Backend Spring Boot (`seasharem1`) y Frontend desplegados; zona horaria de negocio configurada en hora de Colombia (`America/Bogota`).
* **Test Accounts** : 
  * Administrador: `1010000001` / `SeaShare2026!`
  * Propietario 1 (Offline durante eliminación): `1010000002` / `SeaShare2026!`
  * Propietario 2 (Eliminación propia): `1010000003` / `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Eliminación por Administrador con Motivo Válido y Recorte de Espacios (FR-001, FR-002, SC-001)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Iniciar sesión como Administrador | Navegar a `/login`, ingresar credenciales de administrador y enviar formulario. | Usuario: `1010000001` <br> Clave: `SeaShare2026!` | Inicio de sesión exitoso; redirección al panel de administración. En el header **no** se muestra el icono de campana de notificaciones. | [ ] | [ ] |
| 2 | Abrir detalle y modal de eliminación de embarcación | Navegar al detalle de la embarcación "Río Magdalena I" (`adminBoatDetail`) y hacer clic en el botón de eliminar. | Embarcación: `Río Magdalena I` (`CP-05-1042-A`) | Se despliega el modal "¿Eliminar embarcación?" incluyendo el campo obligatorio "Motivo de la eliminación" y un contador de caracteres en `0/300`. | [ ] | [ ] |
| 3 | Validar límite máximo de caracteres en el campo motivo | Intentar escribir o pegar una cadena de 305 caracteres en el campo "Motivo de la eliminación". | Texto de 305 caracteres | El campo bloquea la entrada a partir del carácter 300, conserva exactamente 300 caracteres y el contador muestra `300/300`. | [ ] | [ ] |
| 4 | Ingresar motivo válido con espacios al inicio/final y confirmar | Borrar el texto anterior, escribir el motivo con espacios laterales y hacer clic en "Sí, eliminar". | `   Las fotos no corresponden a la embarcación   ` | Se envía petición `DELETE` al endpoint de flota admin con el payload `AdminDeleteRequest` (`reason`). La embarcación es eliminada correctamente y se muestra confirmación en UI. | [ ] | [ ] |
| 5 | Verificar persistencia de la notificación en base de datos | Consultar la tabla `notification` para el destinatario propietario (`1010000002`). | `recipient_id` de `1010000002` | Existe un registro nuevo con tipo `VESSEL_DELETED`, estado no leída, y el campo mensaje contiene el motivo recortado (`trim`) sin espacios sobrantes. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Recepción y Visualización del Aviso en la Campana del Propietario (Historia 1, FR-001, SC-001)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Cerrar sesión de Administrador e iniciar como Propietario afectado | Cerrar sesión del usuario `1010000001` e iniciar sesión con el Propietario 1 (quien estaba desconectado durante la eliminación). | Usuario: `1010000002` <br> Clave: `SeaShare2026!` | El propietario ingresa a su vista principal. En la barra superior (`header`) aparece la campana con el contador incrementado reflejando la nueva notificación sin leer. | [ ] | [ ] |
| 2 | Abrir el panel de notificaciones | Hacer clic sobre el icono de la campana en el header (`notificationBell`). | N/A | Se despliega el panel de notificaciones mostrando la notificación resaltada como no leída, junto con la fecha y hora de creación del aviso. | [ ] | [ ] |
| 3 | Verificar el formato exacto del mensaje y hora de Colombia | Leer el contenido textual de la notificación recién generada. | Motivo: `Las fotos no corresponden a la embarcación` | El texto cumple exactamente el formato: `"Tu embarcación Río Magdalena I (CP-05-1042-A) fue eliminada por un administrador el [fecha en formato DD de mes de AAAA, hora de Colombia]. Motivo: Las fotos no corresponden a la embarcación"`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Eliminación Realizada por el Propio Propietario (Historia 1 Esc. 3, Historia 2 Esc. 3, FR-003, SC-001)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Iniciar sesión como Propietario 2 | Cerrar sesión previa e iniciar sesión con el Propietario 2. | Usuario: `1010000003` <br> Clave: `SeaShare2026!` | Sesión iniciada; anotar el valor actual del contador de la campana (ej. `0` o `N`). | [ ] | [ ] |
| 2 | Abrir modal de eliminación de su propia embarcación | Ir a la gestión de su embarcación activa y seleccionar la opción de eliminar. | Embarcación propia del usuario `1010000003` | Se abre el modal de confirmación de eliminación (`deleteBoatFlow`), verificando que **no** existe campo de "Motivo de la eliminación" ni contador `n/300`. | [ ] | [ ] |
| 3 | Confirmar eliminación propia y verificar ausencia de notificación | Confirmar la eliminación de la embarcación y abrir la campana en el header. | N/A | La embarcación se elimina; el contador de la campana mantiene el mismo valor previo (`0` o `N`) y al abrir el panel **no** se ha generado ninguna notificación nueva (`VESSEL_DELETED`). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Validación Frontend de Motivo Vacío / Solo Espacios)** : En el modal "¿Eliminar embarcación?" del administrador, al presionar "Sí, eliminar" con el campo vacío o compuesto únicamente por espacios en blanco, el sistema muestra el mensaje `"Escribe el motivo de la eliminación."` y bloquea la eliminación.
* **EC-002 (Validación Backend API - Motivo Nulo, Vacío o >300 Caracteres)** : Al invocar directamente el endpoint `DELETE` de administración con `reason` vacío (`""`), nulo (`null`), solo espacios o de 301 caracteres, el backend valida `AdminDeleteRequest` antes de buscar la embarcación y responde HTTP `400 Bad Request` con `errors[{field: "reason"}]`, conservando intacta la embarcación en base de datos (FR-002, SC-002).
* **EC-003 (Longitud Límite Exacta)** : Ingresar exactamente 1 carácter válido y exactamente 300 caracteres válidos debe ser aceptado por frontend y backend (`200/204`), generando el mensaje con el motivo completo sin truncamientos adicionales.
* **EC-004 (Fallo Técnico al Crear el Aviso en Backend)** : Si ocurre un fallo interno al intentar persistir la notificación después de eliminar la embarcación, la eliminación de la embarcación se conserva ejecutada y el error queda registrado en los logs del servidor sin revertir la operación administrativa.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |