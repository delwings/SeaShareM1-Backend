### Test Case Specification: Concurrencia, Errores Técnicos, Doble Envío, Sesión y Modales
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/eliminar-embarcacion/spec.md` | `docs/features/eliminar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-005-ConcurrenciaErroresModales
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la resiliencia y el control de concurrencia del flujo de eliminación de embarcaciones: cambio concurrente de estado a `Reservado` o `En Navegación` (`409` con `current_status`), embarcación ya eliminada o ajena (`404` anti-enumeración), fallos de red/servidor/timeout de 15 s con reintento o cierre, prevención de doble clic y repetición con `Enter`, expiración de sesión (`401`), orden de verificación en servidor y bloqueo de cierre externo de todos los modales del flujo (US5, FR-022 a FR-029, NFR-001 a NFR-003, SC-006 a SC-010).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Eliminar Embarcación | Specification Document | `docs/features/eliminar-embarcacion/spec.md` |
| Implementation Plan: Eliminar Embarcación | Plan Document | `docs/features/eliminar-embarcacion/plan.md` |
| Contrato REST: Eliminar Embarcación | API Contract | `docs/architecture/contracts/rest/eliminar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* En la eliminación no se muestran spinners en los botones; al enviarse, el botón queda deshabilitado con ícono de prohibido al pasar el mouse y sin tooltips (FR-008, Plan).
* Guardar el destino de retorno al vencer la sesión (FR-027) está marcado como pendiente en `T105` del plan; actualmente `401` cierra los modales y redirige al inicio de sesión (Decisión D13).

##### Dependencies & Assumptions
* Backend (`SeaShareM1`), MySQL y Frontend (`SeaShareM1-Frontend`) operativos.
* DevTools disponible para simular modo *Offline*, respuestas `401`/`404`/`409`/`500`, latencia `> 15 s` y ejecución simultánea desde dos sesiones o pestañas.

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado (`OWNER` o `ADMIN`) con acceso a "Ver Detalles" de embarcaciones en estado `AVAILABLE`.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`), `propietario.otro@seashare.com` (rol `OWNER`) y `admin@seashare.com` (rol `ADMIN`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Cambio concurrente de estado, embarcación ya eliminada o ajena (Escenarios US5-1 a US5-3)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular cambio de estado con modal abierto | Abrir modal "¿Eliminar embarcación?", cambiar la embarcación a `RESERVED` (o `NAVIGATION`) en otra sesión/BD y pulsar **"Sí, eliminar"** | Estado concurrente `RESERVED` / `NAVIGATION` | El servidor no elimina (`409` con `current_status`) y muestra el modal **"No es posible eliminar la embarcación"** con **"La embarcación pasó a estado [Estado] mientras tanto. No es posible eliminarla."** y botón **"Recargar página"** (FR-023, SC-006, Decisión D3/D7/D12). | [ ] | [ ] |
| 2 | Simular embarcación ya eliminada por otra sesión | Abrir modal en Pestaña A, eliminar la embarcación desde Pestaña B y pulsar **"Sí, eliminar"** en Pestaña A | `boatId` ya eliminado (`404`) | No vuelve a eliminar, no crea otra notificación y muestra el modal **"Esta embarcación ya no existe"** con botón **"Aceptar"** que lleva a la lista del rol del usuario (FR-024, FR-029, SC-006, Decisión D3/D6). | [ ] | [ ] |
| 3 | Simular intento de eliminar embarcación ajena | Como `propietario.test@seashare.com`, invocar eliminación sobre un `boatId` perteneciente a `propietario.otro@seashare.com` | `boatId` ajeno (`404`) | El servidor responde `404` anti-enumeración sin eliminar nada y se muestra **"Esta embarcación ya no existe"** sin revelar que la embarcación existe (FR-022, FR-025, SC-006, Decisión D5/D6). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Errores técnicos, doble envío, sesión y cierre de modales (Escenarios US5-4 a US5-8)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular fallo de red, error 5xx o timeout de 15 s | Activar modo *Offline*, forzar `500` o demora `> 15 s` al pulsar **"Sí, eliminar"** | Fallo de red / `500` / timeout `15 s` | Se cancela la petición a los `15 s` (`AbortController`) y se muestra **"No se pudo eliminar la embarcación"** con **"Ocurrió un problema técnico al eliminar la embarcación. Verifique su conexión a internet e inténtelo nuevamente."** y botones **"Cerrar"** y **"Reintentar"**; la embarcación queda intacta (FR-026, NFR-003, SC-008, Decisión D10). | [ ] | [ ] |
| 2 | Probar "Reintentar" y "Cerrar" tras error técnico | Pulsar **"Reintentar"** (con fallo y luego con red restaurada) y en otro intento pulsar **"Cerrar"** | Red caída -> red activa; clic en "Cerrar" | **"Reintentar"** repite la eliminación (mostrando el modal tantas veces como falle y completando si hay red); **"Cerrar"** deja al usuario en "Ver Detalles" (FR-026). | [ ] | [ ] |
| 3 | Verificar protección ante doble clic y tecla Enter | Pulsar **"Sí, eliminar"**, realizar múltiples clics rápidos y presionar `Enter` durante el envío | Clics rápidos + `Enter` | El botón queda deshabilitado de inmediato con ícono de prohibido al pasar el mouse y sin tooltips; `Enter` no repite la solicitud; se envía una sola petición y no se duplican registros ni notificaciones (FR-008, NFR-002, SC-007, Decisión D11). | [ ] | [ ] |
| 4 | Simular sesión expirada y verificar cierre estricto de modales | Forzar `401` al pulsar "Eliminar"/"Sí, eliminar"; intentar cerrar cualquier modal con `Esc` o clic fuera | `401 Unauthorized`; `Esc` / clic en backdrop | `401`: cierra modales y redirige al inicio de sesión sin eliminar (FR-027, Decisión D13). Modales: ni `Esc`, ni clic fuera, ni botón "X" cierran ningún modal; solo responden a sus botones explícitos (FR-028, SC-009). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Carrera simultánea Propietario vs. Administrador)** : Si un Propietario y un Administrador intentan eliminar la misma embarcación casi al mismo tiempo, solo se ejecuta una eliminación; el segundo recibe **"Esta embarcación ya no existe"** y la notificación `VESSEL_DELETED` se genera una sola vez (Edge Cases, FR-021, FR-029).
* **EC-002 (Orden de verificación en servidor)** : El backend evalúa en orden: (1) sesión activa, (2) existencia y no eliminada, (3) permiso de rol/propiedad, (4) estado operativo (`AVAILABLE` / `MAINTENANCE`), (5) ejecución del `UPDATE` atómico `WHERE id = ? AND deleted = FALSE AND status IN ('AVAILABLE','MAINTENANCE')` (FR-022, Decisión D2/D5).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |