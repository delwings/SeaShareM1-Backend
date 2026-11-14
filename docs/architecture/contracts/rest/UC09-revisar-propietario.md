# Contrato REST — Consultar información de propietario (Admin)

**Fecha**: 11 de noviembre de 2026
**Especificación**: `consultar-informacion-propietario-spec.md`
**Base**: `/api/v1/admin/owners` · **Autenticación**: `Authorization: Bearer <JWT>` · **Rol**: solo `ADMIN` (un `OWNER` recibe 403)

---

## 1. Buscar propietarios

`GET /api/v1/admin/owners?q={texto}`

| Parámetro | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `q` | string | Sí | Entre 3 y 100 caracteres (tras recortar espacios) |

**Semántica de búsqueda**
- Solo usuarios con rol `OWNER` (nunca administradores), con o sin embarcaciones.
- Coincide por **nombre** (parcial, sin distinguir mayúsculas ni tildes) o por **documento** (se ignoran puntos, comas y espacios; se comparan solo los dígitos).
- Orden: nombre ascendente, luego documento. Máximo **5** resultados.

**200 OK**
```json
{
  "owners": [
    { "owner_id": "uuid", "full_name": "Carlos Mendoza", "document_number": "1042389104", "email": "carlos@correo.com", "phone": "3104567890" }
  ],
  "has_more": false
}
```
`has_more = true` si existían más de 5 coincidencias.

| Código | Cuándo | Cuerpo |
|---|---|---|
| 400 | `q` ausente, < 3 o > 100 caracteres | RFC 9457 con `errors[{field:"q", message}]` y `correlation_id` |
| 401 | Token ausente, inválido o vencido | RFC 9457 |
| 403 | Usuario sin rol ADMIN | RFC 9457 |

---

## 2. Consultar un propietario

`GET /api/v1/admin/owners/{ownerId}`

| Parámetro | Tipo | Regla |
|---|---|---|
| `ownerId` | UUID (path) | Debe ser un UUID válido de un usuario con rol `OWNER` |

**200 OK**
```json
{
  "owner_id": "uuid",
  "full_name": "Carlos Mendoza",
  "document_number": "1042389104",
  "email": "carlos@correo.com",
  "phone": "3104567890"
}
```

| Código | Cuándo |
|---|---|
| 401 | Token ausente, inválido o vencido |
| 403 | Usuario sin rol ADMIN |
| 404 | No existe, es un administrador o el identificador no es un UUID válido |

La contraseña y demás datos internos **nunca** se devuelven.

---

## 3. Comportamiento del frontend

| Situación | Comportamiento |
|---|---|
| < 3 caracteres | No consulta, ventana cerrada |
| ≥ 3 caracteres | Consulta tras 300 ms sin escribir; muestra cargando |
| Resultados | Avatar con iniciales, nombre, documento y «Ver Detalles» |
| `has_more` | Aviso de que hay más coincidencias |
| Sin resultados | Mensaje «sin resultados» |
| Error de búsqueda | Mensaje con «Reintentar» |
| «Ver Detalles» | Navega a `#/admin/owners/{ownerId}` |
| Pantalla del propietario | Correo, teléfono (`+57 310 456 7890`) y documento sin puntos; solo lectura |
| 404 / 400 | Pantalla de error con «Volver» (sin reintento) |
| 401 | Redirige al inicio de sesión |
| 403 | Pantalla de acceso no autorizado |
| «Volver» | Tabla de flota en su estado por defecto, sin filtros |

## 4. Notas
- Sin migraciones: usa la tabla `users` existente (V1–V7).
- No se guardan tipo de documento, fecha de registro ni estado de cuenta.
- No hay registro de auditoría de consultas (FR-032 pendiente).
