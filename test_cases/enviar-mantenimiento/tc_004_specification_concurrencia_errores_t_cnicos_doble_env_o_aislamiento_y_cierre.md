### Test Case Specification: Concurrencia, Errores Técnicos, Doble Envío, Aislamiento y Cierre
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/enviar-mantenimiento/spec.md` | `docs/features/enviar-mantenimiento/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-004-ConcurrenciaErroresYCIerre
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica los escenarios de concurrencia y errores técnicos en "Enviar a Mantenimiento": cambio de estado a `"Reservado"` o `"En Navegación"` durante la confirmación (`409` con `current_status` y `maintenance_by`), fallos de red/servidor o timeout de `15 s` (`"Cerrar"` y `"Reintentar"`), bloqueo de doble clic/envío repetido, aislamiento por propiedad/rol (`404` anti-enumeración hacia "Mis Embarcaciones"), y la verificación de cierre del plan (`T138` a `T140`) (US1-4, US1-5, Edge Cases, FR-008, FR-009, FR-012, FR-016, FR-022).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Enviar a Mantenimiento | Specification Document | `docs/features/enviar-mantenimiento/spec.md` |
| Implementation Plan: Enviar a Mantenimiento | Plan Document | `docs/features/enviar-mantenimiento/plan.md` |
| Contrato REST: Enviar a Mantenimiento | API Contract | `docs/architecture/contracts/rest/enviar-mantenimiento.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La acción `"Reportar nuevamente"` del Administrador (`Historia 5, escenario 2`) está pendiente y no forma parte de este caso.
* La verificación automatizada de build (`mvn spotless:apply`, `mvn clean verify`) y la colección Postman (`T138`, `T140`) se ejecutan en la fase de cierre del plan.

##### Dependencies & Assumptions
* DevTools disponible para simular modo *Offline*, error `500`, retardo `> 15 s` (timeout) o bloqueo de petición, más dos pestañas o sesiones para provocar cambios concurrentes.
* Embarcaciones de prueba para `propietario.test@seashare.com`, otro propietario (`otro.propietario@seashare.com`) y un `boatId` inexistente o eliminado.

##### Preconditions & Default Setup
* **Initial State** : Usuario autenticado (`OWNER` o `ADMIN`) con acceso a "Ver Detalles".
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`), `otro.propietario@seashare.com` (rol `OWNER`) y `admin@seashare.com` (`1010000001`, rol `ADMIN`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Cambio Concurrente de Estado (409) y Errores Técnicos / Timeout (Escenarios US1-4, US1-5)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Simular cambio a `Reservado` o `En Navegación` mientras se confirma | Abrir "Ver Detalles" en `Disponible`; en otra sesión/flujo pasar la embarcación a `Reservado` o `En Navegación`; confirmar **"Sí, enviar"** | Transición concurrente a `Reservado` / `En Navegación` | El servidor responde `409` con `current_status` y `maintenance_by`; no cambia el estado y muestra el modal **"No es posible enviar la embarcación a mantenimiento"** con el estado actual y el botón **"Recargar página"** (US1-4, FR-008, FR-009, Decisiones D5, D6). | [ ] | [ ] |
| 2 | Simular falla de red, servidor (5xx) o timeout de 15 s | Activar modo *Offline*, forzar `500` o demorar respuesta `> 15 s` al confirmar **"Sí, enviar"**; pulsar **"Cerrar"** y luego reintentar con red normal | Modo *Offline* / `500` / `> 15 s` | Muestra **"No se pudo enviar a mantenimiento"** con botones **"Cerrar"** y **"Reintentar"**; la embarcación no cambia de estado; **"Cerrar"** cierra el modal manteniendo estado y **"Reintentar"** repite la acción con éxito (US1-5, Plan `T134`, `T135`). | [ ] | [ ] |
| 3 | Verificar bloqueo ante doble clic o envío repetido | Hacer doble clic rápido o pulsar `Enter` repetidamente sobre **"Sí, enviar"** | Doble clic en confirmación | Solo sale una solicitud por confirmación y no se generan estados inconsistentes ni avisos duplicados (Edge Cases, FR-016, FR-022). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Aislamiento de Propiedad/Rol (404) y Verificación de Cierre del Plan (Edge Cases, T138–T140)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Intentar operar embarcación eliminada o de otro propietario | Abrir o invocar endpoint de mantenimiento con `boatId` eliminado o de `otro.propietario@seashare.com` | `boatId` ajeno o eliminado | El sistema responde `404` sin revelar si el recurso existe y la pantalla lleva a **"Mis Embarcaciones"** (Edge Cases, FR-008, FR-012). | [ ] | [ ] |
| 2 | Ejecutar verificación de build, pruebas manuales y colección Postman | Ejecutar `mvn spotless:apply` y `mvn clean verify` en backend, pruebas manuales de `OWNER`/`ADMIN` y colección Postman | `T138`, `T139`, `T139b`, `T140` | Compilación, Spotless, pruebas unitarias y de integración (`BoatRegistrationApiIT`, `DatabaseMigrationIT` con 9 migraciones) y escenarios de Postman pasan en verde (Plan Fase 5). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Reintento tras pérdida de respuesta con `200` aplicado)** : Si el servidor aplicó la transición pero la respuesta se perdió y el cliente reintenta, el backend devuelve `200` con la embarcación completa sin modificar `maintenance_by` ni duplicar `MAINTENANCE_NOTICE` / `VESSEL_AVAILABLE` (FR-016, Decisiones D4, D15).
* **EC-002 (Permiso de edición y eliminación durante Mantenimiento)** : Editar y eliminar embarcación siguen permitidos cuando la embarcación está en estado `"Mantenimiento"` (Plan Notas).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |