### Test Case Specification: Consultar Detalles de Embarcación - Supervisión Global y Filtros (Rol Administrador)
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-detalles/spec.md` / `docs/features/consultar-detalles/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-002-ConsultarDetallesEmbarcacion-FlujoAdministrador
**Product Name & Version** : SeaShare - Módulo 1 Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica que un **Administrador** autenticado pueda filtrar embarcaciones en la pantalla "Control General de Flota Fluvial", consultar la vista de detalle ("Ver Detalles") de cualquier embarcación registrada en la flota, validar la ausencia del botón "Editar" y de la tarjeta de propietario, verificar el estado de los botones de mantenimiento y eliminación, y confirmar que al presionar "Volver" (incluyendo tras recargar la página o entrar por enlace directo) el sistema siempre regrese a la tabla general en su estado por defecto sin filtros.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Detalles de Embarcación | Documento de Especificación | `docs/features/consultar-detalles/spec.md` |
| Plan de implementación: Consultar detalles de embarcación | Documento de Plan Técnico | `docs/features/consultar-detalles/plan.md` |
| Contrato REST: Consultar detalles | Documento de Contrato API | `docs/features/consultar-detalles/contract.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* En la vista de detalle del Administrador, los botones "Enviar a Mantenimiento", "Terminar mantenimiento" y "Eliminar" se verifican únicamente en cuanto a su presencia, etiqueta y estado habilitado/deshabilitado; sus flujos de ejecución con modales y motivos pertenecen a sus respectivas especificaciones.
* La barra de búsqueda de propietario por nombre o cédula en "Control General de Flota Fluvial" pertenece al spec "Consultar información de propietario" y se valida únicamente que no filtre ni reordene la tabla de embarcaciones.

##### Dependencies & Assumptions
* Los endpoints de backend `/api/v1/admin/fleet/boats`, `/api/v1/admin/fleet/boats/{boatId}` y `/api/v1/admin/fleet/owners` están desplegados y restringidos al rol `ADMIN`.
* Existen múltiples propietarios (`1010000002` a `1010000006`) con embarcaciones registradas en distintos estados operativos.

##### Preconditions & Default Setup
* **Initial State** : El Administrador tiene sesión iniciada y se encuentra ubicado en "Control General de Flota Fluvial" (`/admin/home`).
* **Environment** : Entorno Web Local / Staging (Frontend puerto `5500`, Backend puerto `8081`).
* **Test Accounts** : 
  * Administrador: `1010000001` / Contraseña: `SeaShare2026!`
  * Propietario (para pruebas de restricción de acceso entre roles): `1010000002` / Contraseña: `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Comportamiento de Referencia de Filtros en "Control General de Flota Fluvial"
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar listado inicial por defecto | Ingresar a "Control General de Flota Fluvial" sin filtros activos | `GET /api/v1/admin/fleet/boats` | La tabla muestra todas las embarcaciones registradas (excluyendo `Borrador`) ordenadas de la más reciente a la más antigua. El filtro por propietario ofrece únicamente a los propietarios que tienen embarcaciones registradas. | [ ] | [ ] |
| 2 | Aplicar filtro excluyente por propietario | Seleccionar un propietario específico en el filtro por propietario | Propietario `1010000002` | La tabla excluye las embarcaciones de los demás propietarios y muestra únicamente las de `1010000002`. | [ ] | [ ] |
| 3 | Combinar filtro por propietario y filtro por estado | Manteniendo el filtro de propietario, seleccionar un estado operativo en el filtro por estado | Propietario `1010000002` + Estado `Disponible` | Los criterios se combinan: se muestran únicamente las embarcaciones de `1010000002` que están en estado `Disponible`. Las opciones del filtro por estado son `Todos` y los 4 estados válidos (`Borrador` no aparece). | [ ] | [ ] |
| 4 | Verificar mensaje cuando no hay coincidencias | Seleccionar una combinación de propietario y estado sin registros | Combinación sin embarcaciones | La tabla muestra el mensaje **"No se encontraron embarcaciones con esos filtros"**. | [ ] | [ ] |
| 5 | Verificar independencia de la barra de búsqueda de propietario | Escribir un nombre o cédula en la barra de búsqueda de propietario | Texto: `1010000002` | Escribir en esta barra **no** filtra ni reordena las filas de la tabla de embarcaciones. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Visualización de Detalles y Reglas de Botones para Administrador
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Abrir "Ver Detalles" de cualquier embarcación desde tabla filtrada | Con filtros activos en "Control General de Flota Fluvial", hacer clic en **"Ver Detalles"** de una embarcación | Embarcación en estado `Disponible` (`GET /api/v1/admin/fleet/boats/{boatId}`) | El sistema navega a `/admin/boats/:boatId` y muestra la ficha completa de la embarcación (idéntica en estructura y formato a la del Propietario, sin mostrar ninguna tarjeta del propietario). | [ ] | [ ] |
| 2 | Verificar ausencia de botón "Editar" y presencia de acciones de Administrador | Inspeccionar la barra de botones de acción en estado `Disponible` | Estado: `Disponible` | El botón **"Editar"** **no existe** en la interfaz. Se muestran únicamente **"Enviar a Mantenimiento"** y **"Eliminar"**, en ese orden y habilitados, sin tooltips. | [ ] | [ ] |
| 3 | Verificar botón de mantenimiento en estado `Mantenimiento` | Abrir "Ver Detalles" de una embarcación en estado `Mantenimiento` | Embarcación en estado `MAINTENANCE` | Aparecen únicamente **"Terminar mantenimiento"** y **"Eliminar"** (habilitados). El botón **"Editar"** no existe en la interfaz. | [ ] | [ ] |
| 4 | Verificar botones deshabilitados en `Reservado` y `En Navegación` | Abrir "Ver Detalles" de embarcaciones en `Reservado` y `En Navegación` | Embarcaciones en `RESERVED` y `NAVIGATION` | Aparecen **"Enviar a Mantenimiento"** y **"Eliminar"** deshabilitados, sin tooltips y con icono de prohibido al pasar el cursor. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Comportamiento de "Volver", Recarga y Enlaces Directos
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar regreso a tabla sin filtros tras navegación normal | Habiendo llegado al detalle desde una tabla filtrada (Paso 1 de Sección 2), hacer clic en **"Volver"** | Clic en "Volver" | El sistema regresa a "Control General de Flota Fluvial" en su estado por defecto, **sin filtros aplicados** (mostrando toda la flota). | [ ] | [ ] |
| 2 | Verificar comportamiento tras recargar la página | En `/admin/boats/:boatId`, recargar el navegador y luego presionar **"Volver"** | Recarga del navegador + Clic en "Volver" | Al recargar, la pantalla vuelve a consultar los datos actualizados al servidor (`GET /api/v1/admin/fleet/boats/{boatId}`). Al pulsar "Volver", regresa a "Control General de Flota Fluvial" en su estado por defecto sin filtros. | [ ] | [ ] |
| 3 | Verificar acceso por enlace directo compartido | Abrir directamente `/admin/boats/:boatId` en una pestaña nueva como Administrador y presionar **"Volver"** | URL directa `/admin/boats/:boatId` | Carga la información de la embarcación y al presionar "Volver" lleva a "Control General de Flota Fluvial" en su estado por defecto sin filtros. | [ ] | [ ] |
| 4 | Verificar manejo de embarcación inexistente, eliminada o en `Borrador` | Abrir `/admin/boats/{idInexistenteOBorrador}` como Administrador | UUID inexistente, eliminado o en `DRAFT` (HTTP 404) | Muestra el mensaje **"No se pudo cargar la información de la embarcación"** con el botón **"Volver"**, el cual lleva a "Control General de Flota Fluvial" en su estado por defecto sin filtros. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : ¿Qué ocurre si un Administrador intenta consumir directamente el endpoint de detalle del Propietario `GET /api/v1/fleet/boats/{boatId}`? -> El backend rechaza la solicitud con `HTTP 403 Forbidden`.
* **EC-002** : ¿Qué ocurre si un Propietario (`1010000002`) intenta acceder a `/admin/boats/:boatId` o invocar `GET /api/v1/admin/fleet/boats/{boatId}`? -> El backend rechaza la solicitud con `HTTP 403 Forbidden`.
* **EC-003** : ¿Qué ocurre cuando se envían parámetros de filtro inválidos a `GET /api/v1/admin/fleet/boats`? -> El backend responde con `HTTP 400 Bad Request` indicando el campo inválido.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | Caída del sistema, corrupción de datos o bloqueo total de un flujo crítico del usuario sin solución alternativa. |
| **Major** | **M** | Funcionalidad principal dañada o pérdida significativa de funcionalidad, pero existe una solución alternativa compleja. |
| **Small / Minor** | **S** | Problema cosmético, desalineación menor en la interfaz, error tipográfico o defecto no bloqueante. |