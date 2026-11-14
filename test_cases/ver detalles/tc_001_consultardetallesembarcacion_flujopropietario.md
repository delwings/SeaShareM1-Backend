### Test Case Specification: Consultar Detalles de Embarcación - Flujo Principal y Gestión de Acciones (Rol Propietario)
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-detalles/spec.md` / `docs/features/consultar-detalles/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-001-ConsultarDetallesEmbarcacion-FlujoPropietario
**Product Name & Version** : SeaShare - Módulo 1 Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica que un **Propietario** autenticado pueda acceder a la pantalla "Ver Detalles" de sus propias embarcaciones registradas desde "Mis Embarcaciones Registradas", visualizar los datos operativos y logísticos completos con el formato requerido y comprobar la presencia, etiquetas y estado habilitado o deshabilitado de los botones de acción ("Editar", "Enviar a Mantenimiento" / "Terminar mantenimiento", "Eliminar") en los cuatro estados operativos válidos (`Disponible`, `Reservado`, `En Navegación`, `Mantenimiento`).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Detalles de Embarcación | Documento de Especificación | `docs/features/consultar-detalles/spec.md` |
| Plan de implementación: Consultar detalles de embarcación | Documento de Plan Técnico | `docs/features/consultar-detalles/plan.md` |
| Contrato REST: Consultar detalles | Documento de Contrato API | `docs/features/consultar-detalles/contract.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Para los botones de acción ("Editar", "Enviar a Mantenimiento", "Terminar mantenimiento", "Eliminar") se valida su presencia, orden, etiqueta, estado habilitado/deshabilitado y comportamiento visual del cursor; la ejecución interna de sus flujos corresponde a sus respectivas especificaciones.
* El comportamiento responsivo debe verificarse hasta un ancho de pantalla de `360 px`.

##### Dependencies & Assumptions
* La funcionalidad "Registrar Embarcación" está operativa y existen embarcaciones semilla registradas en los cuatro estados válidos (`AVAILABLE`, `RESERVED`, `NAVIGATION`, `MAINTENANCE`) así como en estado `DRAFT`.
* El sistema utiliza el diccionario único de cuatro estados (`Disponible`, `Reservado`, `En Navegación`, `Mantenimiento`).

##### Preconditions & Default Setup
* **Initial State** : El propietario tiene sesión iniciada y se encuentra ubicado en la pantalla "Mis Embarcaciones Registradas". La base de datos cuenta con embarcaciones asociadas al usuario `1010000002` en los estados `AVAILABLE`, `RESERVED`, `NAVIGATION`, `MAINTENANCE` y `DRAFT`, además de una embarcación perteneciente al usuario `1010000003`.
* **Environment** : Entorno Web Local / Staging (Frontend puerto `5500`, Backend API puerto `8081` mediante `GET /api/v1/fleet/boats/{boatId}`).
* **Test Accounts** : 
  * Propietario Principal: `1010000002` / Contraseña: `SeaShare2026!`
  * Propietario Secundario (para pruebas de acceso no autorizado): `1010000003` / Contraseña: `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Acceso, Visualización de Datos y Formato (Estado: Disponible)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Navegar al detalle de una embarcación propia en estado `Disponible` | En "Mis Embarcaciones Registradas", hacer clic en **"Ver Detalles"** de una embarcación propia con estado `Disponible` | Embarcación en estado `AVAILABLE` del propietario `1010000002` (`GET /api/v1/fleet/boats/{boatId}`) | El sistema navega a `/owner/boats/:boatId`, muestra brevemente un estado de carga tipo esqueleto (sin spinners) y consulta datos actualizados al servidor (HTTP 200). | [ ] | [ ] |
