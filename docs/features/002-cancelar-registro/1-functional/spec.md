# Especificación de funcionalidad: Cancelar registro

## Registro de cambios

**12 de noviembre de 2026** (conservar el avance del Paso 2):

| # | Cambio | Motivo |
|---|---|---|
| 1 | FR-017 cambia: al salir del Paso 2 ya no se descartan los datos del Paso 2; se conservan (menos la fotografía) y reaparecen al retomar el borrador (FR-023) | Decisión de Jose, probando el registro |
| 2 | Nuevos FR-023 a FR-026: restauración exacta, también al salir sin "Cancelar", aislada por usuario y borrador, tolerante a fallos del navegador | Consecuencia del cambio 1 |
| 3 | El avance se guarda en el navegador (localStorage), sin cambios en backend ni en la base de datos | Decisión de Jose |
| 4 | La fotografía no se conserva: un archivo no se puede guardar de forma fiable en el navegador | Limitación técnica |
| 5 | El avance no viaja entre dispositivos ni navegadores | Consecuencia del cambio 3 |
| 6 | El texto del modal «¿Suspender registro?» no cambia | No solicitado; se puede ajustar si se desea |

**Creado**: 2026-10-30

## Escenarios de usuario y pruebas _(obligatorio)_

### Historia de usuario 1 - Cancelar el registro desde el Paso 1 (Prioridad: P1)

Como propietario, quiero poder cancelar el registro de una embarcación mientras completo el formulario de Datos Básicos (Paso 1 de 2), para abandonar el proceso sin que quede ninguna embarcación guardada.

**Por qué esta prioridad**: Es el punto de salida más temprano del registro y el que evita que queden datos incompletos en el sistema. Sin esta historia, el propietario que se arrepiente en el Paso 1 no tendría una forma clara de abandonar el proceso.

**Prueba independiente**: Se puede probar por completo abriendo el formulario del Paso 1, haciendo clic en "Cancelar" y confirmando o rechazando la cancelación. Entrega valor porque el propietario controla si abandona o continúa, sin riesgo de perder datos por error.

**Escenarios de aceptación**:

1. **Escenario**: Abrir el modal de cancelación con el formulario vacío
    - **Dado** que el propietario está en el Paso 1 del registro y no ha ingresado ningún dato
    - **Cuando** hace clic en el botón "Cancelar"
    - **Entonces** el sistema muestra un modal con un ícono de advertencia, la pregunta "¿Cancelar registro?" y, debajo, la descripción "Los datos ingresados hasta el momento se perderán y no se guardará ninguna embarcación."
    - **Y** el modal ofrece los botones "Continuar registro" y "Sí, cancelar"

2. **Escenario**: Abrir el modal de cancelación con datos ingresados
    - **Dado** que el propietario está en el Paso 1 del registro y ha ingresado datos en uno o más campos
    - **Cuando** hace clic en el botón "Cancelar"
    - **Entonces** el sistema muestra el mismo modal "¿Cancelar registro?" con la misma descripción y los mismos botones

3. **Escenario**: Continuar el registro con el formulario vacío
    - **Dado** que el modal "¿Cancelar registro?" está visible y el propietario no había ingresado ningún dato
    - **Cuando** hace clic en "Continuar registro"
    - **Entonces** el modal se cierra
    - **Y** el propietario permanece en el Paso 1 con el formulario vacío, tal como estaba

4. **Escenario**: Continuar el registro con datos ingresados
    - **Dado** que el modal "¿Cancelar registro?" está visible y el propietario había ingresado datos en el formulario
    - **Cuando** hace clic en "Continuar registro"
    - **Entonces** el modal se cierra
    - **Y** el propietario permanece en el Paso 1 con los datos exactamente como los había ingresado

5. **Escenario**: Continuar el registro con mensajes de error visibles
    - **Dado** que el formulario del Paso 1 mostraba mensajes de error cuando el propietario hizo clic en "Cancelar"
    - **Cuando** hace clic en "Continuar registro"
    - **Entonces** el modal se cierra y los mensajes de error visibles se limpian
    - **Y** los datos ingresados se conservan

6. **Escenario**: Confirmar la cancelación
    - **Dado** que el modal "¿Cancelar registro?" está visible
    - **Cuando** el propietario hace clic en "Sí, cancelar"
    - **Entonces** el sistema lo lleva a la lista de embarcaciones del propietario, es decir, la pantalla que ve al iniciar sesión
    - **Y** los datos ingresados se descartan y no se guarda ninguna embarcación
    - **Y** no se muestra ningún mensaje adicional

