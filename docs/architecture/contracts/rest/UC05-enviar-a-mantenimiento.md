# Contrato REST: Enviar a Mantenimiento / Terminar mantenimiento (Propietario y Administrador)

**Fecha**: 10 de noviembre de 2026
**Especificación**: `enviar-mantenimiento-spec.md`
**Rol**: solo Propietario (`ROLE_OWNER`). Las rutas del Administrador se definirán aparte.

## Resumen

| Método | Ruta | Cambio | Éxito |
|---|---|---|---|
| `POST` | `/api/v1/fleet/boats/{boatId}/maintenance/start` | Disponible → Mantenimiento (`maintenance_by = OWNER`) | `200` + `CompletedBoatResponse` |
| `POST` | `/api/v1/fleet/boats/{boatId}/maintenance/finish` | Mantenimiento → Disponible (`maintenance_by = NULL`) | `200` + `CompletedBoatResponse` |

Autenticación: `Authorization: Bearer <JWT>`. Sin cuerpo (no se pide motivo). `boatId`: UUID.

## Reglas, en orden (se detiene en la primera que falla)

1. Sesión válida → si no, `401`.
2. Rol OWNER → si no, `403`.
3. Existe, es del propietario, no está eliminada ni es borrador → si no, `404` (mismo cuerpo siempre).
4. Estado y origen:

| Acción | Estado actual | Resultado |
|---|---|---|
| start | `AVAILABLE` | Cambia → `200` |
| start | `MAINTENANCE` (cualquier origen) | `200` sin cambios (idempotente; no cambia el origen) |
| start | `RESERVED`, `NAVIGATION` | `409` |
| finish | `MAINTENANCE`, origen `OWNER` o vacío | Cambia → `200` |
| finish | `AVAILABLE` | `200` sin cambios (idempotente) |
| finish | `MAINTENANCE`, origen `ADMIN` | `409` con `maintenance_by: "ADMIN"` |
| finish | `RESERVED`, `NAVIGATION` | `409` |

5. Cambio atómico: `UPDATE` condicional (id, dueño, no eliminada, estado esperado y, en finish, origen distinto de ADMIN). Si afecta 0 filas porque algo cambió en medio, se relee y se responde según el estado nuevo (`200` si ya quedó como se pidió, `409` si no, `404` si fue eliminada).

## Rutas del administrador

| Método | Ruta | Cambio | Éxito |
|---|---|---|---|
| `POST` | `/api/v1/admin/fleet/boats/{boatId}/maintenance/start` | Disponible → Mantenimiento (`maintenance_by = ADMIN`), con motivo | `200` + `CompletedBoatResponse` |
| `POST` | `/api/v1/admin/fleet/boats/{boatId}/maintenance/finish` | Mantenimiento → Disponible, solo si lo inició el administrador | `200` + `CompletedBoatResponse` |

Cualquier propietario; solo rol ADMIN. Orden: motivo válido (400), existe y no está eliminada ni es borrador (404), estado (409), cambio atómico y aviso.

**start** recibe `{ "reason": "Revisión obligatoria del motor" }` (obligatorio, se recorta, 1 a 300 caracteres; si no cumple: `400` con `errors[{field:"reason", message}]`).

| Acción | Estado actual | Resultado |
|---|---|---|
| start | `AVAILABLE` | Cambia → `200` y crea el aviso `MAINTENANCE_NOTICE` |
| start | `MAINTENANCE` (cualquier origen) | `200` sin cambios ni aviso nuevo |
| start | `RESERVED`, `NAVIGATION` | `409` |
| finish | `MAINTENANCE`, origen `ADMIN` | Cambia → `200` y crea el aviso `VESSEL_AVAILABLE` |
| finish | `AVAILABLE` | `200` sin cambios ni aviso |
| finish | `MAINTENANCE`, origen `OWNER` o vacío | `409` con `maintenance_by` |
| finish | `RESERVED`, `NAVIGATION` | `409` |

**Avisos creados** (tabla `notification`, destinatario = propietario de la embarcación):

- `MAINTENANCE_NOTICE`: «Tu embarcación [nombre] ([matrícula]) fue enviada a mantenimiento por un administrador el 3 de noviembre de 2026. Motivo: [motivo]»
- `VESSEL_AVAILABLE`: «Tu embarcación [nombre] ([matrícula]) fue habilitada por un administrador el 3 de noviembre de 2026. Ya puede recibir reservas.»

La fecha va en hora de Colombia. Si guardar el aviso falla, el cambio de estado se conserva.

---

## Respuesta 200

Mismo cuerpo que `GET /api/v1/fleet/boats/{boatId}` (`CompletedBoatResponse`: `boat_id`, `name`, `legal_registration`, `type`, `max_capacity`, `status`, `photo_url`, `base_rate`, `port`, `services`, `registered_at`), con el `status` ya actualizado (`MAINTENANCE` o `AVAILABLE`).

## Errores (RFC 9457, con `correlation_id`)

| Código | Cuándo |
|---|---|
| `401` | Sin sesión o token vencido |
| `403` | Rol distinto de OWNER (en las rutas del administrador, distinto de ADMIN) |
| `404` | Embarcación inexistente, ajena, eliminada o borrador |
| `409` | Estado u origen no permiten el cambio |
| `500` | Error inesperado |

Ejemplo `409`:

```json
{
  "status": 409,
  "title": "Conflict",
  "detail": "La embarcación fue suspendida por el administrador; solo él puede habilitarla.",
  "current_status": "MAINTENANCE",
  "maintenance_by": "ADMIN",
  "correlation_id": "..."
}
```

`current_status` va siempre en el 409; `maintenance_by` solo si está en mantenimiento con origen.

## Solicitar revisión (Propietario)

`POST /api/v1/fleet/boats/{boatId}/maintenance/request-review` — sin cuerpo; rol OWNER.

Crea un aviso `REVIEW_REQUEST` para **cada administrador** (destinatario = administrador, `vessel_id` = la embarcación): «El propietario solicitó la revisión de la embarcación [nombre] ([matrícula]) el [fecha].» No cambia el estado. **Solo se puede pedir una vez por mantenimiento**: la primera vez se guarda `vessel.review_requested = TRUE` (UPDATE atómico); las siguientes responden `204` sin crear avisos. La marca vuelve a `FALSE` cuando el administrador envía o termina un mantenimiento (y cuando el propietario inicia o termina el suyo). `CompletedBoatResponse` trae `review_requested` (boolean).

| Estado | Cuándo |
|---|---|
| `204` | Avisos creados (o la revisión ya estaba solicitada: no se crea nada) |
| `401` / `403` | Sin sesión / rol distinto de OWNER |
| `404` | Inexistente, ajena, eliminada o borrador |
| `409` | No está en mantenimiento por el administrador (disponible, reservada, o mantenimiento propio/del sistema) |

Frontend: botón «Solicitar revisión» (propietario, `status = MAINTENANCE` y `maintenance_by = ADMIN`) → modal «¿Enviar a revisión?» / «Se enviará una notificación al administrador.» → «Solicitud enviada»; el botón queda **deshabilitado** (cursor de bloqueo, sin tooltip) mientras `review_requested = true`, también al recargar o abrir otra pestaña; 404 → «Esta embarcación ya no existe»; 409 → «No es posible solicitar la revisión» (Recargar página).

---

## Cambio en la respuesta (`CompletedBoatResponse`)

Se agrega `maintenance_by`: `"OWNER"`, `"ADMIN"` o `null` (nulo si no está en mantenimiento o el origen es el sistema). Sirve para deshabilitar «Terminar mantenimiento» a quien no lo inició.

---

## Comportamiento del frontend

| Respuesta | Pantalla |
|---|---|
| `200` | Cierra el modal y redibuja el detalle con la embarcación devuelta (sin modal de éxito) |
| `401` | Cierra modales; el cliente HTTP lleva al inicio de sesión |
| `404` | Modal «Esta embarcación ya no existe» → Mis Embarcaciones |
| `409` (ocupada) | Modal «No es posible …» con el estado actual → Recargar página |
| `409` (`maintenance_by = ADMIN`) | Modal «Embarcación suspendida por el administrador» → Aceptar (recarga) |
| `409` del administrador (`maintenance_by = OWNER`) | Modal «Mantenimiento iniciado por el propietario» → Aceptar (recarga) |
| `404` del administrador | Modal «Esta embarcación ya no existe» → «Control General de Flota Fluvial» |
| red, `5xx`, timeout 15 s | Modal técnico con Cerrar / Reintentar |

Botón de mantenimiento: deshabilitado en `RESERVED` y `NAVIGATION`. Una sola solicitud por confirmación.

## Botón «Terminar mantenimiento» según quién lo inició

| Origen | Propietario | Administrador |
|---|---|---|
| `OWNER` | Habilitado | Deshabilitado (cursor de bloqueo) |
| vacío (sistema) | Habilitado | Deshabilitado |
| `ADMIN` | «Solicitar revisión» (habilitado) | Habilitado («Terminar Mantenimiento») |

El modal de «Enviar a Mantenimiento» del administrador agrega «Se enviará una notificación al propietario.» y el campo «Motivo del mantenimiento».

---

## Notas

- Sin migración: usa la columna `maintenance_by` (`OWNER`, `ADMIN`, NULL) de V3.
- `maintenance_by` no se expone todavía en `GET`; se agregará con la parte del Administrador.
- No se crean notificaciones en la parte del Propietario.
