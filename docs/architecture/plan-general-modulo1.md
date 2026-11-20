# Plan general de implementación: Módulo 1 (SeaShareM1)

> Este plan cubre únicamente lo común a todas las funcionalidades del Módulo 1. Las tareas y los contratos por funcionalidad se definen en los planes específicos de cada una. El modelo físico de base de datos se respeta tal cual, con una única excepción autorizada: la columna `vessel.registered_at` (ver «Almacenamiento»). No se agregan ni se cambian tablas. Todo lo que no está decidido está marcado como **[POR DEFINIR]**.

## Contexto técnico

**Lenguaje/Versión**: Java 17.0.12 (backend) y HTML, CSS y JavaScript puro con módulos ES nativos del navegador (frontend).

**Dependencias principales**:

- Backend: Spring Boot 4.1.1, Spring Security con JWT (biblioteca Nimbus que incluye Spring Security, algoritmo HS256, sin jjwt), Spring Data JPA, MapStruct 1.6.3 (`mapstruct` y `mapstruct-processor` con la misma versión), Flyway y la API REST de Cloudinary para imágenes (llamada con `RestClient`, sin el SDK de Cloudinary).
- Frontend: sin frameworks ni herramienta de empaquetado. Node.js se usa solo como herramienta de desarrollo para ESLint, Prettier y Vitest.
- Antes de crear los proyectos se verifica la compatibilidad de las dependencias y los plugins, sin actualizar automáticamente a versiones preliminares o incompatibles. Si aparece un problema real de compatibilidad, se documenta el error y se propone una solución antes de cambiar una versión.

**Almacenamiento**:

- MySQL 8 (Aiven), con el modelo físico existente como fuente de verdad: tablas `users`, `vessel`, `port` y `notification`. El modelo es inmutable y la implementación se adapta a él, salvo la excepción de la columna `registered_at` descrita abajo.
- Las migraciones se gestionan con Flyway, mediante scripts SQL versionados que reproducen el modelo físico.
- Las imágenes se almacenan en Cloudinary. MySQL guarda solo la URL (la dirección https segura) en `photo_path`, nunca el archivo binario.
- **Excepción autorizada al modelo (decisión del 4 de noviembre de 2026, funcionalidad Registrar Embarcación)**: se agrega a `vessel` la columna `registered_at` (`TIMESTAMP`, acepta NULL) mediante la migración V6. Queda vacía mientras la embarcación es un borrador y se llena la primera vez que pasa a `AVAILABLE`. Sirve para ordenar la lista de embarcaciones de la más reciente a la más antigua y para la etiqueta «NUEVA» (168 horas). El diagrama y las tablas de la sección «Modelo físico de datos» ya la incluyen.
- **Puertos**: `port.name` es único y un puerto puede tener varias embarcaciones. Al completar el registro, si ya existe un puerto con ese nombre (sin distinguir mayúsculas ni tildes) se reutiliza, con sus coordenadas originales; si no existe, se crea con las coordenadas elegidas.
- **Excepción autorizada al modelo (decisión del 7 de noviembre de 2026, funcionalidad Eliminar Embarcación)**: la migración V7 agrega a `vessel` las columnas `deleted_at` (`TIMESTAMP`, UTC, acepta NULL) y `deleted_by` (`CHAR(36)`, FK a `users.id`, acepta NULL) para guardar cuándo y quién eliminó la embarcación (propietario o administrador), y la columna generada `active_legal_registration`. El diagrama y las tablas de «Modelo físico de datos» ya las incluyen.
- **Matrícula legal**: debe ser única solo entre embarcaciones no eliminadas. El borrador se elimina de forma física, así que su matrícula queda libre al instante. La embarcación eliminada se conserva con `deleted = TRUE` (eliminación lógica); como MySQL no tiene índices parciales, la unicidad se garantiza con la columna generada `active_legal_registration` (`IF(deleted, NULL, legal_registration)`) y su índice único `uq_vessel_active_legal_registration`, que reemplaza al antiguo `uq_vessel_legal_registration`. Los valores NULL no chocan entre sí, así que la matrícula de una embarcación eliminada queda libre.
- **Tarifa base**: `base_rate` es `DECIMAL(12,2)`, así que el valor máximo aceptado es 9.999.999.999 COP. Ese es un límite técnico, no de negocio, y el mensaje es «La tarifa excede el valor permitido».

**Pruebas**:

- Backend: JUnit 5, Mockito y Spring Boot Test. MockMvc con Spring Security Test para verificar los permisos por rol. Pruebas de integración con Testcontainers sobre MySQL 8. ArchUnit para la regla de dependencias.
- Cobertura: JaCoCo con un mínimo de 80 % para el código propio de `domain` y `application`. No se exige ese porcentaje al resto del backend.
- Frontend: Vitest con jsdom.

**Plataforma objetivo**: aplicación web desplegada en la nube con costo inicial cero: frontend en Vercel, backend en Render, base de datos MySQL 8 en Aiven e imágenes en Cloudinary. El detalle está en la sección «Despliegue e Infraestructura». Mientras el proyecto no esté en evaluación se muestra en local, con MySQL 8 instalado en la máquina de desarrollo. El objetivo final es que backend y frontend estén alojados.

**Tipo de proyecto**: web, con dos repositorios independientes en GitHub: `SeaShareM1` (backend) y `SeaShareM1-Frontend` (frontend).

**Herramienta de construcción**: Maven (backend).

**Arquitectura**: en el backend, Clean Architecture estricta con las áreas `domain`, `application`, `infrastructure` y `configuration`. Los casos de uso son Java puro, sin anotaciones ni dependencias de Spring. Se registran desde `configuration` con `@Configuration` y `@Bean`. El frontend usa una estructura simple de carpetas con dos reglas de arquitectura limpia: la lógica va separada del código que toca el DOM, y el acceso al backend vive en un solo lugar. El detalle está en «Estructura del proyecto».

**Seguridad**:

- Autenticación con Spring Security y token JWT. El usuario inicia sesión con su número de documento (`document_number`) y su contraseña (`password`). Las contraseñas se guardan siempre con hash (por ejemplo BCrypt), nunca en texto plano.
- Control de acceso por roles (`OWNER` y `ADMIN`): las acciones disponibles dependen del rol del usuario. No se implementa seguridad fuerte.
- El token de acceso dura 30 minutos. La duración es configurable mediante variables de entorno y no se deja fija en el código.
- El backend valida la autenticación y la autorización en cada operación protegida. Los controles del frontend solo mejoran la navegación.
- El Módulo 3 consume los endpoints sin API key (el mecanismo con el Módulo 2 es **[POR DEFINIR]**). Es una decisión tomada, y los endpoints son accesibles para cualquiera que conozca la dirección del servicio desplegado.

**Convenciones de la API (aplican a toda la API del Módulo 1)**: prefijo `/api/v1`, JSON, identificadores UUID, campos en `snake_case`, decimales como texto y header opcional `X-Correlation-Id`. Hacia afuera la API usa el vocabulario del contrato (`boat_*`); dentro del backend se usa `vessel`. La conversión se hace en el adaptador web.

**Formato de errores**: RFC 9457 (Problem Details), igual que el Módulo 3. Cada error incluye `type`, `title`, `status`, `detail` (en español), `instance` y dos extensiones en `snake_case`: `correlation_id` y, en los errores 400 y en el 409 por matrícula duplicada, `errors` (lista de `field` y `message`). Su soporte con Spring Boot 4.1.1 está confirmado. Los códigos comunes están en «Catálogo común de errores»; la lista por endpoint se redacta al documentar la API.

**Calidad de código**: Spotless (backend); ESLint para JavaScript y Prettier para HTML, CSS y JavaScript (frontend).

**Idioma**: el código fuente, los identificadores técnicos y los nombres de clases y métodos van en inglés. La interfaz de usuario va en español.

**Resumen de despliegue e imágenes**: Vercel (frontend), Render (backend), Aiven (MySQL 8, sujeto a confirmar que el plan gratuito permita la versión requerida) y Cloudinary (imágenes). Ver «Despliegue e Infraestructura».

**Metas de rendimiento**:

- Las consultas, es decir, las operaciones que solo leen datos, tendrán como objetivo responder en menos de 1 segundo en condiciones normales de funcionamiento.
- Se mide el tiempo de respuesta del servidor, en el entorno desplegado y con datos representativos.
- Es un objetivo de rendimiento, no una garantía absoluta para cualquier condición de red, carga o infraestructura.
- Requisito específico de integración: la consulta de tarifas base del Módulo 3 debe responder en menos de 500 ms para un lote de hasta 50 embarcaciones (contrato UC02).
- Los planes gratuitos pueden apagar los servicios por inactividad, lo que afectaría estas metas en la primera llamada tras una pausa (ver «Despliegue e Infraestructura»).

**Restricciones**:

