# Feature Specification: Eliminar Embarcación

**Created**: 3 de noviembre de 2026

**Last Updated**: 11 de noviembre de 2026 (ver «Historial de cambios» al final)

**Módulo**: Módulo 1 – Gestión de Flota (SeaShare) **[S-18]**

**Actor**: Propietario (usuario autenticado), Administrador (usuario autenticado)

**Objetivo de negocio**: Permitir que el propietario elimine sus embarcaciones, y que el administrador elimine cualquier embarcación de la flota, cuando están en un estado que lo permite. La eliminación es **lógica**: para el usuario es definitiva y no se puede deshacer, pero el sistema conserva el registro como historial para fines de trazabilidad e inventario. Si quien elimina es un administrador, el propietario recibe un aviso en la plataforma.

> **Cómo leer este documento.** Las decisiones ya confirmadas están escritas como reglas. Las que son una recomendación mía, pendientes de confirmación, llevan una marca **[S-nn]** que remite a la tabla "Supuestos y decisiones pendientes" al final del documento. Si una recomendación no te convence, cámbiala allí y ajusta el requisito que la cita.

---

## Flujo y punto de origen

El flujo inicia cuando el usuario presiona el botón **"Eliminar"** en la pantalla **"Ver Detalles"** de una embarcación (definida en el spec "Consultar detalles de embarcación", donde solo se menciona la presencia del botón):

1. Aparece un modal de confirmación.
2. **"Conservar"** cancela la eliminación y deja al usuario en "Ver Detalles", sin cambios.
3. **"Sí, eliminar"** elimina la embarcación, muestra un mensaje de confirmación y lleva al usuario a su lista.

**Quién puede eliminar**:

- El **Propietario** elimina solo sus propias embarcaciones. Después de eliminar regresa a **"Mis Embarcaciones Registradas"**.
- El **Administrador** elimina cualquier embarcación de la flota de cualquier propietario. Después de eliminar regresa a **"Control General de Flota Fluvial"** en su estado por defecto, sin filtros. **[S-02]**
- Los dos roles siguen exactamente las mismas reglas por estado.

**Cuándo se puede eliminar (diccionario único de estados)**:

| Estado | ¿Se puede eliminar? | Botón "Eliminar" |
|---|---|---|
| Disponible | Sí | Habilitado |
| En Mantenimiento/Limpieza | Sí | Habilitado |
| Reservado | No | Deshabilitado |
| En Navegación | No | Deshabilitado |

**Qué significa "eliminación lógica"**:

- La embarcación **no aparece** en ninguna lista, no se puede consultar y **no está disponible** para alquiler.
- El registro **se conserva** en el sistema, marcado como eliminado, junto con la fecha de eliminación y quién la hizo (propietario o administrador). **[S-03]**
- "Eliminada" **no es un estado operativo**: no se agrega al diccionario de estados ni tiene badge. Es una marca aparte del registro. **[S-04]**
- La **matrícula legal queda libre** para usarse en otra embarcación. La unicidad de la matrícula solo se exige entre embarcaciones no eliminadas. **[S-05]**
- La fotografía y los servicios de la embarcación se conservan con el registro. **[S-13]**

**Aviso al propietario**: si quien elimina es un administrador, el propietario dueño de la embarcación recibe una **notificación en la plataforma**, en la campana que ya aparece en sus pantallas, con el texto "Tu embarcación [nombre] ([matrícula]) fue eliminada por un administrador el [fecha]." **[S-06] [S-07]**

**Fuera de alcance de este spec**:

- Cualquier pantalla de historial o inventario que consulte las embarcaciones eliminadas. Por ahora el registro solo se conserva. **[S-10]**
- El centro de notificaciones: la lista, el indicador de nuevas, marcar como leída y cualquier otro tipo de notificación. Hoy no existe un spec de notificaciones. **[S-11] [S-20]**
- Aviso por correo electrónico.
- Recuperar o restaurar una embarcación eliminada.
- Reservas futuras de una embarcación en el Módulo 2: este spec no las contempla. **[S-17]**
- Embarcaciones en estado Borrador: el spec "Registrar embarcación" ya define cómo se elimina un borrador.
- Los flujos de "Editar", "Enviar a Mantenimiento" y "Terminar Mantenimiento" (sus propios specs).
- La pantalla "Ver Detalles" y las tablas de origen (spec "Consultar detalles de embarcación").
- La pantalla de inicio de sesión y su mensaje de sesión expirada.

**Pantallas de referencia (Figma)**: (1) "Ver Detalles" con el botón "Eliminar" en rojo, (2) modal "¿Eliminar embarcación?". Los siguientes elementos no están en el Figma actual y se definen en este spec: modal de éxito, botón "Eliminar" deshabilitado, modales de estado cambiado, de embarcación inexistente y de error técnico, y la notificación al propietario.

**Colores**: este spec no define códigos de color. Los colores del botón, del modal y de la campana se toman de los tokens oficiales de la guía de diseño en Figma. El color nunca es el único indicador de un estado o de una advertencia.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Eliminar una embarcación como Propietario (Priority: P1)

Como propietario, quiero eliminar una de mis embarcaciones desde su pantalla de detalles, confirmando antes la acción, para retirarla de mi flota cuando ya no la voy a operar.

**Why this priority**: Es la funcionalidad central. Sin ella, una embarcación registrada no puede salir de la flota y su matrícula queda ocupada.

**Independent Test**: Con un propietario, abrir "Ver Detalles" de una embarcación `Disponible`, presionar "Eliminar", probar "Conservar" y luego "Sí, eliminar", y verificar el mensaje, la lista y que la matrícula quede libre.

**Acceptance Scenarios**:

1. **Scenario**: Abrir la confirmación
   - **Given** un propietario en "Ver Detalles" de una embarcación en estado "Disponible" o "En Mantenimiento/Limpieza"
   - **When** presiona "Eliminar"
   - **Then** aparece el modal de confirmación

2. **Scenario**: Contenido del modal
   - **Given** que el modal de confirmación está visible
   - **When** el propietario lo observa
   - **Then** muestra un icono de X en un círculo rojo, el título "¿Eliminar embarcación?", el texto "Esta acción eliminará el registro de forma permanente y liberará su matrícula asociada en el sistema.", la línea en rojo "Esta acción no se puede deshacer." y los botones "Conservar" y "Sí, eliminar"

3. **Scenario**: Conservar la embarcación
   - **Given** que el modal de confirmación está visible
   - **When** el propietario presiona "Conservar"
   - **Then** el modal se cierra, el propietario queda en "Ver Detalles" de la misma embarcación y no se modifica ningún dato

4. **Scenario**: Eliminar la embarcación
   - **Given** que el modal de confirmación está visible
   - **When** el propietario presiona "Sí, eliminar"
   - **Then** el sistema elimina la embarcación de forma lógica y muestra el modal "¡Embarcación eliminada!" con el texto "La embarcación ha sido eliminada del sistema." y el botón "Aceptar" **[S-01]**

5. **Scenario**: Regreso a la lista
   - **Given** que el modal "¡Embarcación eliminada!" está visible
   - **When** el propietario presiona "Aceptar"
   - **Then** el sistema cierra el modal y muestra "Mis Embarcaciones Registradas", donde la embarcación eliminada ya no aparece

6. **Scenario**: La embarcación eliminada no se puede consultar
   - **Given** que una embarcación fue eliminada
   - **When** alguien abre su enlace directo
   - **Then** el sistema la trata como una embarcación inexistente y muestra la pantalla de error definida en el spec "Consultar detalles de embarcación"

7. **Scenario**: Matrícula liberada
   - **Given** que una embarcación con matrícula "CP-05-1042-A" fue eliminada
   - **When** un propietario registra una embarcación nueva con esa misma matrícula
   - **Then** el sistema la acepta **[S-05]**

8. **Scenario**: El propietario no recibe aviso al eliminar la suya
   - **Given** que el propietario elimina su propia embarcación
   - **When** termina la eliminación
   - **Then** no se genera ninguna notificación; el propietario ya recibió el mensaje de éxito

---

### User Story 2 - Eliminar una embarcación como Administrador (Priority: P1)

Como administrador, quiero eliminar cualquier embarcación de la flota desde su pantalla de detalles, con la misma confirmación, para mantener el inventario actualizado y retirar embarcaciones que no deben seguir en la plataforma.

**Why this priority**: El administrador supervisa la flota global y necesita poder retirar embarcaciones de cualquier propietario.

**Independent Test**: Con un administrador, abrir "Ver Detalles" de una embarcación de otro propietario desde "Control General de Flota Fluvial", eliminarla y verificar el regreso a la tabla, donde ya no aparece.

**Acceptance Scenarios**:

1. **Scenario**: Botón disponible para el administrador
   - **Given** un administrador en "Ver Detalles" de una embarcación de cualquier propietario
   - **When** observa los botones de acción
   - **Then** ve el botón "Eliminar", habilitado o deshabilitado según el estado de la embarcación

2. **Scenario**: Mismo modal de confirmación
   - **Given** un administrador en "Ver Detalles" de una embarcación en "Disponible" o "En Mantenimiento/Limpieza"
   - **When** presiona "Eliminar"
   - **Then** aparece el mismo modal de confirmación del propietario, con los mismos textos y botones

3. **Scenario**: Conservar la embarcación
   - **Given** que el modal está visible para un administrador
   - **When** presiona "Conservar"
   - **Then** el modal se cierra y el administrador queda en "Ver Detalles" sin cambios

4. **Scenario**: Eliminar la embarcación
   - **Given** que el modal está visible para un administrador
   - **When** presiona "Sí, eliminar"
   - **Then** el sistema elimina la embarcación de forma lógica y muestra el modal "¡Embarcación eliminada!" con el botón "Aceptar" **[S-01]**

5. **Scenario**: Regreso a la tabla
   - **Given** que el administrador eliminó una embarcación desde su "Ver Detalles", sin importar si llegó desde la tabla o desde un enlace directo
   - **When** presiona "Aceptar" en el modal de éxito
   - **Then** regresa a "Control General de Flota Fluvial" en su estado por defecto, sin filtros, donde la embarcación eliminada ya no aparece **[S-02]**

---

### User Story 3 - Estados en los que se puede o no eliminar (Priority: P2)

Como usuario (propietario o administrador), quiero que el botón "Eliminar" solo esté activo cuando la embarcación se puede eliminar, para no intentar una acción que no es válida.

**Why this priority**: Protege a las embarcaciones que están reservadas o en navegación, donde eliminar afectaría a un contrato o a un pago en curso.

**Independent Test**: Abrir "Ver Detalles" de embarcaciones en cada uno de los cuatro estados, con ambos roles, y verificar el estado del botón y su comportamiento al pasar el mouse y al presionarlo.

**Acceptance Scenarios**:

1. **Scenario**: Botón habilitado
   - **Given** una embarcación en estado "Disponible" o "En Mantenimiento/Limpieza"
   - **When** el usuario observa el botón "Eliminar"
   - **Then** el botón está habilitado

2. **Scenario**: Botón deshabilitado
   - **Given** una embarcación en estado "Reservado" o "En Navegación"
   - **When** el usuario observa el botón "Eliminar"
   - **Then** el botón aparece deshabilitado, no se muestran tooltips y, al pasar el mouse por encima, aparece un icono de prohibido (círculo rojo con línea diagonal)

3. **Scenario**: Presionar el botón deshabilitado
   - **Given** que el botón "Eliminar" está deshabilitado
   - **When** el usuario intenta presionarlo
   - **Then** no ocurre nada y no se abre el modal

4. **Scenario**: Pantallas táctiles
   - **Given** un dispositivo táctil, donde no existe el paso del mouse
   - **When** el usuario observa el botón "Eliminar" deshabilitado
   - **Then** el botón se ve claramente deshabilitado y los lectores de pantalla lo anuncian como no disponible **[S-16]**

---

### User Story 4 - Avisar al propietario cuando un administrador elimina su embarcación (Priority: P2)

Como propietario, quiero enterarme de que un administrador eliminó una de mis embarcaciones, para no descubrirlo solo porque desapareció de mi lista.

**Why this priority**: La eliminación no se puede deshacer. Sin un aviso, el propietario no sabría qué pasó, cuál embarcación fue ni cuándo.

**Independent Test**: Con un administrador, eliminar una embarcación de un propietario y comprobar, iniciando sesión como ese propietario, que la campana muestra la notificación con el texto correcto. Eliminar luego una embarcación como propietario y comprobar que no se crea ninguna.

**Acceptance Scenarios**:

1. **Scenario**: Se genera la notificación
   - **Given** que un administrador elimina una embarcación de un propietario
   - **When** la eliminación se completa
   - **Then** el sistema crea una notificación para ese propietario con el texto "Tu embarcación [nombre] ([matrícula]) fue eliminada por un administrador el [fecha]." **[S-06] [S-07]**

   *(Implementado: se guarda en la tabla de notificaciones con el tipo propio `VESSEL_DELETED`; la fecha se escribe en hora de Colombia.)*

2. **Scenario**: El propietario la ve en la campana
   - **Given** que existe una notificación de eliminación para un propietario
   - **When** el propietario está conectado, o inicia sesión después
   - **Then** la ve en la campana de la plataforma, aunque no estuviera conectado cuando se eliminó la embarcación **[S-11]**

3. **Scenario**: Texto con dos embarcaciones del mismo nombre
   - **Given** un propietario con dos embarcaciones llamadas igual y de matrículas distintas
   - **When** el administrador elimina una de ellas
   - **Then** la notificación incluye la matrícula, para que el propietario sepa cuál fue **[S-06]**

4. **Scenario**: No se genera si elimina el propietario
   - **Given** que el propietario elimina su propia embarcación
   - **When** la eliminación se completa
   - **Then** no se crea ninguna notificación

5. **Scenario**: Falla del aviso
   - **Given** que un administrador elimina una embarcación y la creación de la notificación falla
   - **When** termina la operación
   - **Then** la embarcación queda eliminada y el administrador ve el mensaje de éxito; la eliminación no se revierte **[S-08]**

6. **Scenario**: Quién la ve
   - **Given** que se creó una notificación de eliminación
   - **When** se consulta quién puede verla
   - **Then** solo la ve el propietario dueño de la embarcación; el administrador que eliminó ve únicamente el mensaje de éxito **[S-19]**

---

### User Story 5 - Errores, cambios simultáneos y sesión (Priority: P2)

Como usuario, quiero que el sistema responda con claridad cuando algo cambia o falla mientras elimino, para no perder el control de lo que pasó con la embarcación.

**Why this priority**: La eliminación no se puede deshacer, así que el sistema debe evitar eliminar por error o dos veces, y explicar siempre lo que ocurrió.

**Independent Test**: Simular un cambio de estado con el modal abierto, una embarcación ya eliminada por otra sesión, fallo de red, doble clic y sesión expirada.

**Acceptance Scenarios**:

1. **Scenario**: El estado cambió con el modal abierto
   - **Given** que la embarcación pasó a "Reservado" o "En Navegación" mientras el modal de confirmación estaba abierto
   - **When** el usuario presiona "Sí, eliminar"
   - **Then** el sistema no elimina y muestra el modal "No es posible eliminar la embarcación" con el texto "La embarcación pasó a estado [Estado] mientras tanto. No es posible eliminarla." y el botón "Recargar página" **[S-12]**

2. **Scenario**: La embarcación ya fue eliminada
   - **Given** que otra sesión ya eliminó la embarcación
   - **When** el usuario presiona "Sí, eliminar"
   - **Then** el sistema no vuelve a eliminarla, no genera otra notificación y muestra el modal "Esta embarcación ya no existe" con el botón "Aceptar", que lleva a la lista del rol del usuario **[S-12]**

3. **Scenario**: Un propietario intenta eliminar una embarcación ajena
   - **Given** que un propietario intenta eliminar una embarcación que no es suya, por ejemplo manipulando una dirección
   - **When** el servidor evalúa la solicitud
   - **Then** no elimina nada y el propietario ve el mismo modal "Esta embarcación ya no existe", sin revelar que la embarcación existe **[S-09] [S-15]**

4. **Scenario**: Error técnico o tiempo agotado
   - **Given** que ocurre un fallo de red, un error del servidor o la petición supera los 15 segundos
   - **When** el usuario presiona "Sí, eliminar"
   - **Then** la solicitud se cancela y se muestra el modal "No se pudo eliminar la embarcación" con el texto "Ocurrió un problema técnico al eliminar la embarcación. Verifique su conexión a internet e inténtelo nuevamente." y los botones "Cerrar" y "Reintentar"; la embarcación se conserva sin cambios **[S-12] [S-14]**

5. **Scenario**: Reintentar o cerrar tras el error
   - **Given** que el modal de error técnico está visible
   - **When** el usuario presiona "Reintentar" o "Cerrar"
   - **Then** "Reintentar" repite la eliminación; si falla de nuevo se muestra el mismo modal tantas veces como falle; "Cerrar" deja al usuario en "Ver Detalles"

6. **Scenario**: Doble clic
   - **Given** que el usuario presionó "Sí, eliminar"
   - **When** la eliminación está en proceso
   - **Then** el botón queda deshabilitado, sin spinners ni tooltips y, al pasar el mouse, muestra el icono de prohibido; se envía una sola solicitud y la tecla Enter no la repite

7. **Scenario**: Sesión expirada
   - **Given** que la sesión del usuario expiró
   - **When** presiona "Eliminar" o "Sí, eliminar"
   - **Then** el sistema guarda la dirección actual como destino de retorno y lo redirige al inicio de sesión

8. **Scenario**: Cierre de los modales
   - **Given** que cualquier modal de este flujo está visible
   - **When** el usuario hace clic fuera de él o presiona Esc
   - **Then** el modal no se cierra; solo responde a sus botones explícitos

---

## Edge Cases

- **¿Qué pasa si el propietario y un administrador eliminan la misma embarcación casi al mismo tiempo?** El sistema acepta solo la primera. A la segunda le muestra "Esta embarcación ya no existe", y la notificación se genera una sola vez.
- **¿Qué pasa si un administrador elimina la embarcación mientras el propietario la está viendo?** La pantalla del propietario no se actualiza sola. Al presionar cualquier acción, o al recargar, ve el modal "Esta embarcación ya no existe" o la pantalla de error de "Ver Detalles".
- **¿Qué pasa con "Editar" o "Enviar a Mantenimiento" en una embarcación eliminada?** Se tratan como una embarcación inexistente.
- **¿Qué pasa si la matrícula liberada se usa de inmediato en un registro nuevo?** Se acepta, porque la unicidad solo cuenta embarcaciones no eliminadas. **[S-05]**
- **¿Qué pasa con la fotografía y los servicios de la embarcación eliminada?** Se conservan con el registro y no se muestran en ninguna pantalla. **[S-13]**
- **¿Qué pasa si la embarcación `Disponible` tiene reservas próximas en el Módulo 2?** Este spec no lo contempla; queda para el spec del Módulo 2. **[S-17]**
- **¿Qué pasa si se intenta eliminar una embarcación en estado Borrador?** No aplica: los borradores no aparecen en "Ver Detalles" y "Registrar" ya define cómo se eliminan.
- **¿Qué pasa si la sesión expira con el modal abierto?** Al presionar "Sí, eliminar", el sistema redirige al inicio de sesión sin eliminar.
- **¿Qué pasa si se repite una eliminación sobre una embarcación ya eliminada?** No cambia la fecha ni quién la eliminó, y no genera otra notificación.
- **¿Qué pasa si el nombre o la matrícula de la embarcación son largos?** El texto de la notificación se muestra completo, sin cortarse, también a 360 px de ancho.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Acceso y habilitación del botón

- **FR-001**: El sistema DEBE mostrar el botón "Eliminar" en "Ver Detalles" a los dos roles. El Propietario solo puede eliminar sus propias embarcaciones; el Administrador puede eliminar cualquier embarcación de la flota.
- **FR-002**: El botón "Eliminar" DEBE estar habilitado cuando el estado es `Disponible` o `En Mantenimiento/Limpieza`, y deshabilitado cuando es `Reservado` o `En Navegación`, para ambos roles.
- **FR-003**: No se DEBEN mostrar tooltips. Al pasar el mouse sobre el botón "Eliminar" deshabilitado DEBE aparecer un icono de prohibido (círculo rojo con línea diagonal). En pantallas táctiles el botón DEBE verse deshabilitado y anunciarse como no disponible a los lectores de pantalla. **[S-16]**
- **FR-004**: Presionar el botón "Eliminar" deshabilitado NO DEBE abrir el modal ni hacer ninguna acción.

#### Confirmación

- **FR-005**: Al presionar el botón "Eliminar" habilitado, el sistema DEBE mostrar un modal con un icono de X en un círculo rojo, el título "¿Eliminar embarcación?", el texto "Esta acción eliminará el registro de forma permanente y liberará su matrícula asociada en el sistema.", la línea en rojo "Esta acción no se puede deshacer." y los botones "Conservar" y "Sí, eliminar".
- **FR-006**: "Conservar" DEBE cerrar el modal y dejar al usuario en "Ver Detalles" de la misma embarcación, sin modificar ningún dato.
- **FR-007**: "Sí, eliminar" DEBE ejecutar la eliminación lógica de la embarcación.
- **FR-008**: Mientras la eliminación está en proceso, el botón "Sí, eliminar" DEBE permanecer deshabilitado, sin spinners ni tooltips; al pasar el mouse DEBE mostrar el icono de prohibido. La tecla Enter NO DEBE repetir la solicitud y DEBE enviarse una sola.

#### Eliminación lógica

- **FR-009**: La eliminación DEBE ser lógica: el registro de la embarcación se conserva en el sistema, marcado como eliminado. Una embarcación eliminada NO DEBE aparecer en ninguna lista, NO DEBE poder consultarse y NO DEBE estar disponible para alquiler.
- **FR-010**: Al eliminar, el sistema DEBE conservar la fecha de eliminación y quién la hizo (propietario o administrador). **[S-03]**
- **FR-011**: "Eliminada" NO DEBE agregarse al diccionario de estados operativos ni tener badge; es una marca aparte del registro. **[S-04]**
- **FR-012**: La matrícula legal de una embarcación eliminada DEBE quedar libre. La unicidad de la matrícula DEBE exigirse solo entre embarcaciones no eliminadas. **[S-05]**
- **FR-013**: La fotografía y los servicios de la embarcación DEBEN conservarse con el registro eliminado. **[S-13]**
- **FR-014**: Una embarcación eliminada DEBE tratarse como inexistente en "Ver Detalles", en las listas, en los filtros y en cualquier acción posterior.

#### Éxito y navegación

- **FR-015**: Tras una eliminación exitosa, el sistema DEBE mostrar el modal "¡Embarcación eliminada!" con el texto "La embarcación ha sido eliminada del sistema." y el botón "Aceptar". **[S-01]**
- **FR-016**: Al presionar "Aceptar", el sistema DEBE llevar al Propietario a "Mis Embarcaciones Registradas" y al Administrador a "Control General de Flota Fluvial" en su estado por defecto, sin filtros. **[S-02]**

#### Notificación al propietario

- **FR-017**: Si quien elimina es un administrador, el sistema DEBE crear una notificación para el propietario dueño de la embarcación con el texto "Tu embarcación [nombre] ([matrícula]) fue eliminada por un administrador el [fecha].", con la fecha escrita con el mes en minúscula (por ejemplo "3 de noviembre de 2026"). **[S-06] [S-07]**
- **FR-018**: Si quien elimina es el propietario, el sistema NO DEBE crear ninguna notificación.
- **FR-019**: La notificación DEBE mostrarse en la campana de la plataforma al propietario, aunque no estuviera conectado al momento de la eliminación. **[S-11]**
- **FR-020**: Si la creación de la notificación falla, la eliminación NO DEBE revertirse. **[S-08]**
- **FR-021**: Se DEBE crear una sola notificación por cada embarcación eliminada, y solo el propietario dueño DEBE poder verla. **[S-19]**

#### Errores y concurrencia

- **FR-022**: Antes de eliminar, el servidor DEBE verificar, en este orden y deteniéndose en la primera falla: (1) sesión activa, (2) la embarcación existe y no está eliminada, (3) el usuario puede eliminarla (el propietario solo las suyas; el administrador cualquiera), (4) el estado permite eliminar, y (5) elimina. **[S-09]**
- **FR-023**: Si el estado cambió a `Reservado` o `En Navegación`, el sistema NO DEBE eliminar y DEBE mostrar el modal "No es posible eliminar la embarcación" con "La embarcación pasó a estado [Estado] mientras tanto. No es posible eliminarla." y el botón "Recargar página". **[S-12]**
- **FR-024**: Si la embarcación ya fue eliminada, el sistema NO DEBE eliminarla de nuevo ni generar otra notificación, y DEBE mostrar el modal "Esta embarcación ya no existe" con el botón "Aceptar", que lleva a la lista del rol del usuario. **[S-12]**
- **FR-025**: Si un propietario intenta eliminar una embarcación que no es suya, el sistema NO DEBE eliminarla y DEBE mostrar el mismo modal "Esta embarcación ya no existe", sin revelar que existe. **[S-15]**
- **FR-026**: Si la solicitud falla por red o error del servidor, o supera los 15 segundos, el sistema DEBE cancelarla y mostrar el modal "No se pudo eliminar la embarcación" con "Ocurrió un problema técnico al eliminar la embarcación. Verifique su conexión a internet e inténtelo nuevamente." y los botones "Cerrar" y "Reintentar". La embarcación DEBE conservarse. "Reintentar" DEBE repetir la eliminación y "Cerrar" DEBE dejar al usuario en "Ver Detalles". **[S-12] [S-14]**
- **FR-027**: Si la sesión no es válida, el sistema DEBE guardar la dirección actual como destino de retorno, que solo puede ser una dirección interna, y redirigir al inicio de sesión.
- **FR-028**: Ningún modal de este flujo DEBE cerrarse al hacer clic fuera de él ni al presionar Esc; solo se cierran con sus botones explícitos.
- **FR-029**: Una solicitud repetida de eliminación sobre una embarcación ya eliminada NO DEBE cambiar la fecha ni el autor guardados, ni generar nuevas notificaciones.

---

### Requisitos No Funcionales

- **NFR-001** (Seguridad): La eliminación DEBE exigir sesión iniciada. El servidor DEBE decidir si el usuario puede eliminar la embarcación y NO DEBE depender de que el botón esté oculto o deshabilitado en pantalla. **[S-09]**
- **NFR-002** (Integridad): La eliminación y el registro de la fecha y de quién la hizo DEBEN guardarse juntos o no guardarse. No DEBEN generarse registros ni notificaciones duplicados por reintentos o clics repetidos.
- **NFR-003** (Rendimiento): La confirmación de la eliminación DEBE verse en menos de 2 segundos en el 95% de los casos en condiciones normales de red; la solicitud se cancela a los 15 segundos. **[S-14]**
- **NFR-004** (Accesibilidad): Los modales DEBEN anunciarse a los lectores de pantalla, mover el foco al abrirse y devolverlo al cerrarse; el contraste es AA como mínimo; la advertencia "Esta acción no se puede deshacer." y el botón "Eliminar" no DEBEN depender solo del color rojo; la navegación es completa con teclado.
- **NFR-005** (Responsive): El flujo DEBE funcionar desde 360 px de ancho, con botones de al menos 44 px de área táctil.
- **NFR-006** (Idioma): Todos los textos, mensajes y botones se muestran en español.
- **NFR-007** (Coherencia): Los nombres de estado, los formatos de fecha y las reglas de cierre de modales DEBEN ser iguales a los de los demás specs del Módulo 1.

