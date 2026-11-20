# Contrato REST: Registrar Embarcación (Módulo 1)

> Describe la API tal como quedó implementada y probada. Base: `/api/v1`. Todos los endpoints de esta funcionalidad exigen `Authorization: Bearer <token>` y rol **OWNER** (el administrador recibe 403). Cada propietario solo ve y modifica sus propias embarcaciones.

**Última actualización**: 5 de noviembre de 2026.

## Convenciones

| Tema | Regla |
|---|---|
| Nombres de campos | `snake_case` (`boat_id`, `legal_registration`, `max_capacity`) |
| Decimales | Viajan como **texto** (`base_rate`, `latitude`, `longitude`) para no perder precisión |
| Fechas | ISO-8601 en UTC, por ejemplo `2026-11-05T14:30:15Z` |
| Identificadores | UUID en texto |
| Normalización | El servidor elimina espacios al inicio y al final de nombre, matrícula y puerto |
| Errores | RFC 9457 (`application/problem+json`) con `status`, `title`, `detail` y `correlation_id` |

### Formato de error

```json
{
  "status": 400,
  "title": "Solicitud inválida",
  "detail": "La solicitud contiene datos inválidos.",
  "correlation_id": "b1f0c0de-...",
  "errors": [
    { "field": "legal_registration", "message": "Formato de matrícula legal inválido" }
  ]
}
```

`errors` aparece en los 400 de validación y en el 409 de matrícula repetida. Los demás errores solo traen `detail`. El `correlation_id` también viaja en la cabecera `X-Correlation-Id`.

### Campos que pueden aparecer en `errors[].field`

| Paso | Campos |
|---|---|
| 1 | `name`, `legal_registration`, `type`, `max_capacity` |
| 2 | `port_name`, `coordinates` (latitud o longitud), `base_rate`, `photo` |

## Resumen

| Método | Ruta | Para qué | Éxito |
|---|---|---|---|
| GET | `/fleet/boats` | Lista de embarcaciones registradas | 200 |
| GET | `/fleet/boats/legal-registration-availability?value=` | Verificar si una matrícula está libre | 200 |
| GET | `/fleet/boats/draft` | Consultar el borrador del propietario | 200 |
| POST | `/fleet/boats/draft` | Paso 1: crear el borrador | 201 |
| DELETE | `/fleet/boats/draft` | Eliminar el borrador | 204 |
| POST | `/fleet/boats/{boatId}/registration` | Paso 2: completar el registro (multipart) | 200 |
| GET | `/fleet/boat-types` | Tipos de embarcación y capacidad máxima | 200 |

---

## GET `/fleet/boats`

Lista las embarcaciones **registradas** del propietario (los borradores no aparecen), de la más reciente a la más antigua según `registered_at`. Solo la primera puede llevar `is_new = true`, y solo si pasaron menos de 168 horas exactas desde su registro.

**200**

```json
{
  "boats": [
    {
      "boat_id": "7c1e...",
      "name": "Río Magdalena I",
      "legal_registration": "CP-05-1042-A",
      "type": "MOTORBOAT",
      "status": "AVAILABLE",
      "is_new": true,
      "registered_at": "2026-11-05T14:30:15Z"
    }
  ]
}
```

Lista vacía: `{ "boats": [] }`.

**Errores**: 401, 403.

---

## GET `/fleet/boats/legal-registration-availability?value=CP-05-1042-A`

Verificación en tiempo real de la matrícula. No distingue si el valor es válido: solo dice si ya existe.

**200**: `{ "available": true }` o `{ "available": false }`

**Errores**: 401, 403.

---

## GET `/fleet/boats/draft`

Devuelve el borrador del propietario (una embarcación con `status = DRAFT`).

**200**

```json
{
  "boat_id": "7c1e...",
  "name": "Río Magdalena II",
  "legal_registration": "CP-05-1042-A",
  "type": "MOTORBOAT",
  "max_capacity": 8,
  "status": "DRAFT"
}
```

**Errores**: 401, 403, **404** si no tiene borrador.

---

## POST `/fleet/boats/draft` (paso 1)

Crea el borrador. Un propietario solo puede tener uno.

**Cuerpo (JSON)**

| Campo | Tipo | Regla |
|---|---|---|
| `name` | texto | 3 a 50 caracteres, al menos una letra, solo letras, números, espacios, tildes y guiones |
| `legal_registration` | texto | Formato `CP-NN-NNNN-L` (dos dígitos, cuatro dígitos, una letra mayúscula). Única en el sistema |
| `type` | texto | `MOTORBOAT`, `YACHT`, `CATAMARAN` o `SAILBOAT` |
| `max_capacity` | entero | Mayor que 0 y no mayor que el máximo del tipo |

**201**: el borrador (misma forma que `GET /fleet/boats/draft`).

**Idempotencia**: si el propietario ya tiene un borrador con **la misma matrícula**, se devuelve ese borrador (con 201) en vez de crear otro. Sirve para reintentos tras un corte de red.

**Errores**

| Estado | Cuándo | Mensaje |
|---|---|---|
| 400 | Campo vacío | Este campo es obligatorio para continuar con el registro |
| 400 | Nombre inválido | El nombre debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones |
| 400 | Formato de matrícula | Formato de matrícula legal inválido |
| 400 | Tipo desconocido | Tipo de embarcación inválido |
| 400 | Capacidad con decimales o negativa | Ingrese un número entero sin puntos ni comas |
| 400 | Capacidad igual a 0 | Ingrese un número entero mayor que 0 |
| 400 | Capacidad sobre el máximo del tipo | Este valor excede la capacidad máxima de la embarcación |
| 409 | Matrícula ya registrada (de cualquier propietario) | `errors`: `legal_registration` → Matrícula existente en el sistema |
| 409 | Ya tiene un borrador con otra matrícula | Ya tienes un borrador de registro. Continúalo o elimínalo antes de empezar uno nuevo. |
| 401 / 403 | Sin token o rol incorrecto | |

