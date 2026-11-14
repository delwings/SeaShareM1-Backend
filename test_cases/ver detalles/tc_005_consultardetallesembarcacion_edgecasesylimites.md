### Test Case Specification: Consultar Detalles de Embarcación - Edge Cases, Validaciones Límite y Resiliencia Técnica
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-detalles/spec.md` / `docs/features/consultar-detalles/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-005-ConsultarDetallesEmbarcacion-EdgeCasesYLimites
**Product Name & Version** : SeaShare - Módulo 1 Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba maestro consolida todas las condiciones límite, casos borde numéricos y de texto, transiciones de estado concurrentes, reglas de seguridad anti-enumeración, requisitos de accesibilidad y resiliencia ante fallos técnicos definidos en la especificación de **Consultar Detalles de Embarcación**.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Detalles de Embarcación (Edge Cases & NFRs) | Documento de Especificación | `docs/features/consultar-detalles/spec.md` |
| Plan de implementación: Consultar detalles de embarcación | Documento de Plan Técnico | `docs/features/consultar-detalles/plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La simulación de campos obligatorios ausentes en base de datos (por ejemplo, puerto nulo) o cambios concurrentes de estado requiere manipulación controlada de datos o dos sesiones de navegador simultáneas.

##### Dependencies & Assumptions
* Las reglas de validación del backend y las utilidades de formateo en `boatFormat.js` están activas.

##### Preconditions & Default Setup
* **Initial State** : Embarcaciones de prueba preparadas con valores límite (capacidad = 1, tarifa máxima = `9999999999`, coordenadas con `0`, `3` y `6` decimales, coordenadas negativas, campos opcionales/faltantes nulos).
* **Environment** : Entorno Web Local / Staging (`5500` / `8081`).
* **Test Accounts** : 
  * Propietario A: `1010000002` / Contraseña: `SeaShare2026!`
  * Propietario B: `1010000003` / Contraseña: `SeaShare2026!`
  * Administrador: `1010000001` / Contraseña: `SeaShare2026!`

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Validaciones Límite de Formato y Datos Faltantes
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Límite superior de tarifa base (`$ 9.999.999.999`) en pantalla estrecha | Abrir "Ver Detalles" de embarcación con tarifa `9999999999` en resolución de `360 px` | `baseRate = 9999999999`, Viewport `360 px` | Se muestra `$ 9.999.999.999` completo, con puntos de miles, sin truncarse, sin desbordar la tarjeta y sin mencionar periodicidad. | [ ] | [ ] |
| 2 | Límite inferior de capacidad (`1 pasajero`) | Abrir "Ver Detalles" de embarcación con capacidad máxima igual a `1` | `capacity = 1` | La tarjeta "Capacidad máxima" muestra exactamente `1 pasajero` en singular. | [ ] | [ ] |
| 3 | Precisión límite y signo de coordenadas GPS | Consultar embarcaciones con coordenadas enteras (`11`), con 3 decimales (`10.745`), más de 4 decimales (`10.74568`) y negativas (`-74.78912`) | Latitud: `11`, `10.745`, `10.74568`<br>Longitud: `-74.78912` | Se renderizan siempre con signo (menos antes del número si es Sur u Oeste) y exactamente 4 decimales con punto decimal: `11.0000`, `10.7450`, `10.7457`, `-74.7891`. | [ ] | [ ] |
| 4 | Ausencia total de servicios adicionales | Consultar embarcación sin servicios adicionales seleccionados | `additionalServices = []` | Se muestran los servicios base (**Capitán**, **Combustible**) y en el área de adicionales aparece `"Sin servicios adicionales seleccionados"`. | [ ] | [ ] |
| 5 | Tolerancia a dato obligatorio vacío o puerto nulo | Consultar embarcación con un campo obligatorio vacío o puerto nulo en base de datos | `port = null` o campo vacío | El backend no falla con HTTP 500. La pantalla muestra `"—"` en el campo vacío y renderiza el resto de tarjetas con normalidad. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Concurrencia, Seguridad Anti-Enumeración y Resiliencia
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Anti-enumeración: equivalencia entre embarcación ajena e inexistente | Como Propietario A, solicitar por API y UI una embarcación de Propietario B vs. un UUID inexistente | `GET /api/v1/fleet/boats/{idAjeno}` vs `GET /api/v1/fleet/boats/{uuidInexistente}` | Ambos devuelven exactamente `HTTP 404` y la misma pantalla con el mensaje plano `"No se pudo cargar la información de la embarcación"`, sin revelar si el ID existe ni mencionar permisos. | [ ] | [ ] |
| 2 | Identificador con formato no-UUID en URL | Navegar manualmente a `/owner/boats/no-es-uuid` y `/admin/boats/no-es-uuid` | ID: `no-es-uuid` (HTTP 400) | La pantalla trata el `400` igual que un `404`: muestra `"No se pudo cargar la información de la embarcación"` y únicamente el botón de regreso (sin botón `"Reintentar"`). | [ ] | [ ] |
| 3 | Cambio concurrente de estado en segundo plano mientras se visualiza la pantalla | Abrir "Ver Detalles" de una embarcación `Disponible` en Sesión 1; cambiar su estado a `Reservado` en Sesión 2; observar Sesión 1 y luego pulsar `"Editar"` | Cambio externo de `AVAILABLE` a `RESERVED` | La pantalla en Sesión 1 **no** se auto-actualiza sola (mantiene `Disponible` hasta recargar), pero al intentar ejecutar `"Editar"`, el servidor revalida el estado actual y bloquea la edición. | [ ] | [ ] |
| 4 | Actualización inmediata tras cambio de mantenimiento por el propio Propietario | Desde "Ver Detalles" en estado `Disponible`, ejecutar y confirmar `"Enviar a Mantenimiento"` | Acción propia de mantenimiento | A diferencia de cambios externos, tras confirmar la acción propia la pantalla se redibuja inmediatamente con los datos devueltos por el servidor (estado `Mantenimiento` y botón `"Terminar mantenimiento"`). | [ ] | [ ] |
| 5 | Persistencia de modales ante `Esc` o clic externo | Abrir modal de Mantenimiento o Eliminación desde "Ver Detalles" y presionar tecla `Esc` o hacer clic fuera del modal | Tecla `Escape` / Clic fuera del modal | El modal **no** se cierra; permanece abierto hasta que el usuario haga clic en uno de sus botones explícitos. | [ ] | [ ] |
| 6 | Accesibilidad en dispositivos táctiles y lectores de pantalla | Inspeccionar botones deshabilitados y alertas de error mediante atributos ARIA y contraste | Botón deshabilitado en `Reservado` / Pantalla de error | Los botones deshabilitados son anunciados como no disponibles por lectores de pantalla sin depender de hover ni tooltips; los errores usan `role="alert"` y los badges combinan icono circular sólido + texto con contraste mínimo AA. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : ¿Qué ocurre si el servidor backend no responde (`ERR_CONNECTION_REFUSED`) durante la carga inicial y, tras restablecerse la red, el token de sesión expiró al presionar `"Reintentar"`? -> Primero se muestra `"No se pudo cargar la información de la embarcación"` con `"Reintentar"` y `"Volver"`; al presionar `"Reintentar"`, el servidor responde `401` y el sistema redirige inmediatamente al inicio de sesión con `"Tu sesión ha expirado. Por favor, ingresa de nuevo."` en lugar de quedarse en el error de red.
* **EC-002** : ¿Qué ocurre si se inyecta una URL externa en el parámetro de destino de retorno al expirar la sesión? -> El sistema acepta únicamente direcciones internas de la propia aplicación como destino de retorno tras volver a iniciar sesión (FR-007).
* **EC-003** : ¿Qué ocurre cuando el Módulo 2 consulta `/api/v1/vessels/{id}/operational-status` para una embarcación en estado `Borrador` (`DRAFT`)? -> Se trata estrictamente como inexistente (`HTTP 404`), sin devolver estado ni metadatos.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | Caída del sistema, corrupción de datos o bloqueo total de un flujo crítico del usuario sin solución alternativa. |
| **Major** | **M** | Funcionalidad principal dañada o pérdida significativa de funcionalidad, pero existe una solución alternativa compleja. |
| **Small / Minor** | **S** | Problema cosmético, desalineación menor en la interfaz, error tipográfico o defecto no bloqueante. |