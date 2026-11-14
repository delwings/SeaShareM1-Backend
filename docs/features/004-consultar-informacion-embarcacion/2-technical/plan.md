# Plan de implementación: Consultar detalles de embarcación (vista del Propietario)

**Fecha**: 7 de noviembre de 2026 (actualizado el 13 de noviembre de 2026)
**Especificación**: `docs/features/consultar-detalles/spec.md` (creada el 28 de octubre de 2026, actualizada el 10 de noviembre de 2026)
**Contrato REST**: `docs/features/consultar-detalles/contract.md`
**Estado**: implementada para el Propietario (backend y frontend, probada en el navegador, `mvn clean verify` en verde) y, desde el 10 de noviembre, para el Administrador (listado y detalle de solo lectura; pruebas del frontend en verde: 287). La parte del Administrador está pendiente de `mvn spotless:apply`, `mvn clean verify` y prueba manual.

> Lo común a todo el módulo (stack, seguridad, errores, despliegue, modelo físico) está en el plan general. Esta funcionalidad se apoya en Registrar Embarcación (`docs/features/registrar-embarcacion/plan.md`): reutiliza el modelo de embarcación, el mapper web y la lista «Mis Embarcaciones Registradas».

## Resumen

El propietario pulsa «Ver Detalles» en su lista y ve la ficha completa de la embarcación: nombre, estado, fotografía, matrícula, tipo, capacidad, tarifa base, puerto, latitud, longitud y servicios, con los botones de acción de su rol.

**Alcance**: primero la vista del Propietario (7 de noviembre) y después la del Administrador: «Control General de Flota Fluvial» y su «Ver Detalles» (historia 2, 10 de noviembre), ambos de solo lectura. Quedan para después las consultas del Módulo 2 (historias 4 y 5).

Enfoque técnico: un único endpoint de lectura, `GET /api/v1/fleet/boats/{boatId}`, que reutiliza la respuesta de embarcación completada de Registrar. El frontend agrega una ruta con parámetro (`/owner/boats/:boatId`) y la pantalla de detalle.

### Decisiones de diseño

| # | Decisión | Motivo |
|---|---|---|
| D1 | Un solo endpoint `GET /fleet/boats/{boatId}` con la misma forma que la respuesta del paso 2 de Registrar | Reutiliza `CompletedBoatResponse` y `BoatWebMapper`; el frontend no necesita un formato nuevo |
| D2 | Embarcación ajena, inexistente, eliminada o borrador responden el mismo 404 «Embarcación no encontrada.» | FR-013 y NFR-001: no revelar si el identificador existe |
| D3 | Identificador que no es UUID responde 400, y la pantalla lo trata igual que el 404 (sin «Reintentar») | No hay nada que reintentar; documentado como [S-26] |
| D4 | El endpoint hereda `@PreAuthorize("hasRole('OWNER')")` del controlador; el administrador recibe 403 | La vista del Administrador se hace después, con su propio endpoint o regla |
| D5 | La pantalla consulta siempre al servidor al abrirse; no usa datos de la lista | FR-008 |
| D6 | Latitud y longitud en tarjetas separadas; el puerto muestra solo su nombre | Wireframe y decisión del usuario; spec actualizada (FR-017) |
| D7 | «Editar» solo habilitado en `AVAILABLE` y `MAINTENANCE`. Los botones del Propietario ya ejecutan sus flujos (Editar, Enviar/Terminar mantenimiento, Eliminar); los del Administrador solo se muestran | FR-023; sus flujos pertenecen a sus propios specs |
| D8 | Un 401 no muestra error: el cliente HTTP redirige al inicio de sesión | FR-007 (versión mínima; ver pendientes) |
| D9 | Datos faltantes se muestran como «—» sin romper la pantalla | FR-021 |
| D10 | El puerto es tolerante a nulos en backend y mapper | Una embarcación sin puerto no debe producir un 500 |
| D11 | El Administrador tiene endpoints propios (`/admin/fleet/...`) con `@PreAuthorize("hasRole('ADMIN')")`; el detalle devuelve la misma forma que el del Propietario | Reutiliza `CompletedBoatResponse`; no mezcla reglas de pertenencia |
| D12 | La pantalla de detalle es una sola (`createBoatDetailPage`) configurada por rol: fuente de datos, destino de «Volver», subtítulo de servicios y botones | Un único diseño para ambos roles (wireframes) |
| D13 | El detalle del Administrador no muestra el propietario ni «Editar» | Wireframe y decisión del usuario |
| D14 | El listado del Administrador filtra en el servidor (propietario y estado; `q` sigue disponible pero la pantalla no lo usa) y los filtros **excluyen** filas | Decisión del usuario (10 de noviembre de 2026); el spec se ajustó (FR-028) |
| D14b | «Volver» desde el detalle del Administrador regresa a la tabla sin filtros | Decisión del usuario; el spec se ajustó (FR-003, FR-004) |
| D15 | Etiqueta del estado «Mantenimiento» y botón «Terminar mantenimiento» | Decisión del usuario |
| D16 | Botón de mantenimiento y «Eliminar» deshabilitados en `RESERVED` y `NAVIGATION` | Spec «Enviar a Mantenimiento», FR-001 |
| D17 | Sin paginación; el filtrado se hace en memoria en `AdminFleetService` | Volumen actual pequeño; se revisará si crece |