- Base de datos: el modelo físico existente es inmutable, salvo la columna `registered_at` en `vessel` (ver «Almacenamiento»).
- Control de acceso: por roles `OWNER` y `ADMIN`, con JWT y sin seguridad fuerte, como se describe en «Seguridad».
- Despliegue: en la nube con opciones gratuitas, sujeto a sus límites, disponibilidad y condiciones vigentes.
- Integración: el Módulo 3 y el Módulo 2 consultan al Módulo 1 por API REST, sin API key ni seguridad entre módulos. Con el Módulo 2 todo es HTTP, sin RabbitMQ y sin autenticación (contrato de integración v2.0 del 19 de noviembre de 2026; ver «Frontera con los módulos 2 y 3»).
- Idioma: código en inglés e interfaz en español.

**Escala/Alcance**: la aplicación se desarrollará inicialmente para un entorno académico y un volumen reducido de usuarios concurrentes. No se establecen compromisos de escalabilidad masiva ni objetivos de crecimiento empresarial. Las necesidades adicionales de capacidad y rendimiento se evaluarán si el alcance del proyecto cambia.

## Modelo físico de datos

Es la fuente de verdad de la base de datos (MySQL 8, InnoDB, `utf8mb4`) y lo reproducen las migraciones Flyway V1 a V11. Los identificadores son UUID guardados como `CHAR(36)`. Diagrama: `docs/diagrams/modelo-fisico-modulo1.svg` (también en PNG).

![Modelo físico del Módulo 1](diagrams/modelo-fisico-modulo1.svg)

**Relaciones**

| Desde | Hacia | Significado |
|---|---|---|
| `vessel.owner_id` | `users.id` | Cada embarcación pertenece a un propietario (N:1) |
| `vessel.port_id` | `port.id` | Puerto de atraque; varias embarcaciones pueden compartir puerto (N:1) |
| `notification.recipient_id` | `users.id` | Destinatario de la notificación (N:1) |
| `notification.vessel_id` | `vessel.id` | Embarcación a la que se refiere (N:1) |

### `users`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `CHAR(36)` | PK |
| `full_name` | `VARCHAR(100)` | NOT NULL |
| `document_number` | `BIGINT` | NOT NULL, único |
| `email` | `VARCHAR(150)` | NOT NULL, único |
| `phone` | `VARCHAR(20)` | NOT NULL |
| `password` | `VARCHAR(255)` | NOT NULL (hash, nunca texto plano) |
| `role` | `ENUM('OWNER','ADMIN')` | NOT NULL |

### `port`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `CHAR(36)` | PK |
| `name` | `VARCHAR(100)` | NOT NULL, único |
| `latitude` | `DECIMAL(9,6)` | NOT NULL |
| `longitude` | `DECIMAL(9,6)` | NOT NULL |

### `vessel`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `CHAR(36)` | PK |
| `name` | `VARCHAR(100)` | NOT NULL |
| `legal_registration` | `VARCHAR(30)` | NOT NULL (la unicidad entre no eliminadas la da `active_legal_registration`) |
| `type` | `ENUM('MOTORBOAT','YACHT','CATAMARAN','SAILBOAT')` | NOT NULL |
| `max_capacity` | `SMALLINT` | NOT NULL |
| `photo_path` | `VARCHAR(255)` | NULL mientras sea borrador (URL de Cloudinary) |
| `has_life_jackets`, `has_cooler_with_ice`, `has_sound_system`, `has_fishing_gear`, `has_diving_gear` | `BOOLEAN` | NULL mientras sea borrador |
| `base_rate` | `DECIMAL(12,2)` | NULL mientras sea borrador |
| `status` | `ENUM('DRAFT','AVAILABLE','RESERVED','MAINTENANCE','NAVIGATION')` | NOT NULL |
| `maintenance_by` | `ENUM('OWNER','ADMIN')` | NULL; solo tiene valor en mantenimiento |
| `port_id` | `CHAR(36)` | FK a `port.id`, NULL mientras sea borrador |
| `owner_id` | `CHAR(36)` | FK a `users.id`, NOT NULL |
| `deleted` | `BOOLEAN` | NOT NULL, por defecto `FALSE` |
| `registered_at` | `TIMESTAMP` | NULL; **agregada en V6** (excepción autorizada), en UTC. Vacía en borradores y se llena la primera vez que pasa a `AVAILABLE` |
| `deleted_at` | `TIMESTAMP` | NULL; **agregada en V7** (excepción autorizada), en UTC. Se llena al eliminar la embarcación |
| `deleted_by` | `CHAR(36)` | FK a `users.id`, NULL; **agregada en V7**. Usuario (propietario o administrador) que eliminó la embarcación |
| `active_legal_registration` | `VARCHAR(30)` | Columna generada almacenada, **agregada en V7**: la matrícula mientras `deleted = FALSE` y NULL cuando está eliminada. Tiene el índice único `uq_vessel_active_legal_registration` |

### `notification`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `CHAR(36)` | PK |
| `recipient_id` | `CHAR(36)` | FK a `users.id`, NOT NULL |
| `vessel_id` | `CHAR(36)` | FK a `vessel.id`, NOT NULL |
| `type` | `ENUM('MAINTENANCE_NOTICE','REVIEW_REQUEST','VESSEL_AVAILABLE')` | NOT NULL |
| `message` | `TEXT` | NOT NULL |
| `is_read` | `BOOLEAN` | NOT NULL, por defecto `FALSE` |
| `created_at` | `TIMESTAMP` | NOT NULL, por defecto la hora actual |

### Migraciones

| Versión | Contenido |
|---|---|
| V1 | Tabla `users` |
| V2 | Tabla `port` |
| V3 | Tabla `vessel` |
| V4 | Tabla `notification` |
| V5 | Usuarios iniciales de prueba (1 administrador y 5 propietarios) |
| V6 | Columna `vessel.registered_at` |
| V7 | Eliminación lógica: `vessel.deleted_at`, `vessel.deleted_by` y la matrícula única solo entre no eliminadas (`active_legal_registration`) |
| V8 | Nuevo tipo de notificación `VESSEL_DELETED` (el administrador elimina una embarcación) |
| V9 | `vessel.review_requested`: marca que el propietario ya solicitó la revisión del mantenimiento actual (una sola vez) |
| V10 | Estado operativo por avisos del Módulo 2: `vessel.status_changed_at` y las tablas `processed_event` (avisos ya procesados) y `vessel_status_history` (historial de cambios de estado, automáticos y manuales). Excepción autorizada el 13 de noviembre de 2026 |
| V11 | Integración M1–M2 v2.0 (autorizada el 19 de noviembre de 2026): columnas `vessel.maintenance_start` y `vessel.maintenance_until` (ventana del mantenimiento, en UTC) y tabla `maintenance_event_outbox` (avisos de mantenimiento pendientes de enviar al Módulo 2) |

## Frontera con los módulos 2 y 3

Toda la comunicación entre módulos es **HTTP**, sin RabbitMQ y sin autenticación entre módulos (decisión D-01 del contrato de integración M1–M2 v2.0, del 19 de noviembre de 2026). El Módulo 1 responde las consultas de los módulos 2 y 3 y, por primera vez, **llama al Módulo 2** para avisarle de un mantenimiento (C5).

| Módulo | Qué da el Módulo 1 | Qué recibe el Módulo 1 |
|---|---|---|
| Módulo 2 (Gestión de reservas) | Ficha de la embarcación (C1), catálogo paginado (C2) y estado operativo con la ventana de mantenimiento (C3). Además le **envía** el aviso de mantenimiento (C5). | El aviso del nuevo estado operativo (C4, HTTP). |
| Módulo 3 (Gestión de liquidación) | La tarifa base (consulta REST por lote). | Nada. |

**Endpoints de integración (los de entrada son públicos, sin autenticación)**

| Conexión | Módulo | Método y ruta | Para qué sirve | Archivos principales |
|---|---|---|---|---|
| — | 3 | `POST /api/v1/boats/base-rates` | Recibe `boat_ids` y devuelve la tarifa base de cada una | `BaseRateController`, `BaseRateQueryService` |
| C1 | 2 | `GET /api/v1/vessels/{id}` | Ficha de la embarcación (pasos 1 y 2, foto y estado; sin tarifa) | `ReservationsVesselController`, `ReservationsVesselQueryService` |
| C2 | 2 | `GET /api/v1/vessels?page&size&q` | Catálogo paginado `{vessels[], pagination{...}}`; todas las registradas (ni borrador ni eliminadas), por nombre; `q` filtra por nombre o puerto | `ReservationsVesselController`, `VesselCatalogResponse` |
| C3 | 2 | `GET /api/v1/vessels/{id}/operational-status` | Estado operativo con `maintenance_start` y `maintenance_until` (siempre presentes, `null` si no hay). Si el fin ya pasó responde `AVAILABLE` con fechas `null` (liberación perezosa, sin job) | `OperationalStatusResponse`, `ReservationsVesselQueryService` |
| C4 | 2 | `POST /api/v1/internal/status-events` | Aviso de estado (`event_id`, `vessel_id`, `operational_status`, `occurred_at`). Ahora acepta `AVAILABLE → NAVIGATION` | `StatusEventController`, `AssignOperationalStatusService` |
| C5 | 1 → 2 | `POST {M2_BASE_URL}/api/v1/internal/maintenance-events` | Aviso de mantenimiento iniciado por el propietario o el administrador | `MaintenanceEventRelay`, tabla `maintenance_event_outbox` |

