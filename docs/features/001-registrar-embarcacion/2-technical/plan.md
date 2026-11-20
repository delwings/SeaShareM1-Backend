# Plan de implementación: Registrar Embarcación

**Fecha**: 5 de noviembre de 2026
**Especificación**: `docs/features/registrar-embarcacion/spec.md` (Registrar.md, última actualización 4 de noviembre de 2026)
**Contrato REST**: `docs/architecture/contracts/rest/registrar-embarcacion.md`
**Estado**: implementada y probada (backend, frontend y pruebas manuales con Cloudinary).

> Lo común a todo el módulo (stack, seguridad, errores, despliegue, modelo físico) está en el plan general. Este plan cubre solo esta funcionalidad. La spec es la fuente de verdad del comportamiento; el wireframe guía únicamente el estilo visual.

## Resumen

El propietario consulta su flota, registra una embarcación en dos pasos (datos básicos y datos complementarios) y puede retomar o descartar un borrador. El paso 1 guarda un borrador (`vessel` con estado `DRAFT`, uno por propietario); el paso 2 elige el puerto en un mapa, escribe su nombre, indica la tarifa, los servicios y la foto, y deja la embarcación en `AVAILABLE`.

Enfoque técnico: reglas de validación en Java puro dentro de `domain`, casos de uso en `application` sin Spring, adaptadores de persistencia, web y Cloudinary en `infrastructure`; frontend de HTML, CSS y JavaScript puro con las mismas reglas para avisar antes de enviar. Se agrega la columna `vessel.registered_at` (migración V6, excepción autorizada al modelo físico) para ordenar la lista y calcular la etiqueta NUEVA.

### Decisiones de diseño

| # | Decisión | Motivo |
|---|---|---|
| D1 | Un solo borrador por propietario, guardado como fila de `vessel` con `status = DRAFT` | Respeta el modelo físico, sin tabla nueva |
| D2 | «Empezar nuevo» elimina el borrador de forma física | La matrícula queda libre al instante |
| D3 | `registered_at` se llena al pasar a `AVAILABLE` por primera vez | Ordena la lista y activa NUEVA (168 horas) |
| D4 | El puerto se busca por nombre (sin distinguir mayúsculas ni tildes); si existe, se reutiliza con sus coordenadas originales | Varias embarcaciones pueden atracar en el mismo puerto |
| D5 | El paso 2 se identifica por `boatId` en la URL | Hace el reintento seguro (idempotente) |
| D6 | La foto se valida por sus bytes (firma PNG o JPEG), no por extensión ni `Content-Type` | Evita archivos disfrazados |
| D7 | Capitán y Combustible no se guardan | Siempre incluidos; solo se guardan los 5 servicios adicionales |
| D8 | Latitud y longitud con signo y 4 decimales (mitad hacia arriba) | Coincide con el spec |
| D9 | La foto se sube a Cloudinary solo si todo lo demás es válido | No dejar imágenes huérfanas |

## Contexto técnico

**Lenguaje/Versión**: Java 17 (backend); HTML, CSS y JavaScript puro con módulos ES (frontend).
**Dependencias principales**: Spring Boot 4.1.1 (Web, Security con JWT, Data JPA), MapStruct 1.6.3, Flyway, API REST de Cloudinary con `RestClient`; Leaflet 1.9.4 (desde cdnjs) con teselas de OpenStreetMap para el mapa.
**Almacenamiento**: MySQL 8 (tablas `vessel` y `port`, migración V6); imágenes en Cloudinary (solo la URL en `photo_path`).
**Pruebas**: JUnit 5, Mockito, Spring Boot Test y Testcontainers (MySQL 8), ArchUnit y JaCoCo (80 % en `domain` y `application`); Vitest con jsdom en el frontend.
**Plataforma objetivo**: backend en Render y frontend en Vercel; en desarrollo, backend en el puerto 8081 y frontend con Live Server (puerto 5500).
**Tipo de proyecto**: web, con dos repositorios (`SeaShareM1` y `SeaShareM1-Frontend`).
**Metas de rendimiento**: consultas en menos de 1 segundo en condiciones normales (la lista y la disponibilidad de matrícula).
**Restricciones**: modelo físico inmutable salvo `registered_at`; foto PNG o JPG de máximo 10 MB; tarifa de hasta 9.999.999.999 COP (`DECIMAL(12,2)`); el spec es la fuente de verdad.
**Escala/Alcance**: 3 historias de usuario, 7 endpoints y 3 pantallas con sus modales; entorno académico.

## Estructura del proyecto

### Documentación (esta funcionalidad)

```text
docs/
├── architecture/contracts/rest/
│   └── registrar-embarcacion.md    # contrato de los 7 endpoints
├── diagrams/
│   └── modelo-fisico-modulo1.svg   # incluye vessel.registered_at
└── features/registrar-embarcacion/
    ├── plan.md                     # este archivo
    └── spec.md                     # especificación (Registrar.md)
```

### Código fuente (raíz de los repositorios)

```text
SeaShareM1/  (backend)
├── src/main/resources/db/migration/
│   └── V6__add_vessel_registered_at.sql
├── src/main/java/com/seashare/seasharem1/
│   ├── domain/
│   │   ├── model/            # Vessel (newDraft, complete), AdditionalServices
│   │   ├── rules/            # RegistrationRules, Parsed
│   │   └── exception/        # FieldViolation, ValidationFailedException, BusinessConflictException
│   ├── application/
│   │   ├── port/in/          # CreateDraft, CompleteRegistration, GetDraft, DeleteDraft,
│   │   │                     # ListOwnerBoats, CheckLegalRegistration (+ comandos y resultados)
│   │   ├── port/out/         # VesselRepositoryPort, PortRepositoryPort
│   │   └── usecase/          # los 6 servicios
│   ├── infrastructure/adapter/
│   │   ├── in/web/           # BoatController, BoatTypeController, BoatWebMapper, dto/*
│   │   └── out/persistence/  # VesselPersistenceAdapter, PortPersistenceAdapter,
│   │                         # VesselJpaRepository, PortJpaRepository, VesselEntity
│   └── configuration/        # UseCaseConfig, SecurityConfig (@EnableMethodSecurity)
└── src/test/java/com/seashare/seasharem1/
    ├── unit/{rules,domain,application,web}/
    └── integration/          # BoatRegistrationApiIT, DatabaseMigrationIT

SeaShareM1-Frontend/
├── index.html
└── src/
    ├── pages/                # ownerHome, registerBoatBasic, registerBoatDetails (+ .test.js)
    ├── components/           # mapPicker, registrationLayout, formField, header
    ├── services/             # fleetService
    ├── validation/           # registrationRules
    ├── state/                # registrationFlow
    ├── ui/                   # modal, dom
    ├── routes/               # routes, router
    └── styles/main.css
```

**Decisión de estructura**: se mantiene la estructura web de dos repositorios del plan general. En el backend, el vocabulario de la API usa `boat_*` y el interno usa `vessel`; la conversión vive en `BoatWebMapper`. En el frontend, la lógica de reglas y el estado están separados de las páginas que tocan el DOM, y todo acceso al backend pasa por `services/fleetService.js`.

---

## Fase 1: Setup (infraestructura compartida)

Completada en el plan general (T001 a T009). Esta funcionalidad no agrega proyectos ni herramientas nuevas; solo la dependencia de Leaflet, cargada desde cdnjs en tiempo de ejecución.

---

## Fase 2: Foundational (prerrequisitos bloqueantes)

Completada en el plan general (T010 a T022): migraciones V1 a V5, modelos y entidades, autenticación JWT por rol, manejo de errores RFC 9457, CORS, logs, almacenamiento de imágenes en Cloudinary, base de pruebas, reglas de arquitectura y base del frontend.

**Tareas propias de esta funcionalidad que cierran la base**:

- [x] T023 Backend: crear la migración V6 con `vessel.registered_at` (`TIMESTAMP NULL`) y agregar el campo a la entidad, al modelo `Vessel` y a su mapper.
- [x] T024 Backend: crear en `domain` las reglas de validación (`RegistrationRules`), `AdditionalServices`, `FieldViolation` y `ValidationFailedException`, y extender `BusinessConflictException` con lista de violaciones.
- [x] T025 Backend: ampliar `GlobalExceptionHandler` (400 con `errors`, 409 con `errors`, foto mayor a 10 MB como 400), habilitar `@EnableMethodSecurity` y fijar `max-request-size = -1`.

**Punto de control**: la base de la funcionalidad está lista; las historias pueden comenzar.

---

## Fase 3: Historia 1 - Registrar los datos básicos, paso 1 de 2 (Prioridad: P1)

**Objetivo**: ver la lista «Mis Embarcaciones Registradas», abrir el formulario y guardar nombre, matrícula legal, tipo y capacidad como borrador.

**Prueba independiente**: con un propietario, `POST /fleet/boats/draft` crea el borrador, `GET /fleet/boats/legal-registration-availability` lo reconoce como ocupado y, en el navegador, el paso 1 avanza al paso 2.

### Pruebas de la Historia 1

- [x] T026 [P] [US1] Pruebas unitarias de las reglas y del modelo: `RegistrationRulesTest` y `VesselRegistrationTest`.
- [x] T027 [P] [US1] Pruebas unitarias de los casos de uso con repositorios falsos: `CreateDraftServiceTest` y `OwnerBoatsServicesTest`.
- [x] T028 [P] [US1] Prueba de integración `BoatRegistrationApiIT`: flujo completo con etiqueta NUEVA, matrícula repetida entre propietarios (409), segundo borrador (409), disponibilidad, 403 del administrador y 400 con campos inválidos.
- [x] T029 [P] [US1] Pruebas del frontend: `registrationRules.test.js`, `fleetService.test.js`, `ownerHome.test.js` y `registerBoatBasic.test.js`.

### Implementación de la Historia 1

- [x] T030 [P] [US1] Definir los puertos de entrada y salida y los servicios `CreateDraftService`, `GetDraftService`, `CheckLegalRegistrationService` y `ListOwnerBoatsService` (orden por `registered_at` y etiqueta NUEVA solo en la primera, por menos de 168 horas).
- [x] T031 [P] [US1] Implementar `VesselPersistenceAdapter` con sus repositorios JPA; una violación del índice único de matrícula se traduce a un 409 con `errors`.
- [x] T032 [US1] Implementar `BoatController`, `BoatTypeController`, los DTOs y `BoatWebMapper`: `GET /fleet/boats`, `GET /fleet/boats/legal-registration-availability`, `GET /fleet/boats/draft`, `POST /fleet/boats/draft` y `GET /fleet/boat-types` (solo rol `OWNER`).
- [x] T033 [US1] Registrar los casos de uso en `UseCaseConfig`.
- [x] T034 [US1] Frontend: lista de embarcaciones con su estado vacío y la etiqueta NUEVA (`ownerHome.js`).
- [x] T035 [US1] Frontend: paso 1 con validación en tiempo real, verificación de la matrícula (espera 350 ms), selector de tipos con aviso de error y guardado del borrador (`registerBoatBasic.js`, `registrationRules.js`, `fleetService.js`, `registrationFlow.js`).
- [x] T036 [US1] Corregir la carrera del temporizador de verificación: `submit()` lo cancela al empezar para que no cierre el modal de error.

**Punto de control**: la Historia 1 es funcional y verificable por sí sola.

---

## Fase 4: Historia 2 - Completar los datos complementarios y guardar, paso 2 de 2 (Prioridad: P1)

**Objetivo**: elegir el puerto en el mapa, escribir su nombre, indicar tarifa, servicios y foto, y dejar la embarcación `AVAILABLE`.

**Prueba independiente**: con un borrador existente, `POST /fleet/boats/{boatId}/registration` (multipart) devuelve 200 con la embarcación `AVAILABLE`, y aparece en la lista como NUEVA.

### Pruebas de la Historia 2

- [x] T037 [P] [US2] Pruebas unitarias de `CompleteRegistrationService`: puerto reutilizado, reintento idempotente, errores combinados y foto no subida si hay errores.
- [x] T038 [P] [US2] Ampliar `BoatRegistrationApiIT`: paso 2 completo con Cloudinary simulado, foto de formato inválido (400) y dos embarcaciones con el mismo puerto.
- [x] T039 [P] [US2] Pruebas del frontend: `registerBoatDetails.test.js` y `mapPicker.test.js`.

### Implementación de la Historia 2

