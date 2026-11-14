### Test Case Specification: Notificación al Propietario por Eliminación Administrativa
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/eliminar-embarcacion/spec.md` | `docs/features/eliminar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-004-NotificacionEliminacionAdmin
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la generación y el contenido de la notificación `VESSEL_DELETED` cuando un Administrador elimina la embarcación de un Propietario, incluyendo el texto exacto con nombre, matrícula, fecha en hora de Colombia (`America/Bogota`, mes en minúscula) y motivo, la persistencia para cuando el propietario inicie sesión después, la diferenciación entre embarcaciones del mismo nombre pero distinta matrícula, el aislamiento de visibilidad hacia el propietario dueño y la resiliencia ante fallos en la creación del aviso (US4, FR-017 a FR-021, Historial 11/11/2026).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Eliminar Embarcación | Specification Document | `docs/features/eliminar-embarcacion/spec.md` |
| Implementation Plan: Eliminar Embarcación | Plan Document | `docs/features/eliminar-embarcacion/plan.md` |
| Contrato REST: Eliminar Embarcación | API Contract | `docs/architecture/contracts/rest/eliminar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La interfaz visual del centro de notificaciones (campana, lista, indicador de no leídas y marcado como leída) se define en `notificaciones-spec.md`; aquí se verifica la creación, persistencia y contenido de la notificación `VESSEL_DELETED` (Historial 11/11/2026, [S-11], [S-20]).

##### Dependencies & Assumptions
* Migración V8 aplicada (`VESSEL_DELETED` en `notification.type`), `NotificationRepositoryPort` y adaptador JPA operativos.
* En la base de datos existe al menos un Propietario con embarcaciones registradas (incluyendo el caso de dos embarcaciones con el mismo nombre y distinta matrícula).

##### Preconditions & Default Setup
* **Initial State** : Administrador autenticado y Propietario dueño de las embarcaciones objetivo registrados en el sistema.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `admin@seashare.com` (rol `ADMIN`), `propietario.test@seashare.com` (rol `OWNER`) y `propietario.otro@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Generación, formato y aislamiento de la notificación (Escenarios US4-1, US4-2 y US4-6)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Eliminar embarcación como Administrador y verificar registro de notificación | Como `admin@seashare.com`, eliminar embarcación de `propietario.test@seashare.com` con motivo válido | Nombre: `"Brisa del Mar"`, Matrícula: `"CP-05-1042-A"`, Motivo: `"Incumplimiento normativo"` | Se crea una fila en `notification` con tipo `VESSEL_DELETED` para `propietario.test@seashare.com` con el texto `"Tu embarcación Brisa del Mar (CP-05-1042-A) fue eliminada por un administrador el [fecha en hora Colombia, ej. 14 de noviembre de 2026]. Motivo: Incumplimiento normativo"` (FR-017, FR-021, Historial 11/11/2026, Decisión D14/D17). | [ ] | [ ] |
| 2 | Verificar disponibilidad para propietario desconectado | Cerrar sesión del propietario antes de la eliminación, eliminar como admin y luego iniciar sesión como `propietario.test@seashare.com` | `propietario.test@seashare.com` | La notificación `VESSEL_DELETED` está persistida y disponible para el propietario al iniciar sesión (FR-019, [S-11]). | [ ] | [ ] |
| 3 | Verificar visibilidad exclusiva del propietario dueño | Consultar notificaciones / acceso de `propietario.otro@seashare.com` y verificar vista de `admin@seashare.com` | `propietario.otro@seashare.com`, `admin@seashare.com` | Solo `propietario.test@seashare.com` puede ver la notificación; `propietario.otro@seashare.com` no la ve y `admin@seashare.com` solo ve su modal de éxito (FR-021, [S-19]). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Casos límite de notificación y resiliencia (Escenarios US4-3 a US4-5 y Edge Cases)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Eliminar una de dos embarcaciones con el mismo nombre | Tener dos embarcaciones del mismo propietario llamadas `"Perla del Caribe"` con matrículas `"CP-05-1001-A"` y `"CP-05-1002-B"`, y eliminar una | `"Perla del Caribe"` (`"CP-05-1001-A"`) | El texto de la notificación incluye la matrícula `"CP-05-1001-A"`, permitiendo distinguir cuál embarcación fue eliminada (FR-017, [S-06]). | [ ] | [ ] |
| 2 | Verificar que eliminación por Propietario no crea notificación | Iniciar sesión como `propietario.test@seashare.com` y eliminar una embarcación propia | Embarcación propia en `AVAILABLE` | No se crea ninguna notificación `VESSEL_DELETED` (FR-018, SC-005). | [ ] | [ ] |
| 3 | Simular fallo al persistir la notificación | Forzar error al guardar en `notification` durante una eliminación administrativa | Error en repositorio de notificaciones | La embarcación queda eliminada lógicamente, el Administrador ve el modal "¡Embarcación eliminada!" y el error se registra en log sin revertir la eliminación (FR-020, Decisión D16, [S-08]). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Nombres o matrículas largos a 360 px)** : Si el nombre o la matrícula de la embarcación son extensos, el texto de la notificación se muestra completo sin cortarse ni desbordar horizontalmente en un viewport de `360 px` (Edge Cases, NFR-005).
* **EC-002 (Hora en zona `America/Bogota` con mes en minúscula)** : Aunque el timestamp en base de datos se almacena en UTC, el texto generado al propietario formateará la fecha en hora de Colombia (`America/Bogota`) con el mes en minúscula, por ejemplo `"14 de noviembre de 2026"` (FR-017, Decisión D17).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |