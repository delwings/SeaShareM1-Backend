# Plan técnico: Asignar estado operativo

**Fecha**: 13 de noviembre de 2026 · **Especificación**: `asignar-estado-operativo-spec.md` · **Contrato**: `asignar-estado-operativo.md` · **Estado**: Implementado, pendiente de verificación

## Resumen

M1 aplica los avisos de estado operativo de M2 (pago confirmado, check-in, fin de viaje, cancelación), sin repetirlos ni retroceder el tiempo, y guarda el historial de cada cambio. Hoy el aviso entra por un endpoint REST que simula la cola.

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| D1 | Simulador REST ahora; RabbitMQ real en la integración | Decisión de Jose |
| D2 | Un solo caso de uso (`AssignOperationalStatusUseCase`) que usarán REST y RabbitMQ | Arquitectura hexagonal |
| D3 | Solo las transiciones del spec; las demás se ignoran, se registran y se confirman | Decisión de Jose |
| D4 | Se aceptan `IN_NAVIGATION`/`NAVIGATION` y `MAINTENANCE_CLEANING`/`MAINTENANCE` | M2 usa otros nombres |
| D5 | Migración V10 (`status_changed_at`, `processed_event`, `vessel_status_history`) | Aprobada por Jose |
| D6 | El cambio de estado es una sola sentencia condicional (estado esperado, sin eliminar, sin cambio posterior); el historial se anota en la misma transacción | Avisos concurrentes o simultáneos con acciones manuales |
| D7 | El mantenimiento por aviso queda sin origen (`maintenance_by` nulo): lo termina el propietario | Spec, historia 1 escenario 3 |
| D8 | El historial incluye los cambios manuales (Enviar a Mantenimiento y Terminar mantenimiento, de ambos roles) | FR-006 |
| D9 | Los motivos de descarte (`DUPLICATE`, `STALE`, etc.) se devuelven en la respuesta y se registran en el log | Spec: «guarda un registro de lo sucedido» |

## Estructura

```
application/port/in/AssignOperationalStatusUseCase.java
application/port/out/ProcessedEventRepositoryPort.java, VesselRepositoryPort.java (+ changeStatusAutomatically, findStatusChangedAt)
application/usecase/AssignOperationalStatusService.java
infrastructure/adapter/in/web/StatusEventController.java, dto/StatusEventRequest.java
infrastructure/adapter/out/persistence/ProcessedEventPersistenceAdapter.java, VesselPersistenceAdapter.java, repository/VesselJpaRepository.java
configuration/UseCaseConfig.java, SecurityConfig.java
resources/db/migration/V10__operational_status_events.sql
```

## Fases

### Fase 1: Backend
- [x] T175 Migración V10
- [x] T176 Caso de uso: validación, duplicados, aviso viejo, embarcación inexistente, transiciones
- [x] T177 Persistencia: cambio atómico con historial, registro de avisos procesados
- [x] T178 Historial de los cambios manuales de mantenimiento (propietario y administrador)
- [x] T179 Endpoint simulador y ruta pública
- [x] T180 Pruebas: 11 unitarias (`AssignOperationalStatusServiceTest`) y 2 IT; `DatabaseMigrationIT` con 10 migraciones

### Fase 2: Verificación (pendiente)
- [ ] T181 `mvn spotless:apply`, `mvn clean verify` y reinicio del backend
- [ ] T182 Pruebas en Postman según `asignar-estado-operativo.md`

## Pendientes (integración con M2)

- Consumidor RabbitMQ (dependencia AMQP, listener, cola y formato a acordar con M2, reintentos y cola de fallidos).
- Avisar a M2 de los nombres de estado (M1 acepta los suyos y los de M2) y de que los avisos fuera de transición se confirman sin cambios.
- Los cambios del registro (borrador a Disponible) no se anotan en el historial.

## RabbitMQ con Docker (para la integración)

```powershell
docker run -d --name seashare-rabbit -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=seashare -e RABBITMQ_DEFAULT_PASS=SeaShare2026 rabbitmq:3-management
```

Consola en `http://localhost:15672` (`seashare` / `SeaShare2026`). No hace falta para probar lo actual.
