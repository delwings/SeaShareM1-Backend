# Contrato REST: Notificaciones del Propietario

> Base: `/api/v1`. Todos los endpoints exigen `Authorization: Bearer <token>` y rol **OWNER** (el administrador recibe 403). Formato de error: RFC 9457 con `correlation_id`.

**Última actualización**: 11 de noviembre de 2026.

| Método | Ruta | Para qué | Éxito |
|---|---|---|---|
| GET | `/notifications` | Las 50 notificaciones más recientes y el total sin leer | 200 |
| PATCH | `/notifications/{notificationId}/read` | Marcar una como leída | 204 |
| PATCH | `/notifications/read-all` | Marcar todas como leídas | 204 |

## GET `/notifications`

```json
{
  "notifications": [
    {
      "notification_id": "uuid",
      "type": "VESSEL_DELETED",
      "message": "Tu embarcación Río Magdalena I (CP-05-1042-A) fue eliminada por un administrador el 3 de noviembre de 2026. Motivo: Las fotos no corresponden a la embarcación.",
      "read": false,
      "created_at": "2026-11-04T02:15:00Z"
    }
  ],
  "unread_count": 1
}
```

Orden: de la más nueva a la más antigua. `created_at` va en UTC; el frontend lo muestra en hora de Colombia. `unread_count` cuenta todas las no leídas, aunque la lista tenga un máximo de 50.

## PATCH `/notifications/{notificationId}/read`

Sin cuerpo. Es idempotente: repetirlo responde 204.

| Estado | Cuándo |
|---|---|
| 204 | Marcada (o ya lo estaba) |
| 401 | Sin token o inválido |
| 403 | Rol distinto de OWNER |
| 404 | No existe o es de otro usuario («Notificación no encontrada.») |

## PATCH `/notifications/read-all`

Sin cuerpo; marca todas las del usuario. 204, o 401/403 como arriba.

## Cambio relacionado: `DELETE /admin/fleet/boats/{boatId}`

Ahora recibe `{"reason": "..."}` (obligatorio, 1 a 300 caracteres, 400 si no cumple) y lo incluye al final del mensaje: «… el [fecha]. Motivo: [motivo]». Ver `eliminar-embarcacion.md`.

## Notas de implementación

| Tema | Detalle |
|---|---|
| Base de datos | Sin migraciones nuevas (la V8 de antes agregó `VESSEL_DELETED`) |
| Marcar leída | `UPDATE notification SET is_read = TRUE WHERE id = ? AND recipient_id = ?`; 0 filas es 404 |
| Frontend | Campana en el header solo para OWNER; consulta al cargar, al abrir y cada 60 s; clic marca como leída sin navegar |