| 2 | Verificar encabezado, badge de estado y fotografía oficial | Revisar cabecera y columna izquierda de la pantalla "Ver Detalles" | Nombre: ej. "La Perla", Estado: `Disponible`, Fotografía válida | Se muestra el nombre de la embarcación, el badge `Disponible` con un círculo sólido a la izquierda alineado con el texto, y la fotografía oficial con texto alternativo (`alt`) igual al nombre de la embarcación. | [ ] | [ ] |
| 3 | Verificar formato de tarjetas de datos operativos y ubicación | Revisar tarjetas de datos en la columna derecha | Matrícula: `MAT-001`, Tipo: `Lancha`, Capacidad: `12`, Tarifa: `180000`, Puerto: `Muelle Turístico`, Latitud: `10.745`, Longitud: `-74.7891` | - **Matrícula Legal**: `MAT-001`<br>- **Tipo de Embarcación**: `Lancha`<br>- **Capacidad máxima**: `12 pasajeros`<br>- **Tarifa base (COP)**: `$ 180.000` (con puntos de miles, sin decimales, sin texto "COP" junto al valor y sin mención a periodicidad por hora o por día)<br>- **Puerto de atraque y ubicación GPS**: muestra únicamente **Muelle Turístico** en negrita (tarjeta de ancho completo)<br>- **Latitud**: `10.7450` (tarjeta propia)<br>- **Longitud**: `-74.7891` (tarjeta propia) | [ ] | [ ] |
| 4 | Verificar sección de servicios incluidos | Revisar sección de servicios en la columna izquierda | Servicios adicionales seleccionados: ej. `Chalecos salvavidas`, `Nevera con hielo` | Se muestran marcados y fijos los servicios base (**Capitán** y **Combustible**) junto con los servicios adicionales seleccionados. | [ ] | [ ] |
| 5 | Verificar barra de botones en estado `Disponible` | Observar orden y estado de los botones de acción | Estado actual: `Disponible` | Se muestran exactamente 3 botones en este orden: **"Editar"**, **"Enviar a Mantenimiento"** y **"Eliminar"**. Los tres botones están habilitados y ninguno muestra tooltips al pasar el cursor. | [ ] | [ ] |
| 6 | Verificar navegación de retorno | Hacer clic en el botón **"Volver"** | Clic en "Volver" | El sistema redirige al propietario a la pantalla "Mis Embarcaciones Registradas". | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Comportamiento de Botones en Estados Reservado, En Navegación y Mantenimiento
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar embarcación en estado `Reservado` | Abrir "Ver Detalles" de una embarcación propia en estado `Reservado` | Embarcación en estado `RESERVED` | El badge muestra `Reservado`. Los botones **"Editar"**, **"Enviar a Mantenimiento"** y **"Eliminar"** aparecen deshabilitados. | [ ] | [ ] |
| 2 | Verificar cursor y ausencia de tooltips en botones deshabilitados | Pasar el puntero del mouse sobre "Editar", "Enviar a Mantenimiento" y "Eliminar" en estado `Reservado` | Hover sobre botones deshabilitados | No aparece ningún tooltip. El cursor cambia al icono de prohibido (círculo rojo con línea diagonal). | [ ] | [ ] |
| 3 | Consultar embarcación en estado `En Navegación` | Abrir "Ver Detalles" de una embarcación propia en estado `En Navegación` | Embarcación en estado `NAVIGATION` | El badge muestra `En Navegación`. Los botones **"Editar"**, **"Enviar a Mantenimiento"** y **"Eliminar"** aparecen deshabilitados, sin tooltips y con icono de prohibido al pasar el cursor. | [ ] | [ ] |
| 4 | Consultar embarcación en estado `Mantenimiento` | Abrir "Ver Detalles" de una embarcación propia en estado `Mantenimiento` | Embarcación en estado `MAINTENANCE` | El badge muestra `Mantenimiento`. En la barra de botones aparecen, en orden: **"Editar"** (habilitado), **"Terminar mantenimiento"** (habilitado, ocupando el lugar de "Enviar a Mantenimiento") y **"Eliminar"** (habilitado). | [ ] | [ ] |
| 5 | Verificar acción del botón "Editar" habilitado | Hacer clic en **"Editar"** desde una embarcación en `Disponible` o `Mantenimiento` | Clic en "Editar" | El sistema abre la pantalla de edición de la embarcación correspondiente. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Seguridad y Control de Acceso del Propietario
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Intentar consultar embarcación perteneciente a otro propietario por enlace directo | Estando autenticado como `1010000002`, navegar directamente a la URL `/owner/boats/{boatIdAjeno}` | UUID de embarcación perteneciente a `1010000003` (`GET /api/v1/fleet/boats/{boatIdAjeno}` -> HTTP 404) | No se carga ni renderiza ningún dato de la embarcación. Se muestra únicamente la pantalla de error con texto plano **"No se pudo cargar la información de la embarcación"** (sin mencionar permisos ni dueños) y únicamente el botón **"Volver a Mis Embarcaciones"** (sin botón "Reintentar"). | [ ] | [ ] |
| 2 | Intentar consultar embarcación en estado `Borrador` por enlace directo | Navegar directamente a `/owner/boats/{boatIdBorrador}` | UUID de embarcación propia en estado `DRAFT` (HTTP 404) | El sistema la trata exactamente igual que una inexistente: muestra **"No se pudo cargar la información de la embarcación"** y únicamente el botón **"Volver a Mis Embarcaciones"**. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : ¿Qué ocurre cuando una embarcación no tiene servicios adicionales seleccionados? -> Se mantienen visibles los servicios base (**Capitán** y **Combustible**), y en el área de servicios adicionales se muestra el texto exacto `"Sin servicios adicionales seleccionados"`.
* **EC-002** : ¿Qué ocurre cuando la capacidad máxima de la embarcación es `1`? -> La tarjeta "Capacidad máxima" muestra `"1 pasajero"` en singular.
* **EC-003** : ¿Qué ocurre cuando la tarifa base es el valor máximo permitido (`9999999999`) en una pantalla móvil de `360 px` de ancho? -> La tarjeta muestra `"$ 9.999.999.999"` completo sin cortarse ni desbordar el diseño, y las tarjetas se apilan en vertical.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | Caída del sistema, corrupción de datos o bloqueo total de un flujo crítico del usuario sin solución alternativa. |
| **Major** | **M** | Funcionalidad principal dañada o pérdida significativa de funcionalidad, pero existe una solución alternativa compleja. |
| **Small / Minor** | **S** | Problema cosmético, desalineación menor en la interfaz, error tipográfico o defecto no bloqueante. |