## Contexto técnico

**Lenguaje/Versión**: Java 17 (backend); HTML, CSS y JavaScript puro con módulos ES (frontend).
**Dependencias principales**: ninguna nueva.
**Almacenamiento**: MySQL existente; sin migraciones nuevas (solo lectura).
**Pruebas**: JUnit 5, Mockito y Testcontainers (backend); Vitest con jsdom (frontend).
**Plataforma objetivo**: Render (backend) y Vercel (frontend); en local, puertos 8081 y 5500.
**Tipo de proyecto**: web, dos repositorios.
**Metas de rendimiento**: datos de texto en menos de 1,5 s en el 95% de los accesos (NFR-002); la fotografía no cuenta.
**Restricciones**: textos en español exactamente como en la spec; funciona desde 360 px de ancho; sin tooltips.
**Escala/Alcance**: Propietario: 1 endpoint, 1 pantalla, 1 ruta; Administrador: 3 endpoints, 1 listado y 1 ruta de detalle; historias 1 y 2 y parte de la historia 3 de la spec.

## Estructura del proyecto

### Documentación (esta funcionalidad)

```text
docs/features/consultar-detalles/
├── plan.md        # este archivo
├── contract.md    # contrato REST
└── spec.md        # especificación (consultar-detalles-spec.md)
```

### Código fuente (raíz del repositorio)

