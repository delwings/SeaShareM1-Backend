### Test Case Specification: Búsqueda de Propietarios en Control General de Flota Fluvial
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-informacion-propietario/spec.md` | `docs/features/consultar-informacion-propietario/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-001-BusquedaPropietarios
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la barra de búsqueda de propietarios en la pantalla "Control General de Flota Fluvial" para el rol Administrador (`ADMIN`), incluyendo el umbral de 3 a 100 caracteres, el debounce de `300 ms`, la coincidencia parcial sin distinguir mayúsculas/tildes, la búsqueda por documento ignorando puntos/comas/espacios, la inclusión de propietarios sin embarcaciones, la exclusión de administradores, el listado separado de homónimos, el tope de 5 resultados ordenados alfabéticamente con aviso de más coincidencias, los estados de carga (esqueleto), sin resultados y error de búsqueda, el cierre de la ventana desplegable, la navegación por teclado, la independencia frente a los filtros de la tabla y la no exposición del término de búsqueda en la URL (US1, FR-001 a FR-015, NFR-001 a NFR-004, SC-001 a SC-004).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Información de Propietario | Specification Document | `docs/features/consultar-informacion-propietario/spec.md` |
| Implementation Plan: Consultar Información de Propietario | Plan Document | `docs/features/consultar-informacion-propietario/plan.md` |
| Contrato REST: Consultar Información de Propietario | API Contract | `docs/architecture/contracts/rest/consultar-informacion-propietario.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La ventana desplegable de resultados no es un modal; a diferencia de los modales del Módulo 1, sí se cierra con `Esc`, clic fuera o al borrar la barra (FR-011, [S-08]).
* El registro de auditoría de consultas (`FR-032`) está sin implementar y requiere confirmación/revisión legal (Decisión D8).
* La búsqueda es independiente de la tabla y no utiliza el parámetro `q` de `/admin/fleet/boats` (Plan, FR-013).

##### Dependencies & Assumptions
* Backend (`SeaShareM1`) y Frontend (`SeaShareM1-Frontend`) desplegados y autenticados con JWT; `AdminOwnersService`, `AdminOwnersController` y `ownerSearch.js` operativos.
* Datos semilla disponibles (clave `SeaShare2026!`): Administrador `1010000001` (`admin@seashare.com`) y propietarios `1010000002` a `1010000006`, incluyendo al menos un propietario sin embarcaciones, dos propietarios con el mismo nombre y distintos documentos, y al menos 6 propietarios coincidentes para probar el tope de 5 + `has_more`.
* DevTools disponible para simular fallo de red/servidor en el endpoint de búsqueda de propietarios.

##### Preconditions & Default Setup
* **Initial State** : Administrador autenticado ubicado en "Control General de Flota Fluvial" (`/admin`), con barra de búsqueda vacía y ventana de resultados cerrada.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `admin@seashare.com` (usuario `1010000001`, rol `ADMIN`) y `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Barra de búsqueda, coincidencia, resultados, límites e independencia de tabla (Escenarios US1-1 a US1-9 y US1-14)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar barra de búsqueda disponible en inicio de Admin | Ingresar como `admin@seashare.com` a "Control General de Flota Fluvial" | `admin@seashare.com` | Se muestra la barra con el texto de ayuda **"Buscar propietario por nombre o cédula..."**; los usuarios sin rol `ADMIN` no ven esta barra (FR-001, NFR-001). | [ ] | [ ] |
| 2 | Validar umbral mínimo (< 3 caracteres) y máximo (> 100 caracteres) | Escribir 1 o 2 caracteres en la barra; luego escribir más de 100 caracteres (o invocar consulta directa con `< 3` / `> 100`) | `"fe"`, `"10"`, texto de 101 caracteres | Con `< 3` caracteres no se envía petición ni se abre la ventana de resultados; el servidor rechaza con `400` un texto de `< 3` o `> 100` caracteres (FR-002, Decisión D3). | [ ] | [ ] |
| 3 | Buscar por nombre parcial sin tildes/mayúsculas y por cédula formateada | Escribir `"fernan"` para `"Fernando Fernández"`; luego escribir `"1042389104"` y `"1.042.389.104"` | `"fernan"`, `"1042389104"`, `"1.042.389.104"` | Tras esperar `300 ms` (debounce), aparece debajo de la barra la ventana con encabezado **"RESULTADOS DE BÚSQUEDA (PROPIETARIO)"**, avatar con iniciales, nombre, documento sin puntos (ej. `1042389104`) y botón **"Ver Detalles"** (FR-002 a FR-004, Decisión D4/D5). | [ ] | [ ] |
| 4 | Verificar inclusión de propietarios sin embarcaciones, exclusión de admins y homónimos | Buscar un propietario sin embarcaciones, buscar el nombre/documento de `admin@seashare.com` y buscar un nombre compartido por dos propietarios | Propietario sin barcos, `1010000001`, dos `OWNER` con mismo nombre | Aparece el propietario sin embarcaciones; el administrador no aparece; los dos propietarios con el mismo nombre se muestran por separado con su respectivo documento (FR-005, FR-006, Decisión D2). | [ ] | [ ] |
| 5 | Verificar tope de 5 resultados, orden alfabético y aviso de más coincidencias | Escribir un término que coincida con más de 5 propietarios | Término con `>= 6` coincidencias `OWNER` | Se muestran máximo `5` resultados en orden alfabético por nombre (y por documento si hay empate) junto con el aviso **"Hay más resultados. Escribe más caracteres para precisar la búsqueda."** (FR-007, Decisión D3). | [ ] | [ ] |
| 6 | Verificar sin resultados e independencia frente a filtros de la tabla | Escribir un término sin coincidencias; luego aplicar o cambiar filtros en la tabla de "Control General de Flota Fluvial" | `"zzz-sin-coincidencia-999"`, filtros de tabla | Muestra **"No se encontraron propietarios con esa búsqueda."**; la tabla de "Control General de Flota Fluvial" no se filtra ni se reordena al escribir o cambiar resultados (FR-008, FR-013). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Esqueleto de carga, error de búsqueda, cierre, teclado y URL (Escenarios US1-10 a US1-13)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar esqueleto de carga y error de búsqueda con Reintentar | Simular latencia y luego modo *Offline* o error `500` en la búsqueda de propietarios; pulsar **"Reintentar"** | Latencia, modo *Offline* / `500` | Durante la carga muestra estado esqueleto (sin spinners); al fallar muestra **"No se pudo realizar la búsqueda"** con botón **"Reintentar"** que repite la consulta (FR-009, FR-010, NFR-002). | [ ] | [ ] |
| 2 | Verificar cierre de la ventana desplegable | Abrir ventana de resultados y probar: (a) presionar `Esc`, (b) hacer clic fuera de la ventana y (c) borrar el texto de la barra | `Esc`, clic externo, borrar input | La ventana de resultados se cierra en los tres casos (FR-011, [S-08]). | [ ] | [ ] |
| 3 | Verificar navegación por teclado y no exposición en URL | Abrir ventana de resultados, usar `Flecha Abajo` / `Flecha Arriba`, pulsar `Enter` y revisar la barra de direcciones | Teclas `ArrowDown`, `ArrowUp`, `Enter` | Las flechas resaltan el resultado y `Enter` abre su "Información del Propietario"; el término buscado no viaja en la URL de ninguna pantalla (FR-012, FR-014, NFR-003). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Espacios sobrantes, cédula con puntos/comas, mezcla alfanumérica y caracteres especiales)** : Escribir espacios iniciales/finales o múltiples, cédula con puntos/comas/espacios (`"1.042,389 104"`), mezcla de letras y números o caracteres especiales (`"@#$"`): el sistema ignora espacios sobrantes, busca solo dígitos en documento y trata caracteres especiales como texto normal sin generar error (FR-003, Edge Cases).
* **EC-002 (Propietario sin embarcaciones frente al filtro de la tabla y vista a 360 px)** : Un propietario sin embarcaciones aparece en la ventana de búsqueda pero no figura en el selector de filtro por propietario de la tabla; a `360 px` de ancho, la ventana ocupa el ancho de la barra y desplaza su contenido sin cortes (FR-005, NFR-004, Edge Cases).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |