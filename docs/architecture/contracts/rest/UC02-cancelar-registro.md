# Contrato: Cancelar registro (conservar avance del Paso 2)

**Fecha**: 12 de noviembre de 2026 · **Especificación**: `cancelar-registro-spec.md` · **Estado**: Implementado, pendiente de prueba manual

## API REST

Sin cambios. No hay endpoints ni columnas nuevas: el avance del Paso 2 vive solo en el navegador. «Salir» sigue sin llamar al servidor y el borrador conserva los datos del Paso 1.

## Almacenamiento en el navegador

- Clave: `localStorage["seashare.draftProgress.{userId}.{boatId}"]`
- Valor (JSON): `{ portName, rate, point: {latitude, longitude} | null, services: {lifeJackets, coolerWithIce, soundSystem, fishingGear, divingGear} }`
- La fotografía no se guarda.

| Momento | Acción |
|---|---|
| «Salir» del modal «¿Suspender registro?» | Guarda el avance (si todo está vacío, borra el anterior) |
| Cambio de pantalla (`hashchange`) o cierre de pestaña (`pagehide`) | Guarda el avance |
| Entrar al Paso 2 | Si hay avance de ese usuario y borrador, rellena puerto, coordenadas (marcador en el mapa), tarifa y servicios |
| Registro completado, o borrador inexistente (404) | Borra el avance |
| Datos dañados o navegador sin almacenamiento | Se ignoran; el Paso 2 funciona vacío |

## Frontend

- Nuevo `src/state/draftProgress.js` (`saveProgress`, `loadProgress`, `clearProgress`)
- `src/pages/registerBoatDetails.js`: restaura al montar, guarda al salir, limpia al terminar