- [x] T040 [P] [US2] Implementar `CompleteRegistrationService` (validación de puerto, coordenadas, tarifa y foto; búsqueda o creación del puerto; subida a Cloudinary en `seashare/boats`; `registered_at` = ahora) y `PortPersistenceAdapter` (ante una carrera devuelve el puerto existente).
- [x] T041 [US2] Agregar `POST /fleet/boats/{boatId}/registration` (multipart) a `BoatController`.
- [x] T042 [US2] Frontend: componente del mapa (Leaflet, vista inicial en Santa Marta con zoom 12, clic o arrastre del marcador, mensaje con «Reintentar» si no carga).
- [x] T043 [US2] Frontend: paso 2 con puerto escrito a mano, Latitud y Longitud de solo lectura, tarifa, foto, servicios y modales de éxito y de error técnico (`registerBoatDetails.js`).

**Punto de control**: las Historias 1 y 2 funcionan juntas de punta a punta, con la foto en Cloudinary.

---

## Fase 5: Historia 3 - Retomar o descartar un borrador (Prioridad: P2)

**Objetivo**: al pulsar «Registrar Nueva Embarcación», si hay un borrador, ofrecer continuarlo o empezar uno nuevo eliminándolo de forma permanente.

**Prueba independiente**: con un borrador, el botón abre el modal «Borrador detectado»; «Continuar registro pendiente» lleva al paso 2 y «Empezar un registro nuevo» pide confirmación y borra el borrador.

### Pruebas de la Historia 3

- [x] T044 [P] [US3] Ampliar `BoatRegistrationApiIT` con el borrado del borrador y el 404 posterior; las pruebas de `ownerHome.test.js` cubren los modales.

### Implementación de la Historia 3

- [x] T045 [P] [US3] Implementar `DeleteDraftService` y `DELETE /fleet/boats/draft` (borrado físico; 204).
- [x] T046 [US3] Frontend: modales «Borrador detectado», «¿Iniciar nuevo registro?» y los de error («El borrador ya no existe», «No se pudo eliminar el borrador», «No se pudo cargar el registro pendiente») en `ownerHome.js`.

**Punto de control**: las tres historias funcionan de forma independiente y juntas.

---

## Fase 6: Pulido y asuntos transversales

**Propósito**: mejoras que afectan a varias historias.

- [x] T047 Documentación: contrato REST de la funcionalidad, este plan y el diagrama del modelo físico con `registered_at`.
- [x] T048 Pruebas de arquitectura (`DependencyRulesTest`) y formato (`spotless`, ESLint, Prettier) en verde.
- [x] T049 Prueba manual: Postman y navegador de punta a punta con Cloudinary.
- [ ] T050 Agregar `instance` a las respuestas 401 y 403 (`ProblemDetailSecurityHandler`).
- [ ] T051 Configurar la URL del backend en Render y el origen CORS de producción al desplegar.

---

## Dependencias y orden de ejecución

### Dependencias entre fases

- **Setup (Fase 1)** y **Foundational (Fase 2)**: completadas; bloquean todas las historias.
- **Historia 1**: depende de la Fase 2. Entrega la lista y el borrador.
- **Historia 2**: depende de la Historia 1 (necesita un borrador para completarlo).
- **Historia 3**: depende de la Historia 1 (necesita un borrador); se integra con la lista.
- **Pulido (Fase 6)**: depende de las historias.

### Dentro de cada historia

- Modelos y reglas antes que servicios; servicios antes que controladores.
- Implementación del núcleo antes de la integración con el frontend.
- Historia completa antes de pasar a la siguiente prioridad.

## Notas

- La etiqueta [P] marca tareas que se pueden hacer en paralelo; [US#] la historia a la que pertenecen.
- Una foto de más de 10 MB responde 400 con «El tamaño de imagen se excede»; si además no es imagen, Spring corta antes y también responde ese mensaje, no «Formato de imagen inválido».
- La liberación de la matrícula de una embarcación eliminada (`deleted`) queda para la funcionalidad de eliminar.
- Variables de entorno de esta funcionalidad: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` y `UPLOAD_MAX_SIZE_MB` (10 por defecto). El secreto nunca va en el repositorio.
- Pruebas al cierre: 99 unitarias y 19 de integración en el backend, 111 en el frontend.
