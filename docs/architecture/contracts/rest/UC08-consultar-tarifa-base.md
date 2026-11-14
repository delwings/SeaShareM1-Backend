# Contrato: Consultar tarifa base (Módulo 1 → Módulo 3)

**Fecha**: 13 de noviembre de 2026 · **Especificación**: `consultar-tarifa-base-spec.md` · **Estado**: Implementado, pendiente de prueba

## Endpoint

`POST /api/v1/boats/base-rates` · sin autenticación · solo lectura (el POST es por el cuerpo del lote)

**Solicitud**

```json
{ "boat_ids": ["3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01", "..."] }
```

**Respuesta 200**

```json
{
  "rates": [
    { "boat_id": "3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d01", "base_rate": "350000.00" }
  ],
  "not_found": ["b1d0c3e2-0000-4000-8000-000000000000"]
}
```

| Caso | Resultado |
|---|---|
| Embarcación registrada (Disponible, Reservado, En Navegación, Mantenimiento) | Va en `rates` con su tarifa guardada |
| Inexistente, eliminada o en Borrador | Va en `not_found`, sin motivo ni otro dato |
| Identificador repetido | Se entrega una sola vez |
| Ninguna encontrada | `rates: []` y todos en `not_found` |

**Errores** (RFC 9457, con `correlation_id` y `errors[{field,message}]`)

| Código | Cuándo |
|---|---|
| `400` | Lote vacío o ausente, más de 100, identificador con formato inválido (`boat_ids[i]`) o JSON mal formado. No se entrega ninguna tarifa |
| `500` | Falla técnica: error para todo el lote, sin tarifas parciales |

La tarifa se lee de la base en cada consulta (sin caché). Las embarcaciones no encontradas se registran en el log (NFR-005).

## Cómo probar en Postman

1. Levanta el backend (puerto 8081). **No** necesitas login ni token.
2. Obtén IDs reales: entra al sistema, abre «Ver detalles» de una embarcación y copia el `boat_id` (o llama `GET /api/v1/fleet/boats` con tu token de propietario).
3. Crea una petición `POST http://localhost:8081/api/v1/boats/base-rates`, Body → raw → JSON.

| Prueba | Cuerpo | Esperado |
|---|---|---|
| Una embarcación | `{"boat_ids":["<id>"]}` | 200, una tarifa, `not_found: []` |
| Varias | `{"boat_ids":["<id1>","<id2>","<id3>"]}` | 200, tres tarifas en ese orden |
| Mezcla con inexistente | `{"boat_ids":["<id1>","3f2c1a54-8b3e-4d7a-9c10-5a2b7e6f1d99"]}` | 200, una tarifa y el otro en `not_found` |
| Borrador o eliminada | ID de un borrador / embarcación eliminada | 200, en `not_found` |
| Repetido | `{"boat_ids":["<id>","<id>"]}` | 200, una sola tarifa |
| Cualquier estado | Pon una embarcación en mantenimiento y consúltala | 200, su tarifa |
| Tarifa editada | Edita la tarifa en la app y vuelve a consultar | 200, el valor nuevo |
| Lote vacío | `{"boat_ids":[]}` | 400 |
| Sin campo | `{}` | 400 |
| ID inválido | `{"boat_ids":["abc"]}` | 400, `errors[0].field = "boat_ids[0]"` |
| Más de 100 | 101 identificadores | 400 |
