### Test Case Specification: Consultar Detalles de Embarcación - Formato de Datos, Carga y Manejo de Errores
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-detalles/spec.md` / `docs/features/consultar-detalles/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-003-ConsultarDetallesEmbarcacion-FormatoYResiliencia
**Product Name & Version** : SeaShare - Módulo 1 Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica los estándares de formato visual de los datos (tarifa en moneda local, coordenadas GPS, capacidad de pasajeros, badges de estado, imagen por defecto), los estados de carga tipo esqueleto, la consulta siempre actualizada al servidor (sin caché), la adaptación responsiva hasta `360 px` de ancho y el manejo ordenado ante fallos de red, errores de servidor o expiración de sesión para ambos roles (Propietario y Administrador).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Detalles de Embarcación | Documento de Especificación | `docs/features/consultar-detalles/spec.md` |
| Plan de implementación: Consultar detalles de embarcación | Documento de Plan Técnico | `docs/features/consultar-detalles/plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La verificación completa del mensaje de sesión expirada (`"Tu sesión ha expirado. Por favor, ingresa de nuevo."`) y la redirección de retorno tras iniciar sesión se coordinan con la especificación de inicio de sesión (pendiente T081 en el plan técnico).
* El umbral de rendimiento (< 1,5 segundos en el 95% de los accesos) mide únicamente la carga de los datos de texto y excluye la descarga de la fotografía.

##### Dependencies & Assumptions
* Se dispone de herramientas de desarrollo del navegador (estrangulamiento de red, modo sin conexión, emulación de resolución) o proxy de API para simular latencia, respuestas HTTP 500/401 y URLs de imágenes rotas.

##### Preconditions & Default Setup
* **Initial State** : Sesión de usuario válida (`1010000002` para Propietario o `1010000001` para Administrador) con embarcaciones de prueba configuradas con valores límite de formato.
* **Environment** : Entorno Web Local / Staging (Frontend `5500`, Backend `8081`).
* **Test Accounts** : 
  * Propietario: `1010000002` / Contraseña: `SeaShare2026!`
  * Administrador: `1010000001` / Contraseña: `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Carga Inicial, Actualización de Datos y Formatos Visuales
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar estado de carga tipo esqueleto | Aplicar estrangulamiento de red (Slow 3G) y abrir "Ver Detalles" | Cualquier embarcación válida | Mientras la petición está en curso, se muestra un estado de carga tipo esqueleto (`role="status"`), sin spinners ni indicadores llamativos. En condiciones normales de red, los datos de texto cargan en menos de 1,5 segundos. | [ ] | [ ] |
| 2 | Verificar consulta fresca al servidor tras edición | Editar una embarcación, confirmar el guardado y regresar a "Ver Detalles" | Cambio en nombre o tarifa de embarcación | La pantalla ejecuta una nueva consulta `GET` al servidor y muestra los valores actualizados, sin usar caché de la lista ni de visitas previas. | [ ] | [ ] |
| 3 | Verificar formato de tarifa base en diversos rangos | Consultar embarcaciones con tarifas de 3, 6, 7 y 10 dígitos | `950`, `180000`, `1500000`, `9999999999` | Se muestran respectivamente como `$ 950`, `$ 180.000`, `$ 1.500.000` y `$ 9.999.999.999`. Siempre con `$ ` al inicio, puntos de miles, sin decimales, sin "COP" al lado del valor y sin periodicidad. | [ ] | [ ] |
| 4 | Verificar formato de puerto y coordenadas GPS | Consultar embarcaciones con coordenadas enteras, de pocos decimales y más de 4 decimales | Puerto: `Bahía Concha`<br>Latitud: `11` / Longitud: `-74.12346` | La tarjeta puerto muestra únicamente **Bahía Concha** en negrita. La tarjeta **Latitud** muestra `11.0000` (relleno con ceros) y la tarjeta **Longitud** muestra `-74.1235` (redondeado a 4 decimales, con signo y punto decimal). | [ ] | [ ] |
| 5 | Verificar imagen por defecto ante error de carga de fotografía | Consultar embarcación cuya URL de fotografía devuelve 404 o está vacía | `photo_url` roto o nulo | Se renderiza un contenedor con la imagen por defecto de embarcación sin romper ni desplazar la disposición de las demás tarjetas. | [ ] | [ ] |
| 6 | Verificar diseño responsivo a `360 px` de ancho | Redimensionar la ventana del navegador a `360 px` de ancho en "Ver Detalles" | Viewport `360x800` | En pantallas anchas las tarjetas van en dos columnas (foto y servicios a la izquierda, datos a la derecha); a `360 px` todo se apila verticalmente sin desbordamiento horizontal, incluso con tarifa `$ 9.999.999.999`. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Manejo de Errores de Red, Servidor y Sesión Expirada
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular fallo de red o error 500 durante la carga | Desconectar red (Offline) o simular HTTP 500 en `GET /boats/{boatId}` al abrir "Ver Detalles" | Error de red / HTTP 500 | Se muestra el contenedor de error (`role="alert"`) con el texto **"No se pudo cargar la información de la embarcación"** y los botones **"Reintentar"** y **"Volver"**. | [ ] | [ ] |
| 2 | Verificar funcionamiento del botón "Reintentar" | Restablecer la conexión de red y hacer clic en **"Reintentar"** | Clic en "Reintentar" | La pantalla vuelve al estado de carga tipo esqueleto, repite la consulta al servidor (sin límite de reintentos) y carga los datos correctamente. | [ ] | [ ] |
| 3 | Simular sesión expirada durante la carga inicial | Invalidar token/sesión (HTTP 401) antes de abrir "Ver Detalles" | Sesión expirada (HTTP 401) | El sistema no muestra el error de red; guarda la dirección actual como destino de retorno interno y redirige al inicio de sesión mostrando **"Tu sesión ha expirado. Por favor, ingresa de nuevo."** | [ ] | [ ] |
| 4 | Simular sesión expirada al presionar "Reintentar" | Tras un fallo de red (Paso 1), invalidar la sesión (HTTP 401) y presionar **"Reintentar"** | Fallo de red seguido de HTTP 401 al reintentar | El sistema prioriza la redirección al inicio de sesión (FR-007) en lugar de mantener el error de red. | [ ] | [ ] |
| 5 | Simular sesión expirada con la pantalla ya abierta | Con "Ver Detalles" cargado, expirar la sesión e intentar ejecutar una acción o navegar | Sesión expirada en pantalla abierta | El sistema redirige al usuario a la pantalla de inicio de sesión. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : ¿Qué ocurre si se solicita un identificador con formato inválido que no sea UUID (por ejemplo, `/owner/boats/123-invalido` o `/admin/boats/abc`)? -> El backend responde `HTTP 400 Bad Request` y la pantalla lo trata exactamente igual que un `404`: muestra `"No se pudo cargar la información de la embarcación"` con **únicamente** el botón de retorno (`"Volver a Mis Embarcaciones"` o `"Volver"`), sin mostrar `"Reintentar"`.
* **EC-002** : ¿Qué ocurre si un campo obligatorio llega vacío o nulo desde la base de datos (por ejemplo, puerto ausente)? -> El backend no arroja error `500`; la interfaz renderiza `"—"` en ese campo específico mientras muestra el resto de la pantalla con normalidad.
* **EC-003** : ¿Qué ocurre si se abre un modal (Mantenimiento o Eliminar) desde esta pantalla y el usuario presiona `Esc` o hace clic fuera del modal? -> El modal permanece abierto y únicamente responde a sus botones explícitos.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | Caída del sistema, corrupción de datos o bloqueo total de un flujo crítico del usuario sin solución alternativa. |
| **Major** | **M** | Funcionalidad principal dañada o pérdida significativa de funcionalidad, pero existe una solución alternativa compleja. |
| **Small / Minor** | **S** | Problema cosmético, desalineación menor en la interfaz, error tipográfico o defecto no bloqueante. |