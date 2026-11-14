# Contrato: Asignar estado operativo (simulador de la cola de M2)

**Fecha**: 13 de noviembre de 2026 · **Especificación**: `asignar-estado-operativo-spec.md` · **Estado**: Implementado, pendiente de verificación

Hoy el aviso entra por HTTP; en la integración entrará por RabbitMQ con el mismo mensaje y el mismo caso de uso (`AssignOperationalStatusUseCase`).

## POST `/api/v1/internal/status-events` (sin autenticación)

```json
{
  "event_id": "evt-0001",
  "vessel_id": "7c1e0d52-3b9a-4f6e-8c11-5a2d9e4b7f10",
  "operational_status": "RESERVED",
  "occurred_at": "2026-11-13T10:00:00-05:00"
}
```

Cualquier otro campo (por ejemplo `amount`) se ignora.

| Campo | Regla |
|---|---|
| `event_id` | Obligatorio, hasta 100 caracteres, único por aviso |
| `vessel_id` | UUID de la embarcación |
| `operational_status` | `AVAILABLE`, `RESERVED`, `IN_NAVIGATION` o `NAVIGATION`, `MAINTENANCE_CLEANING` o `MAINTENANCE` |
| `occurred_at` | ISO 8601 con zona (`Z` o `-05:00`) |

**Respuesta 200**: `{"event_id":"evt-0001","outcome":"APPLIED"}`

| `outcome` | Significado |
|---|---|
| `APPLIED` | El estado cambió |
| `DUPLICATE` | Ese `event_id` ya se procesó; no cambia nada |
| `STALE` | El suceso es anterior al último cambio de estado de la embarcación; se descarta |
| `UNKNOWN_VESSEL` | No existe, fue eliminada o es un borrador; se registra en el log |
| `INVALID_TRANSITION` | Transición fuera del spec; se registra y no cambia nada |
| `NO_CHANGE` | La embarcación ya tenía ese estado |

**400**: aviso incompleto o con valores inválidos, con `errors[{field, message}]` (`event_id`, `vessel_id`, `operational_status`, `occurred_at`).

## Transiciones aplicadas

| Estado actual | Puede pasar a |
|---|---|
| Disponible | Reservado |
| Reservado | En Navegación, Mantenimiento, Disponible |
| En Navegación | Mantenimiento |
| Mantenimiento | Ninguno por aviso (lo termina el propietario) |

## Datos que guarda

| Tabla | Contenido |
|---|---|
| `vessel.status_changed_at` | Momento (UTC) del último cambio de estado |
| `processed_event` | `event_id` ya procesados (clave primaria) |
| `vessel_status_history` | Estado anterior y nuevo, momento, origen `AUTOMATIC` o `MANUAL` y `event_id` |

## Cómo probar en Postman (sin token)

1. Obtén un `boat_id` de una embarcación **Disponible** (login de propietario y `GET /api/v1/fleet/boats`). Para vigilar el estado usa `GET /api/v1/vessels/<ID>/operational-status`.
2. Todas las pruebas son `POST http://localhost:8081/api/v1/internal/status-events` con body raw JSON.

| # | Prueba | Cambia en el body | Esperado |
|---|---|---|---|
| 1 | Pago confirmado | `evt-1`, `RESERVED`, `2026-11-13T10:00:00-05:00` | `APPLIED`; el estado pasa a `RESERVED` |
| 2 | Duplicado | Repite exactamente la prueba 1 | `DUPLICATE`, sin cambios |
| 3 | Aviso viejo | `evt-2`, `AVAILABLE`, `2026-01-01T10:00:00Z` | `STALE`; sigue `RESERVED` |
| 4 | Check-in | `evt-3`, `IN_NAVIGATION`, hora posterior a la 1 | `APPLIED`; `NAVIGATION` |
| 5 | Fin del viaje | `evt-4`, `MAINTENANCE_CLEANING`, hora posterior | `APPLIED`; `MAINTENANCE` |
| 6 | El dueño la libera | Con token de propietario: `POST /api/v1/fleet/boats/<ID>/maintenance/finish` | 200; vuelve a `AVAILABLE` |
| 7 | Cancelación normal | Con la embarcación Disponible: `evt-5` `RESERVED` y luego `evt-6` `AVAILABLE` (horas crecientes) | `APPLIED` ambos |
| 8 | Transición inválida | Embarcación Disponible: `IN_NAVIGATION` | `INVALID_TRANSITION` |
| 9 | Embarcación inexistente | `vessel_id` UUID cualquiera | `UNKNOWN_VESSEL` |
| 10 | Datos de dinero | Agrega `"amount": 50000` a un aviso válido | Se aplica normal, el monto se ignora |
| 11 | Aviso incompleto | Body `{}` | 400 con los cuatro campos |
| 12 | Estado inválido | `"operational_status": "FLYING"` | 400 en `operational_status` |

Para ver el historial, en MySQL: `SELECT * FROM vessel_status_history ORDER BY changed_at;`

## Pendiente para la integración

Cuando se integren los módulos: consumidor RabbitMQ (cola, exchange y formato del mensaje a acordar con M2; el contenedor está en la guía de `seashare-rabbit`), reintentos y cola de mensajes fallidos.