**Mantenimiento con ventana (D-05 a D-07, D-09, D-11, D-12).** Al enviar a mantenimiento (propietario o administrador) la fecha de fin `maintenance_until` es obligatoria, futura y posterior al inicio `maintenance_start`, que es opcional (por defecto, ahora). Se guarda en `vessel` y, en la misma transacción, se escribe el aviso en `maintenance_event_outbox`. Un proceso en segundo plano (`MaintenanceEventRelay`, cada segundo) lo envía al Módulo 2 con conexión de 1 s y lectura de 5 s, hasta 5 intentos con esperas de 1, 5, 25 y 125 s ante timeout, 503 o 5xx, y sin reintentar ante 4xx. La URL del Módulo 2 viene de la variable de entorno `M2_BASE_URL` (sin valor por defecto; vacía = no se envía nada). Terminar el mantenimiento antes de tiempo no envía aviso. Un mantenimiento que llega por el aviso C4 (`MAINTENANCE_CLEANING`) no tiene ventana ni genera aviso C5.

**Contrato con el Módulo 3**: UC02 «Brindar tarifa base», `POST /api/v1/boats/base-rates`. Recibe una lista `boat_ids` y responde una lista `rates` con `boat_id` y `base_rate`. Es una consulta de solo lectura, sin efectos secundarios, con un tiempo de respuesta menor a 500 ms para lotes de hasta 50 embarcaciones.

**Contrato con el Módulo 2**: contrato de integración M1–M2 v2.0 (19 de noviembre de 2026, normativo) y su diagnóstico de factibilidad. Los estados `IN_NAVIGATION` y `MAINTENANCE_CLEANING` que usa M2 se aceptan y equiparan a `NAVIGATION` y `MAINTENANCE`; M2 dejó de enviar `RESERVED`. Zona horaria de los puertos: `America/Bogota` (fija en el código; no hay columna en la base).

**Regla de negocio**: una embarcación solo está disponible si tiene toda su información completa, así que el Módulo 1 nunca devuelve una tarifa base nula.

**Embarcación inexistente, eliminada o sin información completa**: en la consulta de tarifas base no se incluye en la respuesta, que se devuelve con las tarifas de las demás embarcaciones. No se devuelve un error ni un valor nulo.

**Documentación de la API**: archivos Markdown en `docs/architecture/contracts/rest`, redactados junto con Claude, que pregunta lo necesario. En esa documentación se acuerdan los errores que aplican a cada endpoint, según el catálogo común.

**Puerto local del Módulo 1**: 8081, que es la dirección que el Módulo 3 usa por defecto para llegar al sistema de flota (`http://localhost:8081`).

## Catálogo común de errores

| Estado | Cuándo |
|---|---|
| 400 | Campos inválidos o faltantes; foto con tipo no permitido (solo PNG y JPG) o mayor a 10 MB |
| 401 | Sin token, token expirado o credenciales inválidas. El inicio de sesión devuelve el mismo error y un mensaje genérico tanto si el documento no existe como si la clave es incorrecta |
| 403 | El rol del usuario no puede realizar la acción |
| 404 | Recurso inexistente o perteneciente a otro propietario. Excepción: la consulta de tarifas base (UC02) omite esas embarcaciones de la respuesta en vez de devolver error |
| 409 | El estado actual no permite la acción (por ejemplo, enviar a mantenimiento una embarcación que no está Disponible) o ya existe un borrador de registro, o la matrícula legal ya está registrada (en ese caso la respuesta incluye `errors` con el campo y el mensaje) |
| 502 | Falla del servicio de imágenes (Cloudinary) |
| 500 | Error inesperado, sin exponer detalles internos |

Una foto mayor a 10 MB se responde con 400 (no con 413) y el mensaje «El tamaño de imagen se excede».

Cada endpoint documentado en `docs/architecture/contracts/rest` lista solo los errores que le aplican y define sus mensajes en español.

## Estructura del proyecto

### Documentación (repositorio SeaShareM1, estructura actual)

```text
docs/
├── architecture/
│   ├── contracts/
│   │   └── rest/                  # documentación de la API
│   └── README.md
├── general-technical-spec.md      # este documento
├── diagrams/
├── features/
└── wireframes/
```

### Código fuente: backend (repositorio `SeaShareM1`)

La estructura comienza en la raíz del repositorio, sin carpeta `backend/`. Paquete base: `com.seashare.seasharem1`. groupId: `com.seashare`.

```text
pom.xml
src/
├── main/
│   ├── java/com/seashare/seasharem1/
│   │   ├── domain/                       # modelos y reglas de negocio, sin frameworks
│   │   │   ├── model/
│   │   │   └── exception/
│   │   ├── application/                  # casos de uso y puertos
│   │   │   ├── port/in/                  # puertos de entrada
│   │   │   ├── port/out/                 # puertos de salida
│   │   │   └── usecase/                  # Java puro, sin Spring
│   │   ├── infrastructure/               # adaptadores
│   │   │   ├── adapter/in/web/           # controladores, DTOs, mappers, filtros (correlation_id y logs de peticiones)
│   │   │   ├── adapter/out/persistence/  # entidades JPA, repositorios, mappers, adaptadores
│   │   │   ├── adapter/out/storage/      # adaptador de Cloudinary
│   │   │   └── security/                 # filtros JWT y mecanismos de autenticación
│   │   └── configuration/                # beans, inyección de dependencias, Spring Security
│   └── resources/
│       └── db/migration/                 # migraciones Flyway
└── test/
    └── java/com/seashare/seasharem1/
        ├── unit/
        ├── integration/
        └── architecture/                 # pruebas de ArchUnit
```

**Decisión de estructura**:

- La regla de dependencias de Clean Architecture se cumple de verdad: las capas externas pueden depender de las internas, pero `domain` y `application` no dependen de Spring, JPA, HTTP, MySQL ni de implementaciones concretas de infraestructura. ArchUnit lo verifica.
- Los casos de uso son Java puro y se registran desde `configuration` con `@Configuration` y `@Bean`.
- Las interfaces de los puertos viven en las capas internas (`application/port/in` y `application/port/out`), y sus implementaciones técnicas viven en `infrastructure`. Los puertos de persistencia son independientes de Spring Data JPA.
- El modelo de dominio y la entidad JPA están siempre separados en las cuatro tablas, y se convierten con mappers MapStruct.
- Los controladores solo reciben y devuelven DTOs y delegan en los casos de uso, sin lógica de negocio ni acceso a la persistencia.
- Los componentes técnicos de seguridad están en `infrastructure/security`. La configuración de Spring Security está en `configuration` y no introduce dependencias de infraestructura en las capas internas.
- No se crean paquetes por funcionalidad ni capas adicionales antes de conocer los requisitos de cada una.
- Maven se configura explícitamente solo si se necesitan directorios físicos independientes para las pruebas.

### Código fuente: frontend (repositorio `SeaShareM1-Frontend`)

La estructura comienza en la raíz de su propio repositorio.

```text
index.html
package.json      # solo para herramientas de desarrollo (ESLint, Prettier, Vitest)
src/
├── config.js     # direcciones del backend (local y desplegada)
├── api/          # cliente HTTP centralizado: único lugar que habla con el backend; errores y envío del token
├── auth/         # estado de autenticación, sesión del usuario y rol
├── services/     # lógica sin acceso al DOM
├── components/   # elementos de interfaz reutilizables
├── pages/        # pantallas de la aplicación
├── routes/       # rutas y navegación según el rol
└── styles/       # hojas de estilo CSS
```

**Decisión de estructura**:

- Estructura simple de carpetas con dos reglas de arquitectura limpia: la lógica va separada del código que toca el DOM (`services/`), y el acceso al backend vive solo en `api/`, sin configuración duplicada en cada página.
- Los archivos se separan con módulos ES nativos del navegador (`import` y `export`). El sitio debe servirse desde un servidor y no abrirse como archivo local.
- La protección de rutas y elementos de interfaz por rol solo mejora la navegación. El backend valida cada operación protegida.
- No se incluyen secretos ni API keys privadas en el frontend, ni credenciales en archivos de configuración del cliente.
- No se agregan carpetas por funcionalidad ni bibliotecas adicionales hasta que los requisitos lo justifiquen.
- La dirección del backend se resuelve con un archivo de configuración que contiene las dos direcciones (local y desplegada). El frontend elige una según dónde se abre: si la página está en `localhost` usa la local y, en caso contrario, la desplegada. La dirección del backend no es un secreto.

Ambos repositorios tienen sus propios archivos de configuración, dependencias, pruebas y README. La separación se respeta también en los procesos de desarrollo y despliegue.

## Despliegue e Infraestructura

**Objetivo**: una aplicación académica accesible por internet, con costo inicial cero y sin servicios de pago obligatorios. No se asume que los servicios serán siempre gratuitos ni que estarán disponibles sin interrupciones. Mientras el proyecto no esté en evaluación se muestra en local, con MySQL 8 instalado en la máquina de desarrollo. El objetivo final es que backend y frontend estén desplegados.

| Componente | Plataforma | Nota |
|---|---|---|
| Frontend (HTML, CSS y JavaScript puro) | Vercel | Repositorio `SeaShareM1-Frontend`. Sitio estático. |
| Backend Spring Boot | Render | Repositorio `SeaShareM1`. |
| Base de datos MySQL 8 | Aiven | Sujeto a confirmar que el plan gratuito permita la versión requerida. |
| Imágenes | Cloudinary | Ver abajo. |

**Imágenes (Cloudinary)**:

- El backend Spring Boot gestiona la carga de las fotografías y almacena en MySQL solo la URL o referencia de cada imagen.
- Las credenciales de Cloudinary van en variables de entorno y nunca se exponen en el frontend.
- Se valida el tipo y el tamaño de los archivos.
- Una URL típica de Cloudinary mide alrededor de 100 caracteres y cabe en `photo_path`, que es `VARCHAR(255)`. Por esto no se cambia el modelo físico.

**Variables de entorno (a revisar por el equipo)**: la duración del JWT, las credenciales de Cloudinary y la conexión a la base de datos se configuran por variables de entorno, con nombres según la convención del Módulo 3.

| Variable | Uso | Valor |
|---|---|---|
| `APP_PORT` | Puerto del backend | 8081 |
| `SPRING_PROFILES_ACTIVE` | Perfil de ejecución | `local` o `prod` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a MySQL 8 | — |
| `JWT_SECRET` | Clave para firmar los tokens | — |
| `JWT_EXPIRATION_MINUTES` | Duración del token de acceso | 30 |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Credenciales de Cloudinary | — |
| `UPLOAD_MAX_SIZE_MB` | Tamaño máximo de foto | 10 |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend permitidos, separados por comas, sin comodín `*` | Local: `http://localhost:5500,http://127.0.0.1:5500` (Live Server). Producción: el dominio de Vercel, que se conoce al desplegar |

Los secretos no se versionan ni se incluyen en el frontend.

**Consideraciones a verificar (según la documentación consultada el 21 de octubre de 2026; las condiciones cambian)**:

- Render, plan gratuito: la documentación indica que el servicio se apaga tras unos 15 minutos sin tráfico, que reactivarlo puede tardar hasta un minuto y que el disco es efímero. Esto afecta las metas de rendimiento, incluido el límite de 500 ms de la integración con el Módulo 3.
- Aiven, plan gratuito: la documentación indica un servicio MySQL gratuito por organización, sin cobertura de SLA, que puede apagarse por inactividad. Las cifras de almacenamiento difieren entre páginas (1 GB y 5 GB), y la versión de MySQL disponible en ese plan no está confirmada.

**Verificaciones previas a implementar el despliegue**:

1. Límites y requisitos vigentes de los planes gratuitos de Render, Vercel, Aiven y Cloudinary.
2. Compatibilidad de MySQL 8 con el plan gratuito de Aiven (versión disponible).
3. Conectividad segura entre el backend y la base de datos.
4. Variables de entorno de cada plataforma.
5. CORS entre el frontend y el backend. Por ahora el proyecto se muestra en local, y el origen local es el de Live Server (puerto 5500). El origen de Vercel se conoce al desplegar (**[POR DEFINIR]**).
6. HTTPS.
7. Estrategias de respaldo.
8. Generación correcta de los mappers con MapStruct 1.6.3, Java 17 y Spring Boot 4.1.1 (verificado: compilan y pasan las pruebas).
9. Soporte de RFC 9457 en Spring Boot 4.1.1 (verificado).

## Fase 1: Setup (infraestructura compartida)

**Propósito**: inicialización de los proyectos y de la base técnica. No se agregan funcionalidades de negocio.

- [x] T001 Backend: crear el proyecto Maven con Java 17 y Spring Boot 4.1.1, con la estructura de paquetes aprobada (groupId `com.seashare`, paquete base `com.seashare.seasharem1`), verificando la compatibilidad de dependencias y plugins.
- [x] T002 Frontend: crear el proyecto con HTML, CSS y JavaScript puro, con módulos ES nativos y las carpetas aprobadas.
- [x] T003 Backend: configurar Spotless para aplicar y verificar el formato del código.
- [x] T004 Frontend: configurar ESLint (JavaScript) y Prettier (HTML, CSS y JavaScript) para el análisis estático y el formato.
- [x] T005 Backend: preparar las pruebas unitarias, de integración y de arquitectura con ArchUnit, de modo que Maven pueda ejecutarlas.
- [x] T006 Backend: configurar JaCoCo con un mínimo de 80 % para el código propio de `domain` y `application`:
  - se mide sobre las clases de producción;
  - se excluyen las clases generadas automáticamente y el código ajeno a la lógica propia de esas capas, justificando cada exclusión;
  - la verificación falla por debajo del 80 %;
  - el umbral no se exige al resto del backend;
  - en el Setup esos paquetes aún no tienen lógica, así que la verificación no debe fallar por falta de clases que medir.
