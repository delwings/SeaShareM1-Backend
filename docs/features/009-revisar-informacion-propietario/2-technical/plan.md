# Plan técnico — Consultar información de propietario (Admin)

**Fecha**: 11 de noviembre de 2026
**Especificación**: `consultar-informacion-propietario-spec.md`
**Contrato**: `consultar-informacion-propietario.md`
**Estado**: Implementado; verificación completa (`mvn clean verify`, `npm run check` y pruebas manuales) pendiente del usuario

## Resumen
Búsqueda de propietarios (barra independiente en el inicio del admin) y pantalla de solo lectura con sus datos. Backend con dos endpoints; frontend con componente de búsqueda y página de propietario.

## Decisiones

| # | Decisión | Motivo |
|---|---|---|
| D1 | Sin tipo de documento, fecha de registro ni estado de cuenta | `users` no los guarda; sin cambio de modelo físico |
| D2 | Búsqueda solo sobre rol OWNER | Los administradores no son propietarios |
| D3 | Mínimo 3 / máximo 100 caracteres, 5 resultados + `has_more` | Spec FR-002/FR-005 |
| D4 | Documento comparado solo por dígitos | Tolera puntos, comas y espacios |
| D5 | Debounce de 300 ms en el cliente | Evitar una consulta por tecla |
| D6 | 400 por UUID inválido tratado como 404 en la UI | Sin reintento inútil |
| D7 | «Volver» a la tabla sin filtros | Decisión del usuario |
| D8 | Sin auditoría de consultas | FR-032 sin confirmar |

## Contexto técnico
Java 17, Spring Boot 4.1.1, Clean Architecture; JWT con `@PreAuthorize("hasRole('ADMIN')")`; errores RFC 9457. Frontend JS vanilla con router por rol, Vitest.

## Estructura
```
backend
├─ domain/port: UserRepositoryPort (findById, findAllByRole)
├─ application/usecase: AdminOwnersService
│   ├─ port: SearchOwnersUseCase, GetOwnerUseCase, OwnerSearchResult
├─ infrastructure/persistence: UserJpaRepository.findByRole, UserPersistenceAdapter
├─ infrastructure/web: AdminOwnersController, OwnerSearchResponse, OwnerDetailResponse
└─ config: UseCaseConfig
frontend
├─ services: adminOwnerService.js
├─ ui: ownerFormat.js
├─ components: ownerSearch.js
├─ pages: adminOwner.js, adminHome.js
└─ routes: /admin/owners/:ownerId
```

## Fases
### Fase 1 — Backend
- [x] T149 Puertos, casos de uso y `AdminOwnersService`
- [x] T150 `AdminOwnersController` y DTOs
- [x] T151 Pruebas: `AdminOwnersServiceTest`, ITs en `BoatRegistrationApiIT`

### Fase 2 — Frontend
- [x] T152 `adminOwnerService` y `ownerFormat`
- [x] T153 Componente `ownerSearch` (debounce, estados, cierre por clic fuera)
- [x] T154 Página `adminOwner` y ruta
- [x] T155 Integración en `adminHome`; búsqueda independiente de la tabla

### Fase 3 — Verificación
- [ ] T156 `mvn spotless:apply` + `mvn clean verify`; `npm run check`; pruebas manuales

## Pendientes
- Auditoría de consultas (FR-032), requiere confirmación.
- Tipo de documento, fecha de registro y estado de cuenta: requieren ampliar el modelo y tu aprobación.
- Aviso de sesión expirada y destino de retorno (T081/FR-007), común a todas las pantallas.
- Parámetro `q` de `/admin/fleet/boats` sin uso en la UI; filtrado en memoria sin paginación.

## Dependencias
Spec «Consultar detalles de embarcación» (inicio del admin, filtros); autenticación JWT.

## Notas
- Reiniciar el backend por completo tras copiar el zip.
- Usuarios de prueba (clave `SeaShare2026!`): admin 1010000001; propietarios 1010000002–1010000006.
