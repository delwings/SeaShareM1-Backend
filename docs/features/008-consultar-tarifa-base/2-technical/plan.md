# Plan técnico: Consultar tarifa base

**Fecha**: 13 de noviembre de 2026 · **Especificación**: `consultar-tarifa-base-spec.md` · **Contrato**: `consultar-tarifa-base.md` · **Estado**: Implementado, pendiente de verificación

## Resumen

El Módulo 3 envía un lote de identificadores y el Módulo 1 devuelve la tarifa base de cada embarcación registrada, y aparte las no encontradas. Solo backend, sin migración y sin autenticación.

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| D1 | Endpoint abierto, sin API key ni JWT (`permitAll` solo para este POST) | Decisión de Jose |
| D2 | `POST /api/v1/boats/base-rates`, cuerpo `{"boat_ids":[...]}` | Spec sin ruta; propuesta mía |
| D3 | Respuesta `rates[{boat_id, base_rate}]` + `not_found[]`; `base_rate` como texto con dos decimales | Ejemplo de Jose |
| D4 | Borrador, eliminada e inexistente se tratan igual (`not_found`) | Spec FR-006 |
| D5 | Un lote inválido se rechaza completo con 400 y errores por campo | Spec FR-009 |
| D6 | Sin cambios en el modelo físico: se lee `vessel.base_rate` | — |

## Estructura

```
application/port/in/BaseRateQuery.java
application/usecase/BaseRateQueryService.java
application/port/out/VesselRepositoryPort.java        (+ findRegisteredByIds)
infrastructure/adapter/in/web/BaseRateController.java
infrastructure/adapter/in/web/dto/BaseRatesRequest|Response.java
infrastructure/adapter/out/persistence/...            (consulta por lote)
configuration/SecurityConfig.java, UseCaseConfig.java
```

## Fases

### Fase 1: Backend
- [x] T164 Puerto de entrada y servicio con validación del lote (vacío, >100, formato) y deduplicación
- [x] T165 Consulta por lote excluyendo borradores y eliminadas
- [x] T166 Controlador, DTO y ruta pública
- [x] T167 Pruebas unitarias (8) e IT (2 casos en `BoatRegistrationApiIT`)

### Fase 2: Verificación (pendiente)
- [ ] T168 `mvn spotless:apply` y `mvn clean verify`
- [ ] T169 Pruebas en Postman según `consultar-tarifa-base.md`

## Pendientes

- Integración con el Módulo 3 (confirmar con ellos el formato `"350000.00"` y el nombre `not_found`).
- Si más adelante se asegura la consulta entre módulos, hay que quitar el `permitAll` y definir el mecanismo.

## Notas

Sin cambios de frontend ni migraciones. Reinicia el backend tras copiar el zip.