- [x] T007 Frontend: configurar Vitest con jsdom y una prueba sencilla de ejemplo.
- [x] T008 Ambos: definir scripts y comandos claros para ejecutar las pruebas y las verificaciones de calidad.
- [x] T009 Ambos: documentar en el README de cada repositorio los comandos principales para instalar dependencias, ejecutar el proyecto, probarlo, verificar la calidad y consultar el informe de cobertura. En el frontend se incluye cómo servir el sitio en local, porque los módulos ES requieren un servidor.

**Punto de control**: base técnica funcional y verificable en ambos repositorios, lista para desarrollar las funcionalidades.

---

## Fase 2: Foundational (prerrequisitos bloqueantes)

**Propósito**: infraestructura común que debe estar completa antes de implementar cualquier funcionalidad.

**⚠️ CRÍTICO**: no se puede empezar el trabajo en funcionalidades hasta completar esta fase.

- [x] T010 Backend: crear las migraciones Flyway que reproduzcan el modelo físico (`users`, `vessel`, `port`, `notification`) sin cambios. Al implementar Registrar Embarcación se agrega la migración V6 con la columna `registered_at` (excepción autorizada).
- [x] T011 Backend: crear la migración de datos iniciales con los usuarios iniciales (propietarios y administrador). Las contraseñas se insertan ya con hash, nunca en texto plano.
- [x] T012 Backend: crear los modelos de dominio, las entidades JPA y los mappers MapStruct de las cuatro tablas, siempre separados.
- [x] T013 Backend: implementar la autenticación con JWT y el control de acceso por rol (`OWNER`, `ADMIN`), con inicio de sesión por número de documento y contraseña (comparada contra el hash guardado). El token dura 30 minutos, configurable por variable de entorno.
- [x] T014 Backend: implementar el manejo global de excepciones, con excepciones específicas y errores en formato RFC 9457, según el catálogo común de errores.
- [x] T015 Backend: implementar la base de validación de entrada.
- [x] T016 Backend: configurar los entornos mediante variables de entorno, sin secretos en el código, incluido el límite de tamaño de archivos de 10 MB (`UPLOAD_MAX_SIZE_MB`), porque Spring limita por defecto a 1 MB.
- [x] T017 Backend: configurar el registro de logs.
- [x] T018 Backend: configurar CORS entre el frontend y el backend.
- [x] T019 Backend: crear la infraestructura común de almacenamiento de imágenes: un puerto de salida en `application/port/out` y su adaptador de Cloudinary en `infrastructure`, con credenciales por variables de entorno. Se implementa con la API REST de Cloudinary (subida firmada), sin el SDK.
- [x] T020 Backend: crear la base de pruebas: pruebas unitarias de los casos de uso sin base de datos real y pruebas de integración con Testcontainers sobre MySQL 8.
- [x] T021 Backend: crear las pruebas de arquitectura con ArchUnit que verifican la regla de dependencias.
- [x] T022 Frontend: crear la base del frontend: cliente HTTP centralizado en `api/`, estado de autenticación en `auth/` y rutas protegidas por rol en `routes/`, con la interfaz en español.

**Punto de control**: la base está lista y las funcionalidades pueden comenzar.

---

## Pendientes por definir

1. ~~Consumidor de RabbitMQ~~ Ya no aplica: el Módulo 2 cambió a HTTP (contrato v2.0). Pendiente de confirmar con el Módulo 2: valor real de `M2_BASE_URL` al desplegar.
2. Origen de CORS de producción (dominio de Vercel), que se conoce al desplegar, y confirmar que los nombres de las variables de entorno coinciden con la convención del Módulo 3.
3. Las verificaciones previas de la sección «Despliegue e Infraestructura» que siguen abiertas: límites de los planes gratuitos, versión de MySQL en Aiven, conectividad, variables de cada plataforma, HTTPS y respaldos.
4. ~~Liberación de la matrícula legal cuando una embarcación se elimina~~ Resuelto en la migración V7 (ver «Contexto técnico»).
5. Mantener actualizado el diagrama del modelo físico: cada migración que cambie una tabla actualiza la sección «Modelo físico de datos» y `docs/diagrams/modelo-fisico-modulo1.svg`.