---

## DELETE `/fleet/boats/draft`

Elimina el borrador de forma **permanente** (borrado físico). La matrícula queda libre al instante. Si no hay borrador, igualmente responde 204.

**204** sin cuerpo. **Errores**: 401, 403.

---

## POST `/fleet/boats/{boatId}/registration` (paso 2)

Completa el registro y deja la embarcación en `AVAILABLE`. `Content-Type: multipart/form-data`.

**Campos del formulario**

| Campo | Tipo | Regla |
|---|---|---|
| `port_name` | texto | Mismas reglas del nombre de la embarcación. Si ya existe un puerto con ese nombre (sin distinguir mayúsculas ni tildes) se reutiliza con sus coordenadas originales |
| `latitude` | texto | Número con signo entre -90 y 90; se redondea a 4 decimales (mitad hacia arriba) |
| `longitude` | texto | Número con signo entre -180 y 180; se redondea a 4 decimales |
| `base_rate` | texto | Entero en COP, mayor que 0, sin puntos ni comas, máximo 9.999.999.999 |
| `life_jackets` | booleano | Por defecto `false` |
| `cooler_with_ice` | booleano | Por defecto `false` |
| `sound_system` | booleano | Por defecto `false` |
| `fishing_gear` | booleano | Por defecto `false` |
| `diving_gear` | booleano | Por defecto `false` |
| `photo` | archivo | PNG o JPG **reales** (se valida por el contenido, no por la extensión), máximo 10 MB |

Los servicios Capitán y Combustible siempre van incluidos y no se envían ni se guardan.

**200**

```json
{
  "boat_id": "7c1e...",
  "name": "Río Magdalena II",
  "legal_registration": "CP-05-1042-A",
  "type": "MOTORBOAT",
  "max_capacity": 8,
  "status": "AVAILABLE",
  "photo_url": "https://res.cloudinary.com/.../seashare/boats/xyz.png",
  "base_rate": "180000.00",
  "port": { "name": "Marina Magdalena", "latitude": "11.2408", "longitude": "-74.1990" },
  "services": {
    "life_jackets": true,
    "cooler_with_ice": false,
    "sound_system": false,
    "fishing_gear": false,
    "diving_gear": false
  },
  "registered_at": "2026-11-05T14:30:15Z"
}
```

**Idempotencia**: si la embarcación ya está `AVAILABLE` (por ejemplo, el guardado sí se completó pero la respuesta se perdió), se devuelve el resultado guardado con 200, sin volver a subir la foto.

**Errores**

| Estado | Cuándo | Mensaje |
|---|---|---|
| 400 | Campo vacío | Este campo es obligatorio para continuar con el registro |
| 400 | `port_name` inválido | El nombre del puerto debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones |
| 400 | Coordenadas vacías (`coordinates`) | Las coordenadas son obligatorias |
| 400 | Coordenadas fuera de rango o no numéricas | Las coordenadas no son válidas |
| 400 | Tarifa con texto | Solo se aceptan valores numéricos |
| 400 | Tarifa con puntos, comas o signos | Ingrese un número entero sin puntos ni comas |
| 400 | Tarifa 0 | La tarifa debe ser mayor que 0 |
| 400 | Tarifa mayor a 9.999.999.999 | La tarifa excede el valor permitido |
| 400 | Foto que no es PNG ni JPG | Formato de imagen inválido |
| 400 | Foto mayor a 10 MB | El tamaño de imagen se excede (`field: photo`; es 400, no 413) |
| 404 | El borrador no existe o es de otro propietario | El borrador ya no existe. |
| 409 | La embarcación existe pero no es borrador ni está disponible | La embarcación no está en estado de borrador. |
| 502 | Falla el servicio de imágenes (Cloudinary) | Error en un servicio externo |
| 401 / 403 | Sin token o rol incorrecto | |

Si hay varios campos inválidos, todos se devuelven juntos en `errors`. La foto se sube a Cloudinary **solo** cuando todo lo demás es válido.

---

## GET `/fleet/boat-types`

Alimenta el selector del formulario.

**200**

```json
{
  "types": [
    { "code": "MOTORBOAT", "name": "Lancha", "max_capacity": 10 },
    { "code": "YACHT", "name": "Yate", "max_capacity": 20 },
    { "code": "CATAMARAN", "name": "Catamarán", "max_capacity": 30 },
    { "code": "SAILBOAT", "name": "Velero", "max_capacity": 12 }
  ]
}
```

**Errores**: 401.

---

## Notas de implementación

| Tema | Detalle |
|---|---|
| Foto de más de 10 MB | Spring corta la subida antes de validar el formato; por eso un archivo de más de 10 MB que además no sea imagen responde «El tamaño de imagen se excede» y no «Formato de imagen inválido» |
| Carrera por la matrícula | Si dos propietarios la registran a la vez, el índice único de la base de datos decide y el perdedor recibe el 409 normal |
| Carrera por el puerto | Si dos registros crean el mismo puerto a la vez, el segundo reutiliza el existente |
| Transacciones | Cada operación de persistencia es atómica; la capa de aplicación no usa `@Transactional` |