---

## Key Entities *(include if feature involves data)*

- **Embarcación**: Vehículo fluvial registrado en la plataforma.
   - *Atributos que intervienen en este flujo*: Estado operativo (determina si se puede eliminar), matrícula legal (queda libre al eliminar), nombre, propietario, y la marca de eliminada con su fecha y quién la hizo.
   - *Marca de eliminada*: no es un estado operativo; indica que el registro se conserva pero no se muestra ni se ofrece. **[S-04]**
- **Propietario**: Usuario dueño de la embarcación; solo elimina las suyas y recibe la notificación si las elimina un administrador.
- **Administrador**: Usuario con permiso de supervisión global; puede eliminar cualquier embarcación.
- **Notificación**: Aviso para un propietario. Atributos: propietario destinatario, texto, fecha y si fue leída o no. Su gestión (lista, indicador, marcar como leída) se define en un spec aparte. **[S-11] [S-20]**

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100% de las embarcaciones en `Reservado` o `En Navegación`, el botón "Eliminar" aparece deshabilitado para ambos roles, y en el 100% de las que están en `Disponible` o `En Mantenimiento/Limpieza` aparece habilitado.
- **SC-002**: El 100% de las eliminaciones exigen la confirmación en el modal, y "Conservar" no modifica ningún dato.
- **SC-003**: El 100% de las embarcaciones eliminadas desaparecen de todas las listas y no se pueden consultar, pero su registro se conserva con fecha y autor de la eliminación.
- **SC-004**: El 100% de las matrículas de embarcaciones eliminadas quedan disponibles para un registro nuevo.
- **SC-005**: El 100% de las eliminaciones hechas por un administrador generan una notificación para el propietario dueño, con el texto definido, y el 0% de las hechas por un propietario la generan.
- **SC-006**: El 100% de los intentos de eliminar con el estado cambiado, con la embarcación ya eliminada o ajena terminan en el modal correspondiente, sin eliminar nada.
- **SC-007**: En el 100% de los casos con clics repetidos, se envía una sola solicitud y no se crean registros ni notificaciones duplicados.
- **SC-008**: Ante un error técnico, el 100% de las embarcaciones se conservan sin cambios.
- **SC-009**: El 100% de los modales de este flujo permanecen abiertos al presionar Esc o hacer clic fuera.
- **SC-010**: En el 95% de las eliminaciones, la confirmación aparece en menos de 2 segundos. **[S-14]**

---

---

## Historial de cambios

| Fecha | Cambio |
|---|---|
| 11 de noviembre de 2026 | Eliminar como administrador (historia 2) e historia 4 implementadas. Al presionar «Aceptar» el administrador vuelve a la tabla **sin filtros**, igual que «Volver» (decisión del usuario): cambian el alcance, la historia 2 (escenarios 5 y 6 se unen), FR-016 y [S-02]. |
| 11 de noviembre de 2026 | La notificación se guarda con el tipo nuevo `VESSEL_DELETED` (migración V8, solo amplía el ENUM). La fecha del texto usa la hora de Colombia. |
| 11 de noviembre de 2026 | **Pendiente**: el centro de notificaciones (campana, lista, indicador, marcar como leída; FR-019 en su parte visual) sigue fuera de alcance; hoy la notificación queda guardada y lista para mostrarse. |
| 11 de noviembre de 2026 | El administrador debe escribir un **motivo** (obligatorio, hasta 300 caracteres) en el modal de confirmación; el propietario no ve ese campo. El motivo se agrega al texto de la notificación: «… el [fecha]. Motivo: [motivo]». La campana de notificaciones queda definida en `notificaciones-spec.md`. |
