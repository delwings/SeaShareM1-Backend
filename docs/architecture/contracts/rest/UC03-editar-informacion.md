# Contrato REST: Editar información de embarcación

**Fecha**: 9 de noviembre de 2026
**Especificación**: `editar-embarcacion-spec.md`
**Rol**: solo Propietario (`ROLE_OWNER`)

## Resumen

| Método | Ruta | Content-Type | Éxito |
|---|---|---|---|
| `PUT` | `/api/v1/fleet/boats/{boatId}` | `multipart/form-data` | `200` con `CompletedBoatResponse` |

Autenticación: `Authorization: Bearer <JWT>`.

## Parámetros

Ruta: `boatId` (UUID de la embarcación).

Campos del formulario (todos se envían como texto salvo `photo`):

| Campo | Tipo | Regla |
|---|---|---|
| `name` | texto | Obligatorio. Se recortan y colapsan espacios. 3–50 caracteres |
| `max_capacity` | texto numérico | Obligatorio. Entero ≥ 1 y ≤ tope del tipo (Lancha 10, Yate 20, Catamarán 30, Velero 12). El tipo no se edita |
| `port_name` | texto | Obligatorio. Texto libre |
| `latitude` | texto decimal | Obligatorio. −90 a 90 |
| `longitude` | texto decimal | Obligatorio. −180 a 180 |
| `base_rate` | texto numérico | Obligatorio. Entero COP, 1 a 9.999.999.999 |
| `life_jackets`, `cooler_with_ice`, `sound_system`, `fishing_gear`, `diving_gear` | boolean | Opcionales. Ver «Servicios» |
| `photo` | archivo | Opcional. JPG/PNG, máx. 5 MB. Ver «Fotografía» |

No se aceptan `type`, `legal_registration` ni `status`: no son editables y se ignoran.

## Reglas, en orden estricto (se detiene en la primera que falla)

1. **Sesión**: JWT válido → si no, `401`.
2. **Rol**: debe ser OWNER → si no, `403`.
3. **Existencia y pertenencia**: la embarcación existe, es del propietario, no está eliminada y no es borrador → si no, `404` (misma respuesta para todos los casos, anti-enumeración).
4. **Estado**: solo `AVAILABLE` o `MAINTENANCE` → si no, `409` con `current_status`.
5. **Validación**: se evalúan todos los campos y se devuelven todos los errores juntos → `400`.
6. **Persistencia**: una sola transacción (puerto + embarcación) con `UPDATE` condicional atómico que vuelve a exigir dueño, no eliminada y estado editable. Si afecta 0 filas (el estado cambió en medio) → `409`.

## Semántica

| Tema | Comportamiento |
|---|---|
| Fotografía | Ausente → se conserva la guardada. Presente → se sube a Cloudinary y reemplaza. No hay forma de dejar la embarcación sin foto |
| Servicios | Ninguno de los cinco campos presente → se conservan los guardados. Alguno presente → los ausentes se toman como `false` |
| Puerto | Se busca por nombre sin distinguir mayúsculas. Si existe, se **actualizan sus coordenadas** (compartido: afecta a todas las embarcaciones que lo usan). Si no existe, se crea uno nuevo y el anterior no se toca |
| Concurrencia | Sin control de versión: prevalece el último guardado |
| Idempotencia | Repetir la misma solicitud devuelve `200` con el mismo resultado |

## Respuesta 200

`CompletedBoatResponse`:

```json
{
  "boat_id": "uuid",
  "name": "Brisa del Mar",
  "legal_registration": "CP-12345",
  "type": "MOTORBOAT",
  "max_capacity": 8,
  "status": "AVAILABLE",
  "photo_url": "https://res.cloudinary.com/...",
  "base_rate": "450000",
  "port": { "name": "Muelle La Bodeguita", "latitude": "10.42", "longitude": "-75.55" },
  "services": {
    "life_jackets": true, "cooler_with_ice": false, "sound_system": true,
    "fishing_gear": false, "diving_gear": false
  },
  "registered_at": "2026-10-01T15:00:00Z"
}
```

## Errores (RFC 9457, con `correlation_id`)

| Código | Cuándo | Notas |
|---|---|---|
| `400` | Validación fallida | `errors: [{field, message}]` con todos los campos inválidos. Campos vacíos: «Este campo es obligatorio» |
| `401` | Sin sesión o token vencido | |
| `403` | Rol distinto de OWNER | |
| `404` | Embarcación inexistente, ajena, eliminada o borrador | Mismo cuerpo en todos los casos |
| `409` | Estado `RESERVED` o `IN_NAVIGATION` (o cambió durante el guardado) | Incluye `current_status` |
| `502` | Falla al subir la imagen | No se modifica nada |
| `500` | Error inesperado | |

Ejemplo `409`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "La embarcación no se puede editar en su estado actual.",
  "current_status": "RESERVED",
  "correlation_id": "..."
}
```

Ejemplo `400`:

```json
{
  "status": 400,
  "title": "Bad Request",
  "errors": [
    { "field": "name", "message": "Este campo es obligatorio" },
    { "field": "base_rate", "message": "La tarifa debe ser un número entero mayor a 0" }
  ],
  "correlation_id": "..."
}
```

## Comportamiento del frontend

| Respuesta | Acción en pantalla |
|---|---|
| `200` | Modal de éxito → vuelve a «Ver Detalles» |
| `400` | Errores bajo cada campo, foco en el primero |
| `403` | Modal «No tienes permiso para editar esta embarcación.» → Volver a Ver Detalles |
| `404` | Modal «Esta embarcación ya no existe.» → Volver a la lista |
| `409` | Modal «No se pudieron guardar los cambios» con el estado actual → Recargar página |
| `5xx`, red, timeout 15 s | Modal técnico con Cerrar / Reintentar (conserva lo escrito) |

Al cargar la pantalla se usa `GET /api/v1/fleet/boats/{boatId}`; 404/400/403 → «Esta embarcación no existe o no tienes permiso para editarla.»; estado no editable → pantalla de bloqueo.

## Notas de implementación

- No hay migración de base de datos (sigue en V7).
- La imagen se sube **antes** de la transacción; si el guardado falla después, puede quedar una imagen huérfana en Cloudinary (pendiente de limpieza).
- Si dos propietarios guardan el mismo nombre de puerto casi a la vez, se reintenta una vez ante la violación de unicidad.