```text
SeaShareM1/  (backend)
├── src/main/java/com/seashare/seasharem1/
│   ├── application/
│   │   ├── port/in/GetBoatDetailUseCase.java        # nuevo
│   │   └── usecase/GetBoatDetailService.java        # nuevo
│   │   (Administrador, 10 nov.)
│   │   ├── port/in/FleetFilter.java, FleetItem.java, ListFleetUseCase.java,
│   │   │          ListFleetOwnersUseCase.java, GetAdminBoatDetailUseCase.java   # nuevos
│   │   ├── port/out/VesselRepositoryPort.java, UserRepositoryPort.java          # modificados: findAllRegistered, findByIds
│   │   └── usecase/AdminFleetService.java                                       # nuevo
│   ├── infrastructure/adapter/in/web/
│   │   ├── BoatController.java                      # modificado: GET /{boatId}
│   │   ├── BoatWebMapper.java                       # modificado: tolera tarifa y puerto nulos
│   │   ├── AdminFleetController.java                # nuevo: /admin/fleet (listado, propietarios, detalle)
│   │   └── dto/AdminBoatListResponse.java, OwnerResponse.java, OwnerListResponse.java   # nuevos
│   └── configuration/UseCaseConfig.java             # modificado: beans getBoatDetailUseCase y adminFleetService
└── src/test/java/com/seashare/seasharem1/
    ├── unit/application/GetBoatDetailServiceTest.java   # nuevo
    ├── unit/application/AdminFleetServiceTest.java      # nuevo (Administrador)
    └── integration/BoatRegistrationApiIT.java           # modificado: pruebas del detalle

SeaShareM1-Frontend/
└── src/
    ├── pages/
    │   ├── boatDetail.js                  # nuevo: pantalla de detalle (fábrica createBoatDetailPage, usada por ambos roles)
    │   ├── adminHome.js                   # Administrador: Control General de Flota Fluvial (+ test)
    │   ├── adminBoatDetail.js             # Administrador: Ver Detalles (+ test)
    │   ├── boatDetail.test.js             # nuevo
    │   ├── ownerHome.js                   # modificado: «Ver Detalles» navega al detalle
    │   └── ownerHome.test.js
    ├── routes/
    │   ├── routes.js                      # modificado: rutas con parámetro (/owner/boats/:boatId, /admin/boats/:boatId)
    │   ├── router.js                      # modificado: pasa `params` a la página
    │   └── routes.test.js
    ├── services/
    │   ├── fleetService.js                # modificado: getBoat(boatId)
    │   ├── fleetService.test.js
    │   ├── adminFleetService.js           # nuevo: listFleet, listFleetOwners, getAdminBoat (+ test)
    ├── ui/
    │   ├── boatFormat.js                  # nuevo: formato de tarifa, capacidad y coordenadas
    │   └── boatFormat.test.js
    ├── components/header.js               # modificado: «Módulo de Administración General» para el Administrador
    └── styles/main.css                    # modificado: tarjetas, esqueleto, regla [hidden], filtros del listado
```

**Decisión de estructura**: el formato de datos vive en `ui/boatFormat.js`, separado de la pantalla, para probarlo sin DOM y reutilizarlo en la lista y en «Editar» (NFR-006).

---

## Fase 1: Setup (infraestructura compartida)

Completada en el plan general (T001 a T009). Esta funcionalidad no agrega herramientas ni dependencias.

---

## Fase 2: Foundational (prerrequisitos bloqueantes)

Completada en el plan general (T010 a T022) y en Registrar Embarcación (T023 a T025). No hay migraciones ni cambios de seguridad nuevos.

**Punto de control**: la base está lista; la historia puede comenzar.

---

## Fase 3: Historia 1 - Consultar detalles como Propietario (Prioridad: P1)

**Objetivo**: abrir la ficha completa de una embarcación propia desde «Ver Detalles» y ver sus botones de acción.

**Prueba independiente**: con un propietario, abrir «Ver Detalles» de embarcaciones en los cuatro estados; confirmar datos, botón de mantenimiento y estado de «Editar»; abrir el enlace de una embarcación ajena y comprobar la pantalla de error.

### Pruebas de la historia 1

- [x] T061 [P] Backend: prueba unitaria `GetBoatDetailServiceTest`: devuelve la embarcación y su puerto al dueño; ajena, inexistente y borrador producen `ResourceNotFoundException`; un puerto ausente no rompe el detalle.
- [x] T062 [P] Backend: pruebas de integración en `BoatRegistrationApiIT`: detalle del dueño (200 con todos los campos), embarcación ajena (404), borrador e identificador desconocido (404), administrador (403).
- [x] T063 [P] Frontend: pruebas de `boatFormat`, `routes`, `fleetService.getBoat`, `ownerHome` y `boatDetail`.

### Implementación de la historia 1

- [x] T064 Backend: caso de uso `GetBoatDetailUseCase` y `GetBoatDetailService` (filtra por propietario y descarta borradores).
- [x] T065 Backend: `GET /fleet/boats/{boatId}` en `BoatController`; `BoatWebMapper.toCompleted` tolerante a tarifa y puerto nulos; bean en `UseCaseConfig`.
- [x] T066 Frontend: `getBoat(boatId)` en `fleetService` y ruta `/owner/boats/:boatId` (las rutas literales tienen prioridad; el router entrega `params`).
- [x] T067 Frontend: «Ver Detalles» en `ownerHome` navega al detalle de esa embarcación.
- [x] T068 Frontend: pantalla `boatDetail`: nombre y badge de estado, fotografía con imagen por defecto, tarjetas de datos, servicios base y adicionales, y botón «Volver».
- [x] T069 Frontend: barra de acciones: «Editar» (habilitado en `AVAILABLE` y `MAINTENANCE`), «Enviar a Mantenimiento» o «Terminar Mantenimiento» y «Eliminar». Sin tooltips; sin acción al pulsar (ver pendientes).

**Punto de control**: el propietario abre el detalle de sus embarcaciones y ve la información correcta.

---

## Fase 4: Historia 3 (parcial) - Formato, carga y errores (Prioridad: P2)

**Objetivo**: datos legibles y errores ordenados.

- [x] T070 Frontend: formato de tarifa (`$ 180.000`), capacidad (`12 pasajeros` / `1 pasajero`) y coordenadas (4 decimales con signo) en `ui/boatFormat.js`.
- [x] T071 Frontend: esqueleto de carga (`role="status"`), sin spinners.
- [x] T072 Frontend: pantalla de error (`role="alert"`) «No se pudo cargar la información de la embarcación»: 404 y 400 solo con «Volver a Mis Embarcaciones»; fallo de red o de servidor con «Volver» y «Reintentar».
- [x] T073 Frontend: latitud y longitud en tarjetas separadas y el puerto solo con su nombre (cambio pedido sobre el wireframe).
- [x] T074 Frontend: regla global `[hidden] { display: none !important; }` (el atributo `hidden` no se respetaba en `.btn`).
- [x] T075 Frontend: encabezado adaptado a 360 px (se oculta el módulo y el usuario por debajo de 560 px).
- [x] T076 Frontend: imagen por defecto de embarcación con la vela unida al casco.
- [x] T077 Verificación: ESLint, Prettier y 169 pruebas del frontend en verde; prueba manual en el navegador con datos reales.
- [x] T078 Backend: confirmado `mvn spotless:apply` y `mvn clean verify` con las pruebas del detalle.

**Punto de control**: pantalla completa, con carga, error y formato conforme a la spec.

---

## Fase 5: Historia 2 - Consultar detalles como Administrador (10 de noviembre de 2026)

**Objetivo**: que el Administrador vea toda la flota, filtre y abra el detalle de cualquier embarcación.

**Prueba independiente**: entrar con 1010000001, ver el listado con embarcaciones de varios propietarios, filtrar por propietario y estado, abrir «Ver Detalles» y comprobar que no hay «Editar».

- [x] T141 Backend: `AdminFleetService` (listado con filtros, propietarios y detalle) con sus casos de uso y los puertos `findAllRegistered` y `findByIds`.
- [x] T142 Backend: `AdminFleetController` (`/admin/fleet/boats`, `/boats/{id}`, `/owners`), solo ADMIN; filtros inválidos responden 400 con el campo.
- [x] T143 Backend: pruebas `AdminFleetServiceTest` y de integración (listado y filtros, propietarios, detalle de cualquiera, 404, 400, 403 para propietarios).
- [x] T144 Frontend: `adminFleetService` (`listFleet`, `listFleetOwners`, `getAdminBoat`).
- [x] T145 Frontend: `adminHome` con título, filtros por propietario y estado, tabla, vacío y error con «Reintentar».
- [x] T145b (11 de noviembre) La barra de búsqueda de propietario pasó al spec «Consultar información de propietario» y dejó de filtrar la tabla.
- [x] T146 Frontend: `boatDetail` convertido en fábrica `createBoatDetailPage` y `adminBoatDetail` (sin «Editar», botones solo visibles, «Volver» al listado).
- [x] T147 Frontend: ruta `/admin/boats/:boatId` y encabezado «Módulo de Administración General»; etiqueta «Mantenimiento».
- [ ] T148 Verificación: `mvn spotless:apply` y `mvn clean verify`; prueba manual del Administrador en el navegador.

**Punto de control**: el Administrador consulta cualquier embarcación; las pruebas del frontend están en verde (287).

---

## Fase 6: Historias 4 y 5 - Consultas del Módulo 2 (13 de noviembre de 2026)

**Objetivo**: que el Módulo 2 consulte la información de una embarcación y, por separado, su estado operativo.

**Decisiones**

| # | Decisión | Origen |
|---|---|---|
| D18 | Rutas `/api/v1/vessels/{id}` y `/operational-status`, abiertas (`permitAll` solo GET `/vessels/**`) | Contrato del Módulo 2; sin seguridad entre módulos (Jose) |
| D19 | Solo lo que existe en el modelo: sin eslora, camarotes ni `verified`; `captain_included=true`, zona `America/Bogota` | Decisión de Jose |
| D20 | Catálogo en lote fuera de alcance por ahora | Decisión de Jose |
| D21 | El estado se entrega con los nombres del Módulo 1 (`NAVIGATION`, `MAINTENANCE`) | Diccionario único; avisar al Módulo 2 |
| D23 | La ficha del Módulo 2 incluye foto y estado (pasos 1 y 2) y excluye solo la tarifa base | Decisión de Jose (13 de noviembre), spec ajustada |
| D22 | No hay conflicto con las rutas del Módulo 1 | Revisión de `@RequestMapping` |

- [x] T170 Backend: `ReservationsVesselQuery` y `ReservationsVesselQueryService` (información con foto y estado, y estado por aparte; borrador, eliminada e inexistente dan 404)
- [x] T171 Backend: `ReservationsVesselController`, DTO con los nombres del Módulo 2 y ruta pública
- [x] T172 Pruebas: `ReservationsVesselQueryServiceTest` (5) e IT (2 casos en `BoatRegistrationApiIT`)
- [ ] T173 Verificación: `mvn spotless:apply`, `mvn clean verify` y prueba en Postman
- [ ] T174 Avisar al Módulo 2 de los puntos del contrato (estados, campos, servicios, autenticación, catálogo)

---

## Pendientes y trabajo diferido

- [x] T079 Historia 2: vista del Administrador y «Control General de Flota Fluvial» hechos (fase 5). Decisiones: los filtros excluyen y «Volver» regresa sin filtros, por lo que no hay trabajo pendiente de filtros en la dirección.
- [x] T080 Historias 4 y 5: consultas del Módulo 2 hechas el 13 de noviembre (fase 6). Pendiente de verificación.
- [ ] T081 FR-007: aviso «Tu sesión ha expirado. Por favor, ingresa de nuevo.» y destino de retorno tras iniciar sesión (hoy el 401 solo redirige al inicio de sesión).
- [ ] T082 Acciones del Propietario: **hechas** (Eliminar, Editar T107–T125, Enviar/Terminar mantenimiento T129–T137). Acciones del Administrador (mantenimiento con motivo, Eliminar): pendientes, cada una en su spec.
- [x] T083 Tarifa máxima: rige el límite de Registrar (9.999.999.999); spec actualizada.

---

## Dependencias y orden de ejecución

- Fase 3 depende de Registrar Embarcación (la lista y el modelo ya existen).
- T064 y T065 (backend) preceden a T066 y T068 en un entorno real; en pruebas, el frontend usa la API simulada.
- Dentro de la fase 3, las pruebas T061 a T063 son independientes entre sí.
- T079 a T082 dependen de sus propios specs.

## Notas

- **Tarifa máxima**: se decidió que rige el límite de Registrar (9.999.999.999). La pantalla muestra cualquier tarifa completa, sin cortarla; la spec se actualizó. Cuando se haga «Editar», debe usar el mismo límite.
- **Pruebas manuales**: propietarios 1010000002 a 1010000006; administrador 1010000001; contraseña `SeaShare2026!` (el administrador recibe 403 en este endpoint).
- **Reinicio del backend**: si la pantalla muestra siempre el error, o el listado del Administrador dice «No static resource», el backend en ejecución no tiene aún los endpoints nuevos; hay que copiar los archivos y reiniciarlo.
- **Administrador**: «Enviar a Mantenimiento» y «Eliminar» solo se muestran; sus flujos (motivo obligatorio, notificaciones, solicitar revisión) se hacen con las historias 3 a 5 del spec de mantenimiento. `maintenance_by` aún no se expone en el detalle.
