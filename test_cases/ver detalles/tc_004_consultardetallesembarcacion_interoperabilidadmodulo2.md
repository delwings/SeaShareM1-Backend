### Test Case Specification: Consultar Detalles de Embarcación - Interoperabilidad con Módulo 2 (Reservas)
**Created** : 2026-11-14  **Spec/Plan Link** : `docs/features/consultar-detalles/spec.md` / `docs/features/consultar-detalles/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-004-ConsultarDetallesEmbarcacion-InteroperabilidadModulo2
**Product Name & Version** : SeaShare - Módulo 1 Gestión de Flota (API Inter-Módulo) v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica los dos endpoints de solo lectura expuestos por el Módulo 1 para ser consumidos por el Módulo 2 (Operación de Reservas):
1. **Brindar Información de Estado Operativo** (`GET /api/v1/vessels/{vesselId}/operational-status`): devuelve únicamente el estado operativo en tiempo real utilizando el diccionario único de cuatro estados, sin caché ni campos adicionales.
2. **Proveer Información de Embarcación** (`GET /api/v1/vessels/{vesselId}`): devuelve la ficha oficial de la embarcación (incluyendo fotografía, estado operativo, puerto con GPS y zona horaria `America/Bogota`, servicios incluidos, `captain_included=true` y datos del propietario) excluyendo estrictamente la tarifa base.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Consultar Detalles de Embarcación (US 4 & US 5) | Documento de Especificación | `docs/features/consultar-detalles/spec.md` |
| Plan de implementación: Consultar detalles de embarcación (Fase 6) | Documento de Plan Técnico | `docs/features/consultar-detalles/plan.md` |
| Contrato REST: Consultar detalles | Documento de Contrato API | `docs/features/consultar-detalles/contract.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* Las decisiones de elegibilidad de reserva ("apto/no apto"), el bloqueo preventivo ante fallos (fail-safe), los reintentos y los mensajes al usuario final pertenecen al Módulo 2 y están fuera del alcance de esta prueba.
* Las consultas de catálogo en lote para el Módulo 2 están fuera del alcance de esta versión.

##### Dependencies & Assumptions
* Las rutas públicas internas de solo lectura `GET /api/v1/vessels/**` están configuradas sin autenticación entre módulos (`permitAll` para peticiones GET) según lo acordado en el contrato de integración.
* La base de datos cuenta con embarcaciones registradas en los estados `AVAILABLE`, `RESERVED`, `NAVIGATION`, `MAINTENANCE` y `DRAFT`.

##### Preconditions & Default Setup
* **Initial State** : Servidor backend ejecutándose en el puerto `8081`. Cliente API (Postman / cURL / Suite de integración) configurado sin token JWT de usuario.
* **Environment** : Entorno API Local / Staging (`http://localhost:8081/api/v1`).
* **Test Accounts** : No se requiere token de autenticación para los endpoints `GET /api/v1/vessels/**`.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Consulta de Estado Operativo (`Brindar Información de Estado Operativo`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar estado de embarcación `Disponible` | Enviar petición `GET` sin token de autenticación | `GET /api/v1/vessels/{availableVesselId}/operational-status` | Responde `HTTP 200 OK`. El cuerpo contiene **únicamente** el estado operativo (`AVAILABLE` según diccionario del Módulo 1), sin nombre, capacidad, tarifa ni otros atributos de la embarcación. | [ ] | [ ] |
| 2 | Consultar estado de embarcaciones no disponibles | Enviar petición `GET` para embarcaciones en los otros 3 estados válidos | `GET /api/v1/vessels/{id}/operational-status` para estados `RESERVED`, `NAVIGATION` y `MAINTENANCE` | Responde `HTTP 200 OK` devolviendo exactamente el estado actual correspondiente (`RESERVED`, `NAVIGATION` o `MAINTENANCE`) sin datos adicionales. | [ ] | [ ] |
| 3 | Verificar lectura en tiempo real (sin caché) | Cambiar el estado operativo de una embarcación en Módulo 1 (ej. a `MAINTENANCE`) y consultar inmediatamente el endpoint | `GET /api/v1/vessels/{vesselId}/operational-status` tras cambio de estado | La respuesta refleja de inmediato el nuevo estado (`MAINTENANCE`), confirmando que no se sirven valores cacheados. | [ ] | [ ] |
| 4 | Consultar estado de embarcación inexistente, eliminada o `Borrador` | Enviar petición `GET` con UUID inexistente, eliminado o en estado `DRAFT` | `GET /api/v1/vessels/{draftOrUnknownId}/operational-status` | Responde `HTTP 404 Not Found` sin devolver ningún estado ni dato de la embarcación. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Consulta de Información Oficial (`Proveer Información de Embarcación`)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Consultar ficha completa de embarcación registrada para Módulo 2 | Enviar petición `GET` sin autenticación al endpoint de información | `GET /api/v1/vessels/{validVesselId}` | Responde `HTTP 200 OK` incluyendo:<br>- Código de embarcación, nombre, matrícula legal, tipo y capacidad máxima<br>- Fotografía (`photo_url`) y estado operativo actual<br>- Puerto de salida con coordenadas GPS y zona horaria fija `America/Bogota`<br>- Servicios incluidos (base + adicionales seleccionados) y `captain_included: true`<br>- Código y nombre del propietario | [ ] | [ ] |
| 2 | Verificar exclusión estricta de la tarifa base | Inspeccionar todas las propiedades del JSON de respuesta del Paso 1 | Payload JSON de `GET /api/v1/vessels/{validVesselId}` | El campo de tarifa base **no existe** ni se expone en la respuesta (la cotización corresponde al Módulo 3). | [ ] | [ ] |
| 3 | Verificar lectura en tiempo real de datos modificados | Actualizar capacidad, servicios o fotografía en Módulo 1 y volver a consultar | `GET /api/v1/vessels/{validVesselId}` tras edición | Los datos devueltos corresponden al momento exacto de la consulta sin caché. | [ ] | [ ] |
| 4 | Consultar información de embarcación inexistente, eliminada o `Borrador` | Enviar petición `GET` con UUID inexistente, eliminado o en estado `DRAFT` | `GET /api/v1/vessels/{draftOrUnknownId}` | Responde `HTTP 404 Not Found` sin devolver datos parciales. | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : ¿Qué ocurre si el Módulo 2 consulta `GET /api/v1/vessels/{invalidFormatId}` o `/operational-status` con un identificador que no es UUID? -> El sistema rechaza el identificador inválido con `HTTP 400 Bad Request` sin exponer trazas de error internas ni inventar datos.
* **EC-002** : ¿Qué ocurre si una embarcación no tiene servicios adicionales seleccionados al ser consultada por el Módulo 2? -> El campo `captain_included` se mantiene en `true`, los servicios base están presentes y la colección de servicios adicionales se devuelve vacía sin producir un error `500`.
* **EC-003** : ¿Qué ocurre si un consumidor externo intenta ejecutar una petición `POST`, `PUT` o `DELETE` sobre `/api/v1/vessels/{vesselId}`? -> Únicamente se permiten consultas de solo lectura (`GET`); los métodos de escritura son rechazados (`HTTP 401`/`403` o `405`).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | Caída del sistema, corrupción de datos o bloqueo total de un flujo crítico del usuario sin solución alternativa. |
| **Major** | **M** | Funcionalidad principal dañada o pérdida significativa de funcionalidad, pero existe una solución alternativa compleja. |
| **Small / Minor** | **S** | Problema cosmético, desalineación menor en la interfaz, error tipográfico o defecto no bloqueante. |