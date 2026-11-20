# Plan técnico: Cancelar registro — conservar el avance del Paso 2

**Fecha**: 12 de noviembre de 2026 · **Especificación**: `cancelar-registro-spec.md` · **Contrato**: `cancelar-registro.md` · **Estado**: Implementado, pendiente de prueba manual

## Resumen

Al salir del Paso 2 el propietario conserva lo escrito (puerto, coordenadas, tarifa, servicios) y lo recupera al retomar el borrador. Solo frontend.

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| D1 | Guardar en el navegador (localStorage), sin migración ni endpoints | Decisión de Jose |
| D2 | Guardar al pulsar «Salir» y también al cambiar de pantalla o cerrar la pestaña | Decisión de Jose |
| D3 | Clave por usuario y borrador; se borra al completar el registro o si el borrador no existe | Evitar mezclar datos |
| D4 | La fotografía no se conserva | Limitación técnica |
| D5 | Sin cambios en el texto del modal «¿Suspender registro?» | No solicitado |

## Estructura

```
src/state/draftProgress.js        (nuevo)
src/state/draftProgress.test.js   (nuevo)
src/pages/registerBoatDetails.js  (restaura, guarda, limpia)
src/pages/registerBoatDetails.test.js
```

## Fases

### Fase 1: Implementación
- [x] T158 `draftProgress.js`: guardar, cargar (validando la forma) y borrar
- [x] T159 Paso 2: restaurar campos, coordenadas y marcador al montar
- [x] T160 Paso 2: guardar en «Salir», `hashchange` y `pagehide`; limpiar al registrar o si el borrador no existe
- [x] T161 Pruebas (383 en verde en `npm run check`)

### Fase 2: Verificación (pendiente)
- [ ] T162 Prueba manual: escribir puerto, marcar punto, tarifa y servicios → Cancelar → Salir → retomar el borrador
- [ ] T163 Prueba manual: salir con otro menú sin cancelar; cerrar sesión y entrar con otro usuario; completar el registro y comprobar que no queda avance

## Pendientes

- Guardar la fotografía o el avance en el servidor (permitiría cambiar de dispositivo): requiere cambio de modelo físico.

## Notas

Solo frontend: no hace falta reiniciar el backend ni ejecutar Maven.
