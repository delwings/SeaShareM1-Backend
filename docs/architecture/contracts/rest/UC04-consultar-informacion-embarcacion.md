# Contrato REST: Consultar detalles de embarcación (Módulo 1)

> Describe la API tal como quedó implementada. Base: `/api/v1`. Todos los endpoints exigen `Authorization: Bearer <token>`. Los de `/fleet` son del rol **OWNER** (el administrador recibe 403) y cada propietario solo consulta sus propias embarcaciones; los de `/admin/fleet` son del rol **ADMIN** (el propietario recibe 403) y ven todas las embarcaciones registradas. Las convenciones (snake_case, decimales como texto, fechas UTC, formato de error RFC 9457) son las del contrato de Registrar Embarcación.

**Última actualización**: 13 de noviembre de 2026.
**Alcance**: vista del Propietario y vista del Administrador (listado y detalle, solo lectura). Desde el 13 de noviembre también las dos consultas internas del Módulo 2 (información y estado operativo), al final de este documento.

## Resumen

| Método | Ruta | Para qué | Éxito |
|---|---|---|---|
| GET | `/fleet/boats/{boatId}` | Detalles completos de una embarcación registrada del propietario | 200 |
| GET | `/admin/fleet/boats/{boatId}` | Detalles completos de cualquier embarcación registrada (Administrador) | 200 |
| GET | `/admin/fleet/boats` | Listado global con filtros (Administrador) | 200 |
| GET | `/admin/fleet/owners` | Propietarios con embarcaciones, para el filtro (Administrador) | 200 |

---

## GET `/fleet/boats/{boatId}`

Devuelve la información completa de una embarcación **registrada** del propietario autenticado. Consulta siempre la base de datos: no hay caché.

**Parámetros de ruta**

| Parámetro | Tipo | Regla |
|---|---|---|
| `boatId` | UUID | Identificador de la embarcación (`boat_id` de la lista) |

**200**

```json
{
  "boat_id": "7c1e0d52-3b9a-4f6e-8c11-5a2d9e4b7f10",
  "name": "Río Magdalena II",
  "legal_registration": "CP-05-1042-A",
  "type": "MOTORBOAT",
  "max_capacity": 8,
  "status": "AVAILABLE",
  "photo_url": "https://res.cloudinary.com/.../seashare/boats/xyz.png",
  "base_rate": "180000.00",
  "port": { "name": "Marina Magdalena", "latitude": "11.2408", "longitude": "-74.1990" },
  "services": {
    "life_jackets": true,
    "cooler_with_ice": false,
    "sound_system": false,
    "fishing_gear": false,
    "diving_gear": false
  },
  "registered_at": "2026-11-05T14:30:15Z"
}
```

Es la misma forma que devuelve `POST /fleet/boats/{boatId}/registration`.

| Campo | Notas |
|---|---|
| `type` | `MOTORBOAT` (Lancha), `YACHT` (Yate), `CATAMARAN` (Catamarán), `SAILBOAT` (Velero) |
| `status` | `AVAILABLE`, `RESERVED`, `NAVIGATION`, `MAINTENANCE` (el borrador nunca se devuelve). La pantalla muestra `MAINTENANCE` como «Mantenimiento» |
| `base_rate` | Texto con decimales; el frontend lo muestra como entero con puntos de miles (`$ 180.000`) |
| `port.latitude`, `port.longitude` | Texto con signo, hasta 4 decimales; el frontend los muestra siempre con 4 |
| `services` | Solo los cinco servicios adicionales. Capitán y Combustible van siempre incluidos y no se envían |
| `photo_url` | Dirección de la imagen en Cloudinary; puede faltar o fallar al cargar, el frontend usa una imagen por defecto |
| `port`, `base_rate` | Si faltaran en la base de datos, el servidor responde `null` en lugar de fallar; el frontend muestra «—» |

**Errores**

| Estado | Cuándo | Mensaje |
|---|---|---|
| 400 | `boatId` no es un UUID válido | Mensaje estándar de Spring MVC con la forma común de error (`correlation_id`) |
| 401 | Sin token, token inválido o vencido | Autenticación requerida o token inválido. |
| 403 | Rol distinto de OWNER (por ejemplo, el administrador) | No tienes permiso para realizar esta acción. |
| 404 | La embarcación no existe, es de otro propietario, fue eliminada o está en borrador | Embarcación no encontrada. |

El 404 es idéntico para los cuatro casos, para no revelar si un identificador existe (FR-013, NFR-001).

**Comportamiento del frontend ante cada respuesta**

| Respuesta | Pantalla |
|---|---|
| 200 | Ficha completa de la embarcación |
| 400, 404 | «No se pudo cargar la información de la embarcación» con «Volver a Mis Embarcaciones» (sin «Reintentar») |
| 401 | Redirección al inicio de sesión, sin mensaje de error |
| 5xx o fallo de red | El mismo mensaje con «Volver» y «Reintentar» |

Desde esta pantalla el propietario también usa `PUT /fleet/boats/{boatId}` (Editar), `DELETE /fleet/boats/{boatId}` (Eliminar) y `POST /fleet/boats/{boatId}/maintenance/start|finish` (Enviar a Mantenimiento / Terminar mantenimiento); cada uno tiene su propio contrato.

---

## Vista del Administrador (solo lectura)

Todos exigen rol **ADMIN**; un propietario recibe 403 «No tienes permiso para realizar esta acción.». Sin token o con token vencido: 401. Consultan siempre la base de datos y nunca devuelven borradores ni embarcaciones eliminadas.

### GET `/admin/fleet/boats/{boatId}`

Detalles de cualquier embarcación registrada, sin importar su propietario. La respuesta es **idéntica** a la de `GET /fleet/boats/{boatId}` (`CompletedBoatResponse`); no incluye datos del propietario.

| Estado | Cuándo |
|---|---|
| 200 | Embarcación registrada |
| 404 | No existe, fue eliminada o es un borrador («Embarcación no encontrada.») |

### GET `/admin/fleet/boats`

Listado «Control General de Flota Fluvial». Orden: registro más reciente primero.

| Parámetro (query) | Tipo | Regla |
|---|---|---|
| `owner_id` | UUID | Opcional. Solo las embarcaciones de ese propietario |
| `status` | texto | Opcional. `AVAILABLE`, `RESERVED`, `MAINTENANCE` o `NAVIGATION` (no se acepta `DRAFT`) |
| `q` | texto | Opcional, hasta 100 caracteres. Coincide con parte del nombre del propietario o de su cédula. **La pantalla ya no lo usa** (la barra de búsqueda usa `GET /admin/owners`, ver su contrato); sigue disponible en el servidor |

Los filtros excluyen las embarcaciones que no cumplen y se combinan (todos deben cumplirse). «Volver» desde el detalle regresa al listado sin filtros.

```json
{
  "boats": [
    {
      "boat_id": "7c1e0d52-3b9a-4f6e-8c11-5a2d9e4b7f10",
      "name": "Río Magdalena I",
      "owner": { "owner_id": "2e38dc8e-...", "full_name": "Fernando Fernández", "document_number": "1010000002" },
      "legal_registration": "CP-05-1042-A",
      "type": "MOTORBOAT",
      "status": "AVAILABLE"
    }
  ]
}
```

| Estado | Cuándo |
|---|---|
| 200 | Siempre que los filtros sean válidos (la lista puede ir vacía) |
| 400 | `owner_id` no es un UUID, `status` no es válido o `q` supera 100 caracteres. Trae `errors: [{field, message}]` («El propietario no es válido», «El estado no es válido») |

### GET `/admin/fleet/owners`

Propietarios que tienen al menos una embarcación registrada, ordenados por nombre. Alimenta el filtro «Filtrar por Propietario».

```json
{ "owners": [ { "owner_id": "2e38dc8e-...", "full_name": "Fernando Fernández", "document_number": "1010000002" } ] }
```

**Comportamiento del frontend (Administrador)**

| Respuesta | Pantalla |
|---|---|
| Listado 200 | Tabla con Embarcación, Propietario, Matrícula, Tipo, Estado operativo y «Ver Detalles» |
| Listado vacío | «No se encontraron embarcaciones con esos filtros» (con filtros) o «Aún no hay embarcaciones registradas en la plataforma» |
| Listado 5xx o red | Mensaje con «Reintentar» |
| Propietarios fallan | El listado sigue funcionando; el filtro queda sin opciones |
| Detalle 200 | Misma ficha del propietario, sin «Editar», con el botón de mantenimiento y «Eliminar» (solo visibles por ahora) |
| Detalle 400, 404 | «No se pudo cargar la información de la embarcación» con «Volver» al listado (sin «Reintentar») |
| Detalle 5xx o red | El mismo mensaje con «Volver» y «Reintentar» |
| 401 | Redirección al inicio de sesión |

La barra «Buscar propietario por nombre o cédula...» no consulta este listado ni filtra la tabla: abre la ventana de resultados de «Consultar información de propietario» (contrato `consultar-informacion-propietario.md`).

---

## Notas de implementación

| Tema | Detalle |
|---|---|
| Autorización | `@PreAuthorize("hasRole('OWNER')")` en `BoatController` (el filtrado por propietario y por estado lo hace `GetBoatDetailService`) y `@PreAuthorize("hasRole('ADMIN')")` en `AdminFleetController` (casos de uso en `AdminFleetService`) |
| Borradores | `GetBoatDetailService` descarta las embarcaciones en borrador; el borrador se consulta solo con `GET /fleet/boats/draft` |
| Orden de rutas | `/{boatId}` convive con `/draft` y `/legal-registration-availability`: Spring da prioridad a las rutas literales |
| Sin cambios de base de datos | Solo lectura; no hay migraciones nuevas |
| Filtrado del listado | Se hace en memoria en `AdminFleetService` (sin paginación); suficiente para el volumen actual |
| Pendiente | Catálogo en lote del Módulo 2 (`GET /vessels`), por definir con ellos |

---

## Consultas internas del Módulo 2 (13 de noviembre de 2026)

Rutas y nombres de campo del contrato del Módulo 2. **Sin autenticación** (decisión del proyecto; si envían `X-Internal-Service-Token`, se ignora). Solo lectura, sin caché. No chocan con las rutas del Módulo 1 (`/auth`, `/fleet`, `/admin`, `/notifications`, `/boats/base-rates`).

| Método | Ruta | Para qué | Éxito |
|---|---|---|---|
| GET | `/vessels/{vessel_id}` | Información completa (pasos 1 y 2) con foto y estado, sin tarifa | 200 |
| GET | `/vessels/{vessel_id}/operational-status` | Solo el estado operativo | 200 |

### GET `/vessels/{vessel_id}`

```json
{
  "vessel_id": "7c1e0d52-3b9a-4f6e-8c11-5a2d9e4b7f10",
  "name": "Río Magdalena II",
  "registration_number": "CP-05-1042-A",
  "type": "MOTORBOAT",
  "max_capacity": 8,
  "operational_status": "AVAILABLE",
  "photo_url": "https://res.cloudinary.com/.../seashare/boats/xyz.png",
  "captain_included": true,
  "port": { "name": "Marina Magdalena", "latitude": 11.2408, "longitude": -74.199, "timezone": "America/Bogota" },
  "amenities": ["Capitán", "Combustible", "Chalecos salvavidas"],
  "owner": { "owner_id": "2e38dc8e-...", "validated_name": "Fernando Fernández" }
}
```

`operational_status` y `photo_url` van en esta ficha además de la consulta de solo estado. La tarifa base no se incluye (la cotiza el Módulo 3). `latitude` y `longitude` son números. `amenities` trae siempre Capitán y Combustible más los adicionales elegidos. No se envían `length_feet`, `cabin_count` ni `owner.verified` (no existen en el modelo).

### GET `/vessels/{vessel_id}/operational-status`

```json
{ "vessel_id": "7c1e0d52-...", "operational_status": "AVAILABLE", "queried_at": "2026-11-13T12:05:00-05:00" }
```

`operational_status`: `AVAILABLE`, `RESERVED`, `NAVIGATION`, `MAINTENANCE`. `queried_at` en hora de Colombia (-05:00).

### Errores (ambas)

| Estado | Cuándo |
|---|---|
| 400 | `vessel_id` no es un UUID |
| 404 | No existe, fue eliminada o es un borrador (mismo mensaje en los tres casos) |
| 500 | Falla técnica, sin datos inventados |

### Puntos para avisar al Módulo 2

| # | Tema | Situación |
|---|---|---|
| 1 | Valores de estado | Módulo 1 responde `NAVIGATION` y `MAINTENANCE`; ellos esperan `IN_NAVIGATION` y `MAINTENANCE_CLEANING` |
| 0 | Ficha | Incluye foto y estado; no la tarifa |
| 2 | Campos no disponibles | `length_feet`, `cabin_count`, `owner.verified` no existen |
| 3 | `captain_included` y `timezone` | Valores fijos: `true` y `America/Bogota` |
| 4 | Servicios | Solo los 2 base y los 5 adicionales del Módulo 1 (los del ejemplo, como «Aire Acondicionado», no existen) |
| 5 | Autenticación | Sin token entre módulos |
| 6 | Catálogo en lote | Pendiente; la ruta del ejemplo (`/embarcaciones`) no coincide con la tabla (`/vessels`) |

### Cómo probar en Postman (sin token)

| Prueba | Petición | Esperado |
|---|---|---|
| Información | `GET http://localhost:8081/api/v1/vessels/<id>` | 200 con la ficha |
| Estado | `GET http://localhost:8081/api/v1/vessels/<id>/operational-status` | 200 `AVAILABLE` |
| Cambia el estado | Envía la embarcación a mantenimiento y repite | 200 `MAINTENANCE` |
| Inexistente, eliminada o borrador | `GET .../vessels/<otro-uuid>` | 404 |
| ID inválido | `GET .../vessels/abc` | 400 |
