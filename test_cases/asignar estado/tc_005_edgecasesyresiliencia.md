### Test Case Specification: Casos Límite (Edge Cases), Aislamiento Financiero y Resiliencia Operativa
**Created** : 2026-11-14  **Spec/Plan Link** : `asignar-estado-operativo-spec.md` / `asignar-estado-operativo-plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-005-EdgeCasesYResiliencia
**Product Name & Version** : SeaShare - Módulo 1 (Gestión de Flota y Activos P2P) v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica el comportamiento del sistema ante condiciones límite y escenarios excepcionales definidos en la sección *Edge Cases* de la especificación (`asignar-estado-operativo-spec.md`): (1) avisos dirigidos a embarcaciones inexistentes, eliminadas o no publicadas (`borrador`), (2) aislamiento estricto frente a datos financieros enviados por error (`FR-007`, `SC-003`), y (3) resiliencia y ordenamiento de mensajes en cola de espera ante indisponibilidad temporal del servicio (`FR-002`).

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Spec - Asignar Estado Operativo | Specification Document | `asignar-estado-operativo-spec.md` |
| Implementation Plan - Asignar Estado Operativo | Plan Document | `asignar-estado-operativo-plan.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Para el escenario de caída del servicio y retención en fila de espera (Sección 3), se requiere que el broker RabbitMQ (`seashare-rabbit`) esté desplegado e integrado en la fase de integración con M2 (`D1`). En la fase actual con simulador REST, la Sección 3 queda marcada como pendiente de integración.

##### Dependencies & Assumptions
* `TC-001-TransicionesAutomaticasEstado` aprobado.
* Migración `V10__operational_status_events.sql` ejecutada (`vessel`, `processed_event`, `vessel_status_history`).

##### Preconditions & Default Setup
* **Initial State** :
  * Base de datos sin registro para el identificador `VESSEL-INEXISTENTE-999`.
  * Embarcación `VESSEL-BORRADOR-01` creada en estado borrador (no publicada).
  * Embarcación `VESSEL-ELIMINADA-01` marcada con borrado lógico (`soft-deleted`).
  * Embarcación `VESSEL-FIN-01` publicada y en estado `AVAILABLE`.
* **Environment** : Entorno local / QA Backend (Spring Boot) con cliente HTTP (Postman), acceso a logs de aplicación, base de datos relacional y consola RabbitMQ (`http://localhost:15672` cuando aplique).
* **Test Accounts** : Ruta pública del endpoint simulador (`StatusEventController`) habilitada; credenciales RabbitMQ (`seashare` / `SeaShare2026`) para pruebas de integración AMQP.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Embarcación Inexistente, No Publicada o Eliminada (`Edge Case 1`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Aviso sobre embarcación inexistente | Enviar petición `POST` al endpoint simulador con un `vesselId` que no existe en la base de datos. | `eventId: "EVT-EC-01"`, `vesselId: "VESSEL-INEXISTENTE-999"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:00:00Z"` | Respuesta HTTP `200 OK` (confirma recepción del mensaje para evitar reintentos infinitos). El sistema ignora el cambio y guarda un reporte/registro del error en el log técnico para revisión. | [ ] | [ ] |
| 2 | Aviso sobre embarcación en borrador (no publicada) | Enviar petición `POST` al endpoint simulador apuntando a `VESSEL-BORRADOR-01`. | `eventId: "EVT-EC-02"`, `vesselId: "VESSEL-BORRADOR-01"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:05:00Z"` | Respuesta HTTP `200 OK` confirmando recepción. El sistema ignora el cambio, registra el incidente en el log técnico y mantiene `VESSEL-BORRADOR-01` sin alterar. | [ ] | [ ] |
| 3 | Aviso sobre embarcación eliminada | Enviar petición `POST` al endpoint simulador apuntando a `VESSEL-ELIMINADA-01`. | `eventId: "EVT-EC-03"`, `vesselId: "VESSEL-ELIMINADA-01"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T10:10:00Z"` | Respuesta HTTP `200 OK` confirmando recepción. El cambio es ignorado por la condición de embarcación sin eliminar (`D6`), se registra en el log y no se crea historial. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Aislamiento de Información Financiera (`Edge Case 2`, `FR-007`, `SC-003`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Enviar aviso con datos de cobros, tarifas y multas | Enviar petición `POST` válida para reservar `VESSEL-FIN-01`, adjuntando en el JSON campos financieros no solicitados. | `eventId: "EVT-EC-04"`, `vesselId: "VESSEL-FIN-01"`, `status: "RESERVED"`, `occurredAt: "2026-11-14T11:00:00Z"`, `price: 450.00`, `totalPaid: 500.00`, `fineAmount: 50.00`, `currency: "USD"` | Respuesta HTTP `200 OK`. El sistema ignora los datos financieros (`FR-007`) y aplica únicamente el cambio de estado de `VESSEL-FIN-01` a `RESERVED`. | [ ] | [ ] |
| 2 | Auditar persistencia en base de datos de Gestión de Flota | Inspeccionar las tablas `vessel`, `processed_event`, `vessel_status_history` y logs de aplicación tras procesar `EVT-EC-04`. | `vesselId: "VESSEL-FIN-01"`, `eventId: "EVT-EC-04"` | Cero datos financieros (precios, tarifas, cobros o multas) almacenados en la base de datos de Gestión de Flota (`SC-003`). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Indisponibilidad del Servicio y Cola de Espera (`Edge Case 3`, `FR-002`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Detener servicio M1 y encolar secuencia de avisos en M2 | Con el backend de Módulo 1 apagado, publicar 3 mensajes consecutivos en orden cronológico en la cola de RabbitMQ (`seashare-rabbit`) para `VESSEL-FIN-01`. | Msg 1: `RESERVED` (`12:00Z`), Msg 2: `IN_NAVIGATION` (`13:00Z`), Msg 3: `MAINTENANCE_CLEANING` (`15:00Z`) | Los 3 mensajes permanecen retenidos en la fila de espera de RabbitMQ sin perderse mientras M1 está apagado. | [ ] | [ ] |
| 2 | Reiniciar servicio M1 y verificar consumo ordenado | Encender nuevamente el servicio backend de Módulo 1 y monitorear el consumo de la cola y el estado final de `VESSEL-FIN-01`. | `vesselId: "VESSEL-FIN-01"` | Al encenderse, M1 lee y procesa los mensajes pendientes en orden (`FR-002`). `VESSEL-FIN-01` queda en estado final `MAINTENANCE` y `vessel_status_history` registra las 3 transiciones secuenciales. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Reenvío de aviso sobre embarcación inexistente)** : Reenviar exactamente el mismo `eventId` (`"EVT-EC-01"`) para `"VESSEL-INEXISTENTE-999"`; verificar que el sistema responda `200 OK` de manera idempotente sin generar errores no controlados.
* **EC-002 (Concurrencia exacta entre evento automático y acción manual - `D6`)** : Ejecutar simultáneamente en el mismo instante una acción manual de "Enviar a Mantenimiento" y un aviso automático `AVAILABLE` → `RESERVED`; verificar que la sentencia condicional atómica garantice que solo uno prevalezca sin corrupción de estado (`SC-002`).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |