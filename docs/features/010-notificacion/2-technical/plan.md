# Plan de implementación: Notificaciones del Propietario (campana)

**Fecha**: 11 de noviembre de 2026
**Especificación**: `notificaciones-spec.md`
**Contrato REST**: `notificaciones.md`
**Estado**: backend y frontend implementados y entregados. Frontend: 337 pruebas en verde. Backend: pruebas unitarias del caso de uso en verde en el sandbox; falta `mvn spotless:apply` y `mvn clean verify` (ITs con MySQL).

## Resumen

El propietario ve en una campana del header las notificaciones que le llegan; la primera es la de embarcación eliminada por un administrador, que ahora incluye el motivo que el administrador escribe en el modal.

### Decisiones

| # | Decisión | Motivo |
|---|---|---|
| D1 | El motivo viaja en el cuerpo del `DELETE` del administrador y se guarda dentro del mensaje | Sin migración ni columna nueva; la campana solo muestra texto |
| D2 | El motivo se valida antes de buscar la embarcación (400, campo `reason`) | No eliminar nada con un motivo inválido |
| D3 | Solo el Propietario tiene campana y endpoints | Decisión del usuario; el administrador no recibe avisos hoy |
| D4 | Clic en una notificación solo la marca como leída | La embarcación eliminada ya no se puede abrir |
| D5 | Sin tiempo real: consulta al cargar la pantalla, al abrir el panel y cada 60 s | Simple y suficiente |
| D6 | Marcar como leída es un `UPDATE` con `recipient_id`; 0 filas es 404 | Privacidad sin revelar si existe |
| D7 | Máximo 50 en la lista; el contador cuenta todas las no leídas | Evita listas enormes sin paginar |

## Estructura

```text
backend (src/main/java/com/seashare/seasharem1)
├── application/port/in/{ListNotificationsUseCase, MarkNotificationsReadUseCase, NotificationList, AdminDeleteBoatUseCase*}
├── application/port/out/NotificationRepositoryPort*       # lista, contador, marcar
├── application/usecase/{NotificationService, AdminDeleteBoatService*}
├── infrastructure/adapter/in/web/{NotificationController, AdminFleetController*}
├── infrastructure/adapter/in/web/dto/{NotificationResponse, NotificationListResponse, AdminDeleteRequest}
├── infrastructure/adapter/out/persistence/{NotificationPersistenceAdapter*, repository/NotificationJpaRepository*}
└── configuration/UseCaseConfig*
frontend (src)
├── services/{notificationService.js, adminFleetService.js*}
├── components/{notificationBell.js, header.js*, deleteBoatFlow.js*}
├── pages/adminBoatDetail.js*
└── styles/main.css*                                       # (* = modificado)
```

## Fases

### Fase 1 - Motivo de la eliminación
- [x] T113 Backend: `reason` obligatorio (1 a 300) en `AdminDeleteBoatUseCase`; 400 con `errors[{field:"reason"}]`; el mensaje termina en «Motivo: …».
- [x] T114 Backend: `AdminDeleteRequest` y `DELETE` con cuerpo en `AdminFleetController`.
- [x] T115 Frontend: `deleteBoatFlow` con `askReason` (campo, contador, error, el motivo se conserva al reintentar); `deleteAdminBoat(boatId, reason)`.

### Fase 2 - Campana
- [x] T116 Backend: `NotificationRepositoryPort` ampliado, `NotificationService`, `NotificationController` (`GET`, `PATCH /{id}/read`, `PATCH /read-all`), DTOs y bean.
- [x] T117 Frontend: `notificationService`, `notificationBell` (contador, panel, marcar una y todas, vacío, error con «Reintentar», Esc y clic fuera) y su inserción en el header del propietario.

### Fase 3 - Pruebas
- [x] T118 Backend: `NotificationServiceTest`, `AdminDeleteBoatServiceTest` ampliado (motivo vacío, nulo, de 301, recortado), ITs: el propietario ve el motivo, marca leídas, es privado, el administrador recibe 403, 400 sin motivo.
- [x] T119 Frontend: `notificationBell.test.js`, `notificationService.test.js`, motivo en `deleteBoatFlow.test.js` y `adminBoatDetail.test.js`.
- [ ] T120 Backend: `mvn spotless:apply` y `mvn clean verify`.
- [ ] T121 Prueba manual: eliminar como admin con motivo; entrar como propietario, ver el contador y el motivo, marcar como leída.

## Pendientes
- Notificaciones de mantenimiento, reseñas y disponibilidad (los tipos existen; nada las crea).
- Campana del administrador.
- Actualización en tiempo real, paginación y borrado de notificaciones.

## Notas
- Reiniciar el backend por completo tras copiar el zip. Sin migraciones nuevas.
- Prueba rápida: administrador `1010000001`, propietarios `1010000002`–`1010000006`, clave `SeaShare2026!`.