7. **Escenario**: Cierre del modal solo con sus botones
    - **Dado** que el modal "¿Cancelar registro?" está visible
    - **Cuando** el propietario hace clic fuera del modal o presiona la tecla Esc
    - **Entonces** el modal permanece abierto
    - **Y** solo se cierra al usar uno de sus botones

---

### Historia de usuario 2 - Suspender el registro desde el Paso 2 (Prioridad: P2)

Como propietario, quiero poder suspender el registro de una embarcación mientras completo el formulario de Datos Complementarios (Paso 2 de 2), para salir del proceso y retomarlo en una sesión futura sin volver a ingresar los datos básicos.

**Por qué esta prioridad**: Protege el avance del propietario, pero depende de que el Paso 1 ya se haya completado, por lo que es menos crítica que poder abandonar el registro desde el inicio (Historia 1).

**Prueba independiente**: Se puede probar por completo desde el Paso 2, haciendo clic en "Cancelar" y luego en "Salir" o "Continuar registro". Entrega valor porque el propietario no pierde los datos básicos al salir.

**Escenarios de aceptación**:

1. **Escenario**: Abrir el modal de suspensión
    - **Dado** que el propietario está en el Paso 2 del registro
    - **Cuando** hace clic en el botón "Cancelar"
    - **Entonces** el sistema muestra un modal con un ícono de advertencia, la pregunta "¿Suspender registro?" y, debajo, la descripción "Los datos de identificación básica (Paso 1) se almacenarán de forma temporal en estado borrador, permitiendo reanudar el proceso de registro en futuras sesiones."
    - **Y** el modal ofrece los botones "Continuar registro" y "Salir"

2. **Escenario**: Continuar el registro sin datos en el Paso 2
    - **Dado** que el modal "¿Suspender registro?" está visible y el propietario no había ingresado ningún dato en el Paso 2
    - **Cuando** hace clic en "Continuar registro"
    - **Entonces** el modal se cierra
    - **Y** el propietario permanece en el Paso 2 con el formulario vacío, tal como estaba

3. **Escenario**: Continuar el registro con datos en el Paso 2
    - **Dado** que el modal "¿Suspender registro?" está visible y el propietario había ingresado datos en el Paso 2
    - **Cuando** hace clic en "Continuar registro"
    - **Entonces** el modal se cierra
    - **Y** el propietario permanece en el Paso 2 con los datos exactamente como los había ingresado

4. **Escenario**: Continuar el registro con mensajes de error visibles
    - **Dado** que el formulario del Paso 2 mostraba mensajes de error cuando el propietario hizo clic en "Cancelar"
    - **Cuando** hace clic en "Continuar registro"
    - **Entonces** el modal se cierra y los mensajes de error visibles se limpian
    - **Y** los datos ingresados se conservan

5. **Escenario**: Salir y conservar el Paso 1
    - **Dado** que el modal "¿Suspender registro?" está visible
    - **Cuando** el propietario hace clic en "Salir"
    - **Entonces** el sistema lo lleva a la lista de embarcaciones del propietario
    - **Y** los datos del Paso 1 permanecen guardados, con la embarcación en estado Borrador
    - **Y** no se muestra ningún mensaje adicional

6. **Escenario**: Salir con datos ingresados en el Paso 2
    - **Dado** que el propietario había ingresado datos en el Paso 2 (nombre del puerto, coordenadas, tarifa base o servicios adicionales)
    - **Cuando** hace clic en "Cancelar" y luego en "Salir"
    - **Entonces** esos datos se conservan en el navegador, junto con los del Paso 1 guardados en el borrador
    - **Y** la fotografía no se conserva

6b. **Escenario**: Retomar el borrador
- **Dado** que el propietario salió del Paso 2 con datos ingresados y la embarcación sigue en estado Borrador
- **Cuando** retoma el registro y llega al Paso 2
- **Entonces** el nombre del puerto, las coordenadas (con el marcador en el mapa), la tarifa base y los servicios adicionales aparecen exactamente como los dejó
- **Y** no se muestra ningún mensaje de error
- **Y** la fotografía aparece vacía y debe elegirse de nuevo

6c. **Escenario**: Salir del Paso 2 sin pulsar "Cancelar"
- **Dado** que el propietario tiene datos escritos en el Paso 2
- **Cuando** cambia de pantalla o cierra la pestaña sin usar "Cancelar"
- **Entonces** los datos también se conservan y se recuperan al retomar el borrador

7. **Escenario**: Cierre del modal solo con sus botones
    - **Dado** que el modal "¿Suspender registro?" está visible
    - **Cuando** el propietario hace clic fuera del modal o presiona la tecla Esc
    - **Entonces** el modal permanece abierto
    - **Y** solo se cierra al usar uno de sus botones

---

### Casos borde

- ¿Qué ocurre si el propietario hace clic en "Cancelar" en el Paso 1 sin haber ingresado nada? El modal "¿Cancelar registro?" se muestra igualmente; el sistema no omite la confirmación aunque el formulario esté vacío.
- ¿Qué ocurre si el propietario hace clic en "Cancelar" en el Paso 2 sin haber ingresado nada en ese paso? El modal "¿Suspender registro?" se muestra igualmente.
- ¿Qué ocurre si el propietario hace clic en "Salir" y su sesión ya expiró? El sistema lo redirige al inicio de sesión, sin ningún aviso especial. El borrador se conserva.
- ¿Qué ocurre si el propietario hace clic en "Salir" y el borrador ya no existe porque lo eliminó desde otra sesión? El sistema lo redirige a la lista de embarcaciones, sin ningún aviso especial.
- ¿Qué ocurre si el propietario pierde la conexión al hacer clic en "Salir"? El sistema lo redirige a la lista de embarcaciones, sin ningún aviso especial.
- ¿Qué ocurre con las embarcaciones que el propietario ya tenía registradas? Ninguna cancelación o suspensión las modifica ni las elimina.

## Requisitos _(obligatorio)_

### Requisitos funcionales

**Cancelar desde el Paso 1**

- **FR-001**: El sistema DEBE mostrar el botón "Cancelar" en el formulario del Paso 1 del registro.
- **FR-002**: El sistema DEBE mostrar el modal "¿Cancelar registro?" al hacer clic en "Cancelar" en el Paso 1, tanto si el formulario está vacío como si tiene datos ingresados.
- **FR-003**: El modal "¿Cancelar registro?" DEBE mostrar un ícono de advertencia, la pregunta "¿Cancelar registro?" y, debajo, la descripción "Los datos ingresados hasta el momento se perderán y no se guardará ninguna embarcación."
- **FR-004**: El modal "¿Cancelar registro?" DEBE ofrecer los botones "Continuar registro" y "Sí, cancelar".
- **FR-005**: Al hacer clic en "Continuar registro" en el modal del Paso 1, el sistema DEBE cerrar el modal y mantener al propietario en el Paso 1, con el formulario vacío si no había datos o con los datos tal como estaban si los había.
- **FR-006**: Al hacer clic en "Continuar registro" en el modal del Paso 1, el sistema DEBE limpiar los mensajes de error que estuvieran visibles en el formulario.
- **FR-007**: Al hacer clic en "Sí, cancelar", el sistema DEBE llevar al propietario a la lista de embarcaciones del propietario, la misma pantalla que ve al iniciar sesión.
- **FR-008**: Al hacer clic en "Sí, cancelar", el sistema DEBE descartar los datos ingresados y NO DEBE guardar ninguna embarcación.

**Suspender desde el Paso 2**

- **FR-009**: El sistema DEBE mostrar el botón "Cancelar" en el formulario del Paso 2 del registro.
- **FR-010**: El sistema DEBE mostrar el modal "¿Suspender registro?" al hacer clic en "Cancelar" en el Paso 2, tanto si el formulario está vacío como si tiene datos ingresados.
- **FR-011**: El modal "¿Suspender registro?" DEBE mostrar un ícono de advertencia, la pregunta "¿Suspender registro?" y, debajo, la descripción "Los datos de identificación básica (Paso 1) se almacenarán de forma temporal en estado borrador, permitiendo reanudar el proceso de registro en futuras sesiones."
- **FR-012**: El modal "¿Suspender registro?" DEBE ofrecer los botones "Continuar registro" y "Salir".
- **FR-013**: Al hacer clic en "Continuar registro" en el modal del Paso 2, el sistema DEBE cerrar el modal y mantener al propietario en el Paso 2, con el formulario vacío si no había datos o con los datos tal como estaban si los había.
- **FR-014**: Al hacer clic en "Continuar registro" en el modal del Paso 2, el sistema DEBE limpiar los mensajes de error que estuvieran visibles en el formulario.
- **FR-015**: Al hacer clic en "Salir", el sistema DEBE llevar al propietario a la lista de embarcaciones del propietario.
- **FR-016**: Al hacer clic en "Salir", el sistema DEBE conservar los datos del Paso 1 guardados, con la embarcación en estado Borrador.
- **FR-017**: Al hacer clic en "Salir", el sistema DEBE conservar los datos que el propietario hubiera ingresado en el Paso 2 (nombre del puerto, coordenadas, tarifa base y servicios adicionales), con excepción de la fotografía.
- **FR-023**: Al retomar el borrador, el Paso 2 DEBE mostrar esos datos tal como se dejaron, con el marcador del mapa en las coordenadas guardadas y sin mensajes de error.
- **FR-024**: El sistema DEBE conservar esos datos también cuando el propietario sale del Paso 2 sin usar "Cancelar" (cambio de pantalla o cierre de la pestaña).
- **FR-025**: Los datos conservados pertenecen a un usuario y a un borrador concretos: no se muestran a otro usuario ni en otro borrador. Se eliminan al completar el registro o cuando el borrador ya no existe.
- **FR-026**: Si el navegador no permite guardar los datos, el registro DEBE seguir funcionando con normalidad, sin avisos.

**Comportamiento común**

- **FR-018**: Los modales "¿Cancelar registro?" y "¿Suspender registro?" DEBEN cerrarse únicamente con sus botones; NO DEBEN cerrarse al hacer clic fuera del modal ni al presionar la tecla Esc.
- **FR-019**: El sistema NO DEBE mostrar ningún mensaje al propietario al llegar a la lista de embarcaciones tras "Sí, cancelar" o "Salir".
- **FR-020**: Si al hacer clic en "Salir" la sesión del propietario ya expiró, el sistema DEBE redirigirlo al inicio de sesión sin ningún aviso especial y conservar el borrador.
- **FR-021**: Si al hacer clic en "Salir" el borrador ya no existe o se pierde la conexión, el sistema DEBE redirigir al propietario a la lista de embarcaciones sin ningún aviso especial.
- **FR-022**: El sistema NO DEBE modificar ni eliminar las embarcaciones que el propietario ya tenía registradas al cancelar o suspender un registro.

### Requisitos no funcionales

- **NFR-001**: El modal de confirmación DEBE mostrarse en menos de 1 segundo después de que el propietario haga clic en "Cancelar", en condiciones normales.
- **NFR-002**: Todos los textos de los modales y botones DEBEN mostrarse en español, exactamente como se indican en este documento.

### Objetivo de negocio

Evitar que el propietario pierda su avance de forma accidental, pidiéndole una confirmación consciente antes de salir del registro. Además, permitirle retomar el registro en una sesión futura sin repetir los datos básicos cuando decida suspenderlo.

### Entidades clave

- **Embarcación**: Vehículo fluvial que el propietario registra en el sistema. Cuando solo se completó el Paso 1 y el propietario sale desde el Paso 2, queda en estado Borrador con los datos básicos (nombre, matrícula legal, tipo y capacidad máxima).
- **Propietario**: Usuario que registra la embarcación. Tras cancelar o suspender, regresa a la lista de sus embarcaciones.

## Criterios de éxito _(obligatorio)_

### Resultados medibles

- **SC-001**: El propietario puede cancelar el registro desde cualquiera de los dos formularios en máximo 2 clics (el botón "Cancelar" y luego el botón de confirmación).
- **SC-002**: En el 100 % de las salidas desde el Paso 2, los datos del Paso 1 permanecen guardados y los datos de texto del Paso 2 se recuperan al retomar el borrador.
- **SC-003**: En el 100 % de las cancelaciones desde el Paso 1, no queda ninguna embarcación guardada.
- **SC-004**: El propietario llega a la lista de embarcaciones en menos de 1 segundo después de confirmar la cancelación o la salida, en condiciones normales.
- **SC-005**: En el 100 % de los casos en que el propietario hace clic en "Continuar registro", regresa al mismo formulario con los datos exactamente como los tenía (vacío o con lo ingresado), sin perder nada.
- **SC-006**: En el 100 % de las cancelaciones o suspensiones, no se modifica ni se elimina ninguna embarcación registrada previamente por el propietario.
- **SC-007**: El modal de confirmación aparece en menos de 1 segundo después de que el propietario hace clic en "Cancelar", en condiciones normales.
