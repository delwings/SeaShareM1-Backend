# Contrato REST: Eliminar embarcación (Módulo 1)

> Describe la API tal como quedó implementada. Base: `/api/v1`. Cada endpoint exige `Authorization: Bearer <token>`: el de propietario, rol **OWNER**; el de administrador, rol **ADMIN**. Convenciones y formato de error: las del contrato de Registrar Embarcación (RFC 9457 con `status`, `title`, `detail` y `correlation_id`).

**Última actualización**: 11 de noviembre de 2026.
**Alcance**: Propietario y Administrador. La notificación al propietario se crea y se guarda; su pantalla (campana) todavía no existe.

## Resumen

| Método | Ruta | Para qué | Éxito |
|---|---|---|---|
| DELETE | `/fleet/boats/{boatId}` | Eliminar (lógicamente) una embarcación registrada del propietario | 204 |
| DELETE | `/admin/fleet/boats/{boatId}` | Eliminar (lógicamente) cualquier embarcación registrada, como administrador | 204 |

---

## DELETE `/fleet/boats/{boatId}`

Marca la embarcación como eliminada. No lleva cuerpo ni en la solicitud ni en la respuesta.

**Parámetros de ruta**

| Parámetro | Tipo | Regla |
|---|---|---|
| `boatId` | UUID | Identificador de la embarcación |

**Reglas** (en este orden, deteniéndose en la primera que falle)

1. La sesión es válida y el rol es OWNER.
2. La embarcación existe, no está eliminada, no es borrador y es del propietario.
3. Su estado es `AVAILABLE` o `MAINTENANCE`.
4. Se marca como eliminada, con la fecha (UTC) y el usuario que eliminó, en una sola operación atómica.

**Efectos**: la embarcación deja de aparecer en la lista y de poder consultarse; su matrícula legal queda libre para un registro nuevo; el registro, la foto y los servicios se conservan en la base de datos.

**204**: eliminada. Sin cuerpo.

**Errores**

| Estado | Cuándo | Mensaje |
|---|---|---|
| 400 | `boatId` no es un UUID válido | Mensaje estándar de Spring MVC con la forma común de error |
| 401 | Sin token, token inválido o vencido | Autenticación requerida o token inválido. |
| 403 | Rol distinto de OWNER (por ejemplo, el administrador) | No tienes permiso para realizar esta acción. |
| 404 | No existe, ya fue eliminada, es borrador o es de otro propietario | Embarcación no encontrada. |
| 409 | El estado actual no permite eliminar (`RESERVED` o `NAVIGATION`) | La embarcación no se puede eliminar en su estado actual. |

El 404 es idéntico para los cuatro casos, para no revelar si el identificador existe.

**Cuerpo del 409**

```json
{
  "status": 409,
  "title": "Conflicto",
  "detail": "La embarcación no se puede eliminar en su estado actual.",
  "correlation_id": "b1f0c0de-...",
  "current_status": "RESERVED"
}
```

`current_status` toma uno de `AVAILABLE`, `RESERVED`, `NAVIGATION`, `MAINTENANCE`; el frontend lo muestra con los nombres del diccionario único («Reservado», «En Navegación»...).

**Idempotencia**: repetir la solicitud sobre una embarcación ya eliminada responde 404 y no cambia la fecha ni el autor guardados.

**Comportamiento del frontend ante cada respuesta**

| Respuesta | Pantalla |
|---|---|
| 204 | Modal «¡Embarcación eliminada!»; «Aceptar» lleva a «Mis Embarcaciones Registradas» |
| 404 | Modal «Esta embarcación ya no existe»; «Aceptar» lleva a la lista |
| 409 | Modal «No es posible eliminar la embarcación» con el estado nuevo y «Recargar página» |
| Fallo de red, 5xx o más de 15 s | Modal «No se pudo eliminar la embarcación» con «Cerrar» y «Reintentar» |
| 401 | Redirección al inicio de sesión |

---

## DELETE `/admin/fleet/boats/{boatId}`

Igual que el endpoint del propietario, con estas diferencias. **Lleva cuerpo** (`Content-Type: application/json`):

```json
{ "reason": "Las fotos no corresponden a la embarcación." }
```

`reason` es obligatorio: se recorta y debe tener entre 1 y 300 caracteres. Si falta, está vacío o es más largo, responde **400** con `errors[{field:"reason", message}]` y no cambia nada (se valida antes de buscar la embarcación).

**Reglas** (en este orden)

1. Sesión válida y rol ADMIN.
2. La embarcación existe, no está eliminada y no es borrador; puede ser de **cualquier propietario**.
3. Su estado es `AVAILABLE` o `MAINTENANCE`.
4. Se marca como eliminada (fecha UTC y el administrador como autor) en una operación atómica.
5. Se crea la notificación para el propietario dueño.

**Notificación creada**: «Tu embarcación [nombre] ([matrícula]) fue eliminada por un administrador el 3 de noviembre de 2026. Motivo: [motivo]» (fecha en hora de Colombia, mes en minúscula). El propietario la lee en la campana (`notificaciones.md`).

**Errores**

| Estado | Cuándo |
|---|---|
| 400 | Falta el motivo o supera los 300 caracteres |
| 401 | Sin token, token inválido o vencido |
| 403 | Rol distinto de ADMIN (el propietario no puede usar esta ruta) |
| 404 | No existe, ya fue eliminada o es borrador |
| 409 | `RESERVED` o `NAVIGATION`; mismo cuerpo con `current_status` |

**Comportamiento del frontend**: los mismos modales del propietario, con el campo «Motivo de la eliminación» en la confirmación (sin motivo no se envía y se muestra «Escribe el motivo de la eliminación.»); «Aceptar» (éxito o «ya no existe») lleva a «Control General de Flota Fluvial» sin filtros.

---

## Notas de implementación

| Tema | Detalle |
|---|---|
| Atomicidad | `UPDATE vessel SET deleted = TRUE, deleted_at = ?, deleted_by = ? WHERE id = ? AND deleted = FALSE AND status IN ('AVAILABLE','MAINTENANCE')`; si afecta 0 filas, el servicio relee para responder 404 o 409 |
| Matrícula | Índice único sobre la columna generada `active_legal_registration` (NULL cuando `deleted = TRUE`); el choque de matrícula activa sigue siendo un 409 con `errors[].field = legal_registration` |
| Orden de rutas | `DELETE /{boatId}` convive con `DELETE /draft`: Spring da prioridad a la ruta literal |
| Plazo del cliente | El frontend cancela la solicitud a los 15 segundos; el servidor no tiene un plazo propio |
| Notificación | Solo la crea la eliminación del administrador: una fila en `notification` (`type = VESSEL_DELETED`, `recipient_id` = propietario, `vessel_id`, `message`, `is_read = FALSE`). Se crea después del `UPDATE` atómico, así que una segunda solicitud (404) no genera otra |
| Fallo del aviso | Si guardar la notificación falla, se registra en el log y la eliminación se conserva (204) |
| Pendiente | Pantalla de notificaciones (campana, lista, marcar como leída): spec aparte |
