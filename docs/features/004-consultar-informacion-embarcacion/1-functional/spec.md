# Feature Specification: Consultar Detalles de Embarcación

**Created**: 28 de octubre de 2026
**Last Updated**: 13 de noviembre de 2026

**Módulo**: Módulo 1 – Gestión de Flota (SeaShare)

**Actor**: Propietario (usuario autenticado), Administrador (usuario autenticado)

**Consumidores externos**: Módulo 2 – Operación de Reservas, mediante sus consultas "Ver detalle de embarcación" (datos sin estado operativo) y "Brindar Información de Estado Operativo" (solo estado).

**Objetivo de negocio**: Permitir que los propietarios y administradores consulten la información operativa, logística y de equipamiento completa de una embarcación registrada en la flota (identificación legal, tipo, capacidad, tarifa base, puerto y ubicación GPS, fotografía oficial y servicios incluidos), adaptando los botones de acción disponibles según el rol del usuario autenticado.

---

## Flujo y puntos de origen

El flujo de consulta se origina desde dos pantallas distintas según el rol del usuario:

- **Vista Propietario**: inicia al presionar el botón **"Ver Detalles"** en la tabla de la pantalla **"Mis Embarcaciones Registradas"** (definida en el spec "Registrar embarcación").
- **Vista Administrador**: inicia al presionar el botón **"Ver Detalles"** en la tabla de la pantalla **"Control General de Flota Fluvial"**.

**Consumo por parte del Módulo 2 (Reservas)**:

El Módulo 2 consume información de la embarcación mediante **dos consultas distintas**, cuyos contratos, contenido y reglas de acceso se definen exclusivamente en los specs del Módulo 2. Este spec solo declara la frontera entre ambas y el diccionario de estados compartido.

| Consulta del Módulo 2 | Qué consume | Dónde se define (normativo) | Alcance en este spec |
|---|---|---|---|
| **Ver detalle de embarcación** (vía `Proveer Información de Embarcación`) | Código, nombre, matrícula legal, tipo, capacidad máxima, puerto de salida con GPS, servicios incluidos y código del propietario. **Con fotografía y estado operativo (13 de noviembre), sin tarifa base.** | Specs del Módulo 2 | Solo referencia.  |
| **Brindar Información de Estado Operativo** | **Únicamente** el estado operativo | Spec del Módulo 2 y User Story 4 / FR-031 de este spec | El Módulo 1 expone el estado de solo lectura, en tiempo real y con el diccionario único de cuatro estados. |

Ninguna de las dos consultas usa la pantalla "Ver Detalles" de este spec ni se rige por FR-005, FR-006 ni NFR-001.

La matrícula legal se consume como dato; mostrarla o no corresponde al `Ver detalle de embarcación` del Módulo 2. La tarifa base y la fotografía no forman parte de este consumo: la tarifa la cotiza el Módulo 3 y la fotografía se mantiene solo en "Ver Detalles" del Módulo 1.

**Quién ve qué en esta pantalla**:

- El **Propietario** solo ve y consulta sus propias embarcaciones. Si intenta consultar una embarcación que no es suya, el sistema no carga ninguna información y muestra solo la pantalla de error (ver FR-005).
- El **Administrador** puede consultar cualquier embarcación de la flota.
- Las embarcaciones en estado **Borrador** no aparecen en la lista del propietario ni en la del administrador, y se tratan como inexistentes si se intenta consultarlas.

**Diferenciación visual por rol**:

- **Propietario**: la pantalla "Ver Detalles" muestra los botones **"Editar"**, el botón de mantenimiento y **"Eliminar"**.
- **Administrador**: la pantalla "Ver Detalles" muestra únicamente el botón de mantenimiento y **"Eliminar"**. El botón **"Editar"** no existe en su interfaz.
- **Botón de mantenimiento (ambos roles)**: se llama **"Enviar a Mantenimiento"** en todos los estados, excepto cuando la embarcación está en `Mantenimiento`, donde pasa a llamarse **"Terminar mantenimiento"** y ocupa el mismo lugar.

**Estados operativos (diccionario único para todos los specs)**:

| Estado | Descripción | ¿Se puede editar? | Botón de mantenimiento |
|---|---|---|---|
| Disponible | Visible para alquiler | Sí | Enviar a Mantenimiento |
| Reservado | Bloqueo temporal por proceso de pago | No | Enviar a Mantenimiento |
| En Navegación | Contrato activo y embarcación fuera del puerto | No | Enviar a Mantenimiento |
| Mantenimiento | Inhabilitada por reparaciones o adecuación | Sí | Terminar mantenimiento |

Estos cuatro nombres son los únicos válidos en la lista, en "Ver Detalles" y en "Editar". No se usan variantes como "En Reserva", "En Mantenimiento" o "En Mantenimiento/Limpieza". El Módulo 2 usa este mismo diccionario en su consulta de estado operativo.

**Referencia: pantalla "Control General de Flota Fluvial" (definida en su propio spec)**:

Es el origen de la vista del administrador. Los siguientes dos controles actúan sobre la tabla:

- **Filtro por propietario**: muestra solo las embarcaciones del propietario elegido.
- **Filtro por estado**: muestra solo las embarcaciones en el estado elegido.

Los filtros excluyen: las embarcaciones que no cumplen el criterio dejan de mostrarse, y los criterios se combinan (deben cumplirse todos). Al quitar un filtro vuelven a verse. **[S-22] [S-23]**

**Fuera de alcance de este spec**:

- Flujos de los botones "Editar" (spec "Editar información de embarcación"), "Enviar a Mantenimiento", "Terminar mantenimiento" y "Eliminar" (sus respectivos specs). **[S-24]** Aquí solo se especifica su presencia, ausencia y estado habilitado o deshabilitado en pantalla.
- Reglas, validaciones y modales de "Enviar a Mantenimiento", "Terminar mantenimiento" y "Eliminar". Este spec solo define el nombre del botón de mantenimiento según el estado de la embarcación y su estado habilitado o deshabilitado en pantalla (FR-026); las reglas de fondo viven en sus propios specs. Para el Propietario esos flujos ya están implementados; para el Administrador, por ahora los botones solo se muestran.
- Definición completa de los filtros y la paginación de las tablas de origen (cada una vive en el spec de su pantalla). Este spec solo describe el comportamiento de referencia de los filtros del administrador. "Volver" no conserva los filtros: la tabla se muestra en su estado por defecto.
- Barra de búsqueda de propietario por nombre o cédula de "Control General de Flota Fluvial": pertenece al spec "Consultar información de propietario". Es independiente de la tabla: escribir en ella no filtra ni reordena las embarcaciones.
- Las dos consultas del Módulo 2 (Reservas): "Ver detalle de embarcación" (datos sin estado operativo) y "Brindar Información de Estado Operativo" (solo estado). Su diseño, contenido, contratos, botones y reglas de acceso pertenecen a los specs del Módulo 2.
- Pantalla de inicio de sesión y su mensaje de sesión expirada (se coordina con el spec de inicio de sesión).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar detalles de embarcación como Propietario (Priority: P1)

Como propietario, quiero consultar la información completa de mis embarcaciones desde mi lista principal, para revisar sus datos operativos y logísticos y acceder a las acciones de gestión de mi rol.

**Why this priority**: Es la vista central de consulta del propietario y el punto de partida de las acciones de edición, mantenimiento y eliminación.

**Independent Test**: Ingresar a "Mis Embarcaciones Registradas" con un propietario, presionar "Ver Detalles" en embarcaciones en cada uno de los cuatro estados y verificar la información, los botones y el estado del botón "Editar".

**Acceptance Scenarios**:

1. **Scenario**: Acceso a detalles desde la lista del propietario
   - **Given** un propietario autenticado en "Mis Embarcaciones Registradas"
   - **When** hace clic en "Ver Detalles" de una de sus embarcaciones
   - **Then** el sistema muestra la pantalla "Ver Detalles" con la información completa de esa embarcación

2. **Scenario**: Información y botones del propietario
   - **Given** que "Ver Detalles" carga correctamente para un propietario
   - **When** se muestra la pantalla
   - **Then** aparecen el nombre de la embarcación, el badge de estado operativo, la fotografía oficial, "Matrícula Legal", "Tipo de Embarcación", "Capacidad máxima", "Tarifa base (COP)", "Puerto de atraque y ubicación GPS", "Latitud", "Longitud", los servicios incluidos (base y adicionales) y los botones "Editar", el botón de mantenimiento ("Enviar a Mantenimiento" o "Terminar mantenimiento" según el estado) y "Eliminar"

3. **Scenario**: "Editar" habilitado
   - **Given** que la embarcación está en estado "Disponible" o "Mantenimiento"
   - **When** el propietario observa la barra de botones
   - **Then** el botón "Editar" está habilitado y, al presionarlo, abre la pantalla de edición (spec "Editar información de embarcación")

4. **Scenario**: "Editar" deshabilitado
   - **Given** que la embarcación está en estado "Reservado" o "En Navegación"
   - **When** el propietario observa la barra de botones
   - **Then** el botón "Editar" aparece deshabilitado; no se muestran tooltips y, al pasar el mouse por encima, aparece un icono de prohibido (círculo rojo con línea diagonal)

5. **Scenario**: Botón de mantenimiento en una embarcación en mantenimiento
   - **Given** que la embarcación está en estado "Mantenimiento"
   - **When** el propietario observa la barra de botones
   - **Then** aparecen "Editar" (habilitado), "Terminar mantenimiento" en el lugar donde en otros estados aparece "Enviar a Mantenimiento", y "Eliminar"

6. **Scenario**: Botón de mantenimiento en los demás estados
   - **Given** que la embarcación está en estado "Disponible", "Reservado" o "En Navegación"
   - **When** el propietario observa la barra de botones
   - **Then** el botón de mantenimiento se llama "Enviar a Mantenimiento"; en "Reservado" y "En Navegación" aparece deshabilitado (spec "Enviar a Mantenimiento", FR-001), sin tooltip y con el icono de prohibido al pasar el mouse

7. **Scenario**: Regreso a la lista
   - **Given** que el propietario está en "Ver Detalles"
   - **When** hace clic en "Volver"
   - **Then** el sistema lo lleva a "Mis Embarcaciones Registradas"

8. **Scenario**: Embarcación ajena, inexistente, eliminada o en Borrador mediante enlace directo
   - **Given** que un propietario abre un enlace directo a una embarcación que no le pertenece, que no existe, que fue eliminada o que está en estado Borrador
   - **When** el sistema evalúa la solicitud
   - **Then** no se carga ninguna información de la embarcación y se muestra únicamente la pantalla de error con el mensaje "No se pudo cargar la información de la embarcación" y el botón "Volver a Mis Embarcaciones"

---

### User Story 2 - Consultar detalles de embarcación como Administrador (Priority: P1)

Como administrador, quiero consultar la información completa de cualquier embarcación de la flota desde la pantalla de administración y regresar a la tabla general.

**Why this priority**: Permite la supervisión global sin perder el contexto de trabajo en la tabla general.

> **Estado de implementación (10 de noviembre de 2026)**: historia 2 completa en lo que se refiere a la consulta: listado con filtro por propietario y filtro por estado (los filtros excluyen filas) y detalle. La barra de búsqueda de propietario no filtra la tabla; se rige por su propio spec. Decisión del usuario: "Volver" regresa a la tabla sin filtros.

**Independent Test**: En "Control General de Flota Fluvial", aplicar filtro por propietario y filtro por estado, verificar que la tabla muestra solo lo que cumple los criterios, abrir "Ver Detalles" y presionar "Volver" para comprobar que regresa a la tabla en su estado por defecto.

**Acceptance Scenarios**:

1. **Scenario**: Acceso a detalles desde el control general
   - **Given** un administrador en "Control General de Flota Fluvial"
   - **When** hace clic en "Ver Detalles" en cualquier fila
   - **Then** el sistema muestra "Ver Detalles" con los datos completos de esa embarcación

2. **Scenario**: Botones del administrador
   - **Given** que "Ver Detalles" se muestra para un administrador
   - **When** observa los botones de acción
   - **Then** aparecen el botón de mantenimiento ("Enviar a Mantenimiento" o "Terminar mantenimiento" según el estado) y "Eliminar", y el botón "Editar" no existe en la interfaz; no se muestra ninguna tarjeta del propietario (la pantalla es idéntica a la del Propietario salvo por "Editar")
   - **And** *(estado actual)* los dos botones solo se muestran; sus flujos del Administrador (motivo obligatorio, notificaciones) se definen en el spec de mantenimiento, historias 3 a 5

3. **Scenario**: Botón de mantenimiento en una embarcación en mantenimiento (administrador)
   - **Given** que "Ver Detalles" se muestra para un administrador y la embarcación está en estado "Mantenimiento"
   - **When** observa los botones de acción
   - **Then** aparecen "Terminar mantenimiento" y "Eliminar", y el botón "Editar" no existe en la interfaz; en cualquier otro estado, el botón de mantenimiento se llama "Enviar a Mantenimiento"

4. **Scenario**: Regreso a la tabla
   - **Given** que el administrador aplicó filtro por propietario y/o filtro por estado antes de abrir "Ver Detalles"
   - **When** hace clic en "Volver"
   - **Then** el sistema lo lleva a "Control General de Flota Fluvial" en su estado por defecto, sin filtros

5. **Scenario**: Recarga de la pantalla de detalles
   - **Given** que el administrador está en "Ver Detalles"
   - **When** recarga la pantalla y luego presiona "Volver"
   - **Then** la pantalla vuelve a consultar los datos al servidor y "Volver" lleva a la tabla en su estado por defecto

6. **Scenario**: Acceso por enlace directo
   - **Given** que un administrador abre "Ver Detalles" desde un enlace compartido
   - **When** presiona "Volver"
   - **Then** el sistema lo lleva a "Control General de Flota Fluvial" en su estado por defecto, sin filtros

7. **Scenario**: Embarcación inexistente, eliminada o en Borrador
   - **Given** que un administrador abre una embarcación que no existe, fue eliminada o está en estado Borrador
   - **When** el sistema evalúa la solicitud
   - **Then** muestra el mensaje "No se pudo cargar la información de la embarcación" con el botón "Volver", que lo lleva a "Control General de Flota Fluvial" en su estado por defecto

8. **Scenario**: El filtro por propietario excluye las demás embarcaciones (comportamiento de referencia)
   - **Given** un administrador en "Control General de Flota Fluvial" con embarcaciones de varios propietarios
   - **When** elige un propietario en el filtro
   - **Then** la tabla muestra solo las embarcaciones de ese propietario; al quitar el filtro vuelven a verse todas

9. **Scenario**: El filtro por estado excluye las demás embarcaciones (comportamiento de referencia)
   - **Given** un administrador en "Control General de Flota Fluvial" con embarcaciones en distintos estados
   - **When** elige un estado en el filtro
   - **Then** la tabla muestra solo las embarcaciones en ese estado; al quitar el filtro vuelven a verse todas

> **Nota**: la definición completa de esta pantalla (columnas, filtros y orden por defecto) se documenta en su propio spec.

---

### User Story 3 - Carga de datos, formato de campos y manejo de errores (Priority: P2)

Como usuario (propietario o administrador), quiero que los datos se muestren con un formato claro y que la pantalla responda con orden ante fallos de red, de imagen o de sesión.

**Why this priority**: Garantiza lectura clara de los datos clave y evita que la pantalla quede inutilizable ante fallos.

**Independent Test**: Simular respuestas exitosas, no encontradas, fallo de red y sesión vencida; verificar formato de tarifa y coordenadas, badge, imagen que no carga y botones de cada error.

**Acceptance Scenarios**:

1. **Scenario**: Carga inicial
   - **Given** que el usuario presiona "Ver Detalles"
   - **When** la información se está consultando
   - **Then** se muestra un estado de carga tipo esqueleto, sin spinners ni indicadores llamativos

2. **Scenario**: Datos siempre actualizados
   - **Given** que el propietario edita una embarcación y presiona "Aceptar" en el modal de éxito
   - **When** regresa a "Ver Detalles"
   - **Then** la pantalla consulta de nuevo al servidor y muestra los datos actualizados

3. **Scenario**: Formato de la tarifa base
   - **Given** una tarifa guardada de `180000`
   - **When** se muestra la tarjeta "Tarifa base (COP)"
   - **Then** se ve `$ 180.000`, con puntos de miles, sin decimales, sin el texto "COP" junto al valor y sin ninguna mención a periodicidad por hora o por día

4. **Scenario**: Formato del puerto y las coordenadas
   - **Given** un puerto con latitud 10.745 y longitud -74.7891
   - **When** se muestran las tarjetas "Puerto de atraque y ubicación GPS", "Latitud" y "Longitud"
   - **Then** la tarjeta del puerto muestra solo su nombre, en negrita; la tarjeta "Latitud" muestra `10.7450` y la tarjeta "Longitud" muestra `-74.7891`, siempre con 4 decimales y con signo

5. **Scenario**: Badge de estado
   - **Given** una embarcación en cualquiera de los cuatro estados
   - **When** se muestra el badge
   - **Then** contiene un círculo sólido a la izquierda del texto del estado, alineado con él, con los colores de los tokens oficiales del diseño

6. **Scenario**: Fotografía que no carga
   - **Given** que la fotografía oficial no está disponible o falla al cargar
   - **When** se muestra la tarjeta de la imagen
   - **Then** aparece un contenedor con la imagen por defecto de embarcación y el resto de la pantalla mantiene su disposición

7. **Scenario**: Sin servicios adicionales
   - **Given** una embarcación sin servicios adicionales seleccionados
   - **When** se muestra la sección de servicios
   - **Then** aparecen los servicios base (Capitán y Combustible) y, en el área de adicionales, el texto "Sin servicios adicionales seleccionados"

8. **Scenario**: Fallo de red o de servidor
   - **Given** que la consulta falla por error de red o de servidor
   - **When** termina el intento de carga
   - **Then** se muestra "No se pudo cargar la información de la embarcación" con los botones "Reintentar" y "Volver"; "Reintentar" vuelve al estado de carga y repite la consulta; si la sesión caducó mientras tanto, el sistema redirige al inicio de sesión en lugar de reintentar

9. **Scenario**: Sesión expirada durante la carga
   - **Given** que la sesión del usuario expiró antes o durante la carga inicial
   - **When** el servidor responde que no hay sesión válida
   - **Then** el sistema guarda la dirección actual como destino de retorno y redirige al inicio de sesión con el aviso "Tu sesión ha expirado. Por favor, ingresa de nuevo."; no se muestra el error de red

10. **Scenario**: Sesión expirada con la pantalla ya abierta
   - **Given** que la sesión expira mientras el usuario está en "Ver Detalles"
   - **When** presiona un botón de acción o intenta navegar
   - **Then** el sistema lo redirige al inicio de sesión

11. **Scenario**: Formato de la capacidad
   - **Given** una embarcación con capacidad máxima de 12 pasajeros
   - **When** se muestra la tarjeta "Capacidad máxima"
   - **Then** se lee `12 pasajeros`; si la capacidad es 1, se lee `1 pasajero`

---

### User Story 4 - Proveer el estado operativo al Módulo 2 (Priority: P1)

Como Módulo 2 (Reservas), necesito consultar únicamente el estado operativo de una embarcación en tiempo real, para validar si es apta para reserva antes de continuar con el proceso.

**Why this priority**: Es la verificación de disponibilidad que evita sobreventas y alquileres de embarcaciones ocupadas o en mantenimiento. Sin este dato, el Módulo 2 no puede confirmar una reserva.

**Independent Test**: Consultar el estado de embarcaciones en los cuatro estados, de una inexistente o en Borrador y con fallo de comunicación; verificar que solo se devuelve el estado y que no se entregan otros datos.

**Acceptance Scenarios**:

1. **Scenario**: Embarcación disponible
   - **Given** una embarcación registrada cuyo estado operativo es "Disponible"
   - **When** el Módulo 2 consulta únicamente su estado operativo
   - **Then** el sistema devuelve el estado "Disponible", sin otros datos de la embarcación

2. **Scenario**: Embarcación no disponible
   - **Given** una embarcación en estado "Reservado", "En Navegación" o "Mantenimiento"
   - **When** el Módulo 2 consulta únicamente su estado operativo
   - **Then** el sistema devuelve ese estado, sin otros datos; la decisión de "apto/no apto", el fail-safe y los mensajes corresponden al Módulo 2

3. **Scenario**: Embarcación inexistente, eliminada o en Borrador
   - **Given** un identificador que no existe, que fue eliminado o que está en estado Borrador
   - **When** el Módulo 2 consulta su estado operativo
   - **Then** el sistema responde recurso no encontrado (404) sin devolver ningún dato

4. **Scenario**: Falla de comunicación
   - **Given** que la consulta de estado no puede completarse
   - **When** termina el intento
   - **Then** el sistema responde error sin inventar un estado; el Módulo 2 aplica su bloqueo preventivo

5. **Scenario**: Estado en tiempo real
   - **Given** una consulta de estado operativo
   - **When** el sistema la responde
   - **Then** el estado corresponde al momento exacto de la consulta y no a un valor en caché

---

### User Story 5 - Proveer información de la embarcación al Módulo 2 (Priority: P1)

Como Módulo 2 (Reservas), necesito obtener los datos oficiales de una embarcación desde el Módulo 1, para mostrarlos en su catálogo y detalle y para validar la capacidad de pasajeros.

**Why this priority**: El Módulo 1 es la única fuente de verdad de la flota. Sin estos datos, el Módulo 2 no puede mostrar la ficha ni validar la capacidad antes de reservar.

**Independent Test**: Consultar embarcaciones de distintos tipos y capacidades; verificar que se entrega el contrato acordado y nunca estado, tarifa ni fotografía; probar inexistente/Borrador y falla de comunicación.

**Acceptance Scenarios**:

1. **Scenario**: Datos entregados correctamente
   - **Given** una embarcación registrada
   - **When** el Módulo 2 solicita su información
   - **Then** el sistema devuelve código, nombre, matrícula legal, tipo, capacidad máxima, fotografía, estado operativo, puerto de salida con GPS, servicios incluidos y código del propietario, sin tarifa base

2. **Scenario**: Embarcación inexistente, eliminada o en Borrador
   - **Given** un identificador que no existe, que fue eliminado o que está en estado Borrador
   - **When** el Módulo 2 solicita su información
   - **Then** el sistema responde recurso no encontrado (404) sin devolver ningún dato

3. **Scenario**: Falla de comunicación
   - **Given** que la consulta de información no puede completarse
   - **When** termina el intento
   - **Then** el sistema responde error sin inventar datos; el Módulo 2 aplica su bloqueo preventivo

4. **Scenario**: Datos en tiempo real
   - **Given** una consulta de información
   - **When** el sistema la responde
   - **Then** los datos corresponden al momento exacto de la consulta y no a un valor en caché

5. **Scenario**: La validación de capacidad corresponde al Módulo 2
   - **Given** los datos de capacidad máxima entregados por el Módulo 1
   - **When** el Módulo 2 valida el número de pasajeros
   - **Then** el Módulo 1 solo entrega el dato oficial; la validación y el rechazo por exceso de capacidad corresponden al Módulo 2

---

## Edge Cases

- **¿Qué pasa si la embarcación no tiene servicios adicionales?** Se muestran los servicios base y, en el área de adicionales, "Sin servicios adicionales seleccionados".
- **¿Qué pasa si las coordenadas guardadas tienen más de 4 decimales?** Se redondean a 4 decimales para mostrarlas. En condiciones normales ya llegan redondeadas desde "Registrar" y "Editar".
- **¿Qué pasa si una coordenada es negativa (Sur u Oeste)?** Se muestra con el signo menos antes del número.
- **¿Qué pasa si una coordenada tiene menos de 4 decimales (por ejemplo `10.745` o `11`)?** Se muestra con ceros de relleno hasta 4 decimales (`10.7450`, `11.0000`). **[S-04]**
- **¿Qué pasa si la tarifa es muy alta?** El máximo es $ 9.999.999.999 (límite de "Registrar", que rige para todo el módulo). La tarjeta debe mostrarlo completo sin cortarse, también a 360 px de ancho. **[S-05]**
- **¿Qué pasa si la capacidad es 1?** Se muestra `1 pasajero`, en singular. **[S-20]**
- **¿Qué pasa si el identificador de la embarcación no existe o tiene un formato inválido?** Se muestra la pantalla de error de FR-010, sin nada que reintentar (FR-013).
- **¿Qué pasa si falta algún dato obligatorio de la embarcación?** No debería ocurrir, porque "Registrar" los exige todos. Si llegara a ocurrir, el campo muestra "—" y el resto de la pantalla se muestra normalmente. **[S-11]**
- **¿Qué pasa si el estado de la embarcación cambia mientras el usuario mira la pantalla?** La pantalla no se actualiza sola; el estado se muestra tal como se consultó. El nombre del botón de mantenimiento tampoco cambia hasta volver a cargar. Las acciones posteriores (por ejemplo "Editar") revalidan el estado en el servidor. La excepción es el propio cambio de mantenimiento del Propietario: al confirmarse, la pantalla se redibuja con la embarcación que devuelve el servidor. **[S-14]**
- **¿Qué pasa si la sesión expira con la pantalla abierta?** Ver escenario 10 de la historia 3.
- **¿Qué pasa si se presiona "Reintentar" tras un fallo de red y la sesión expiró mientras tanto?** El sistema prioriza la redirección al inicio de sesión sobre el reintento (ver FR-011).
- **¿Qué pasa si se abre un modal desde esta pantalla (Mantenimiento o Eliminar) y se presiona Esc o se hace clic fuera?** El modal no se cierra; solo responde a sus botones. La regla se hereda de los demás specs. **[S-09]**
- **¿Qué pasa si el Módulo 2 consulta el estado de una embarcación en Borrador?** Se trata como inexistente: responde 404 sin devolver estado ni datos.
- **¿Qué pasa si el Módulo 2 consulta los datos de una embarcación?** Recibe la información definida en su consulta "Ver detalle de embarcación", sin el estado operativo; el estado se consulta por separado con "Brindar Información de Estado Operativo".
- **¿Qué pasa si el Módulo 2 necesita la tarifa base?** No la consume: la cotización y el total son responsabilidad del Módulo 3. La tarifa base se mantiene en "Ver Detalles" del Módulo 1.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Acceso y navegación

- **FR-001**: El sistema DEBE permitir abrir "Ver Detalles" desde la tabla de "Mis Embarcaciones Registradas" (Propietario) y desde "Control General de Flota Fluvial" (Administrador).
- **FR-002**: Al presionar "Volver", el sistema DEBE llevar al Propietario a "Mis Embarcaciones Registradas".
- **FR-003**: Al presionar "Volver", el sistema DEBE llevar al Administrador a "Control General de Flota Fluvial" en su estado por defecto, sin filtros, aunque hubiera aplicado alguno antes de abrir el detalle. **[S-03]**
- **FR-004**: "Volver" DEBE comportarse igual cuando el Administrador llega a "Ver Detalles" desde un enlace compartido o tras recargar la pantalla: siempre lleva a "Control General de Flota Fluvial" sin filtros.
- **FR-005**: Si un Propietario abre la pantalla "Ver Detalles" de una embarcación que no le pertenece, que no existe, que fue eliminada o que está en estado Borrador, el sistema NO DEBE cargar ninguna información de la embarcación y DEBE mostrar únicamente la pantalla de error con "No se pudo cargar la información de la embarcación" y el botón "Volver a Mis Embarcaciones". **[S-01] [S-02] [S-18]**
- **FR-006**: Si un Administrador abre una embarcación que no existe, fue eliminada o está en estado Borrador, el sistema DEBE mostrar "No se pudo cargar la información de la embarcación" con el botón "Volver" hacia "Control General de Flota Fluvial" en su estado por defecto. **[S-02]**
- **FR-007**: Si la sesión no es válida al cargar o al ejecutar una acción, el sistema DEBE guardar la dirección actual como destino de retorno y redirigir al inicio de sesión, mostrando "Tu sesión ha expirado. Por favor, ingresa de nuevo." (el aviso se coordina con el spec de inicio de sesión). El destino de retorno solo puede ser una dirección interna del propio sistema. Tras iniciar sesión, se vuelven a aplicar las reglas de acceso de FR-005 y FR-006. **[S-06]**
- **FR-008**: Cada vez que se abre "Ver Detalles", el sistema DEBE consultar la información al servidor y NO DEBE usar datos guardados de la lista ni de una visita anterior. **[S-07]**

#### Carga y errores

- **FR-009**: Mientras se obtienen los datos, el sistema DEBE mostrar un estado de carga tipo esqueleto, sin spinners ni indicadores llamativos.
- **FR-010**: Los botones de cada pantalla de error DEBEN ser los siguientes:

| Situación | Propietario | Administrador | Motivo |
|---|---|---|---|
| Inexistente, ajena, eliminada o Borrador | "Volver a Mis Embarcaciones" | "Volver" a Control General | No hay nada que reintentar |
| Fallo de red o de servidor | "Reintentar" y "Volver" | "Reintentar" y "Volver" | El problema puede ser momentáneo |

- **FR-011**: "Reintentar" DEBE regresar al estado de carga y repetir la consulta, sin límite de reintentos. Si al reintentar el servidor indica que la sesión no es válida, el sistema DEBE aplicar FR-007 (redirección al inicio de sesión) en lugar de mostrar el error de red. **[S-13]**
- **FR-012**: El mensaje de error "No se pudo cargar la información de la embarcación" DEBE mostrarse en texto plano, igual para ambos roles y para ambas situaciones de FR-010. El mensaje NO DEBE mencionar permisos ni propietarios ajenos. **[S-02]**
- **FR-013**: Un recurso no encontrado DEBE responder con el código estándar de recurso no encontrado (404). La embarcación ajena DEBE tratarse de la misma forma que la inexistente, para no revelar si el identificador existe. Un identificador con formato inválido (que no sea un UUID) DEBE tratarse también como no encontrado: la pantalla muestra el mismo error, sin opción de reintentar. **[S-01] [S-26]**

#### Información y formato

- **FR-014**: La pantalla "Ver Detalles" DEBE mostrar:
   - Nombre de la embarcación.
   - Badge de estado operativo (`Disponible`, `Reservado`, `En Navegación`, `Mantenimiento`).
   - Fotografía oficial.
   - Matrícula Legal.
   - Tipo de Embarcación (Lancha, Yate, Catamarán, Velero).
   - Capacidad máxima, con unidad (por ejemplo `12 pasajeros`).
   - Tarifa base (COP).
   - Puerto de atraque y ubicación GPS (solo el nombre del puerto).
   - Latitud (tarjeta propia).
   - Longitud (tarjeta propia).
   - Servicios incluidos (base y adicionales).

  **Disposición**: en pantallas anchas, la fotografía y la sección de servicios ocupan la columna izquierda y las tarjetas de datos, la derecha, en dos columnas (Matrícula Legal y Tipo de Embarcación; Capacidad máxima y Tarifa base); la tarjeta del puerto ocupa todo el ancho de su fila y "Latitud" y "Longitud" van lado a lado en la fila siguiente. En pantallas angostas todo se apila en vertical (NFR-004). **[S-25]**
- **FR-015**: El badge de estado DEBE incluir un círculo sólido a la izquierda del texto, alineado con la etiqueta. Los colores del badge y de su círculo DEBEN tomarse de los tokens oficiales de diseño y cumplir como mínimo el contraste AA. **[S-17]**
- **FR-016**: La tarjeta "Tarifa base (COP)" DEBE mostrar la tarifa con puntos de miles y sin decimales, precedida de `$ ` (por ejemplo `$ 950`, `$ 180.000`, `$ 1.500.000`, `$ 9.999.999.999`). El dato guardado es el entero; la máscara solo cambia cómo se ve. No se muestra el texto "COP" junto al valor, porque la etiqueta ya lo indica. NINGÚN texto de la pantalla DEBE mencionar periodicidad por hora ni por día. **[S-05]**
- **FR-017**: La tarjeta "Puerto de atraque y ubicación GPS" DEBE mostrar únicamente el nombre del puerto, en negrita. La latitud y la longitud DEBEN mostrarse en dos tarjetas separadas, "Latitud" y "Longitud", con signo y siempre con 4 decimales, usando el punto como separador decimal (por ejemplo `10.7450` y `-74.7891`). **[S-04] [S-25]**
- **FR-018**: La tarjeta "Capacidad máxima" DEBE mostrar un número entero seguido de la unidad (por ejemplo `12 pasajeros`; si la capacidad es 1, `1 pasajero`). **[S-10] [S-20]**
- **FR-019**: Los servicios base (Capitán y Combustible) DEBEN mostrarse marcados y fijos para toda embarcación. Los servicios adicionales DEBEN mostrar los seleccionados entre las cinco opciones posibles: Chalecos salvavidas, Nevera con hielo, Equipo de sonido, Equipo de pesca y Equipo de buceo. Si no hay ninguno, DEBE mostrarse "Sin servicios adicionales seleccionados".
- **FR-020**: La fotografía oficial DEBE tener un texto alternativo con el nombre de la embarcación. Si no está disponible o falla al cargar, DEBE mostrarse un contenedor con la imagen por defecto de embarcación sin alterar la disposición de la pantalla. **[S-15]**
- **FR-021**: Si algún dato obligatorio llegara vacío, el campo DEBE mostrar "—" y el resto de la pantalla DEBE seguir mostrándose. **[S-11]**

#### Botones de acción por rol

- **FR-022**: En la vista del Propietario, el sistema DEBE mostrar los botones "Editar", el botón de mantenimiento (FR-026) y "Eliminar", en ese orden. **[S-21]**
- **FR-023**: En la vista del Propietario, "Editar" DEBE estar habilitado cuando el estado es `Disponible` o `Mantenimiento`, y deshabilitado cuando es `Reservado` o `En Navegación`. Al estar habilitado, abre la pantalla de edición.
- **FR-024**: No se DEBEN mostrar tooltips en ningún botón. Al pasar el mouse sobre un botón deshabilitado DEBE aparecer un icono de prohibido (círculo rojo con línea diagonal). En pantallas táctiles, donde no existe el paso del mouse, el botón deshabilitado DEBE verse claramente deshabilitado y ser anunciado como no disponible por los lectores de pantalla. **[S-08]**
- **FR-025**: En la vista del Administrador, el sistema DEBE mostrar únicamente el botón de mantenimiento (FR-026) y "Eliminar", en ese orden. El botón "Editar" NO DEBE existir en la interfaz.
- **FR-026**: El botón de mantenimiento DEBE mostrarse para ambos roles con el nombre "Terminar mantenimiento" cuando el estado es `Mantenimiento`, y con el nombre "Enviar a Mantenimiento" en cualquier otro estado. DEBE estar habilitado en `Disponible` y `Mantenimiento` y deshabilitado en `Reservado` y `En Navegación`. "Eliminar" DEBE mostrarse para ambos roles, habilitado en `Disponible` y `Mantenimiento` y deshabilitado en los otros dos. Las demás reglas se definen en sus propios specs. **[S-24]**

#### Convenciones heredadas de otros specs

- **FR-027**: Cualquier modal que se abra desde esta pantalla DEBE cerrarse únicamente con sus botones explícitos, no al hacer clic fuera ni al presionar Esc. Esta regla se hereda de los specs "Registrar embarcación" y "Editar información de embarcación"; aquí se cita como referencia. **[S-09]**

#### Referencia: filtros de "Control General de Flota Fluvial"

- **FR-028**: En "Control General de Flota Fluvial", el filtro por propietario y el filtro por estado DEBEN excluir las embarcaciones que no cumplen el criterio, y los criterios DEBEN combinarse (se muestran solo las que cumplen ambos). La barra de búsqueda de propietario NO DEBE filtrar ni reordenar la tabla. El filtro por propietario ofrece solo a los propietarios que tienen embarcaciones registradas. Sin criterios, la tabla muestra todas las embarcaciones registradas, de la más reciente a la más antigua. Las opciones del filtro por estado son los cuatro estados del diccionario y "Todos"; Borrador no aparece. Si ninguna embarcación cumple, la tabla muestra "No se encontraron embarcaciones con esos filtros". La definición completa de la pantalla vive en su propio spec. **[S-22] [S-23]**

#### Interoperabilidad con el Módulo 2 (Reservas)

- **FR-029**: La pantalla "Ver Detalles" de este spec DEBE estar disponible únicamente para Propietario y Administrador. El Módulo 2 NO DEBE usar esta pantalla ni regirse por FR-005, FR-006 ni NFR-001.
- **FR-030**: El Módulo 2 DEBE obtener los datos de la embarcación mediante `Proveer Información de Embarcación`, invocado por `Ver detalle de embarcación`. El contrato incluye: código, nombre, matrícula legal, tipo, capacidad máxima, puerto de salida con GPS, servicios incluidos y código del propietario. NO incluye estado operativo, tarifa base ni fotografía. Su contenido, botones y reglas de acceso se definen en los specs del Módulo 2.
- **FR-031**: El sistema DEBE exponer a los consumidores internos (Módulo 2, sin autenticación entre módulos) una consulta de solo lectura que devuelva ÚNICAMENTE el estado operativo de la embarcación, usando el diccionario único de cuatro estados. La consulta NO DEBE devolver otros datos de la embarcación, NO DEBE usar caché y DEBE tratar las embarcaciones inexistentes, eliminadas o en Borrador como recurso no encontrado (404).
- **FR-032**: La decisión de "apto/no apto para reserva", el fail-safe ante fallas, los reintentos y los mensajes al usuario final corresponden al Módulo 2 y se definen en su spec. Este spec solo garantiza la entrega del estado operativo.
- **FR-033**: El sistema DEBE exponer a consumidores internos (Módulo 2, sin autenticación entre módulos) una consulta de solo lectura con los datos oficiales de la embarcación: código, nombre, matrícula legal, tipo, capacidad máxima, puerto de salida con GPS (con su zona horaria, fija `America/Bogota`), servicios incluidos (los dos base y los adicionales seleccionados), código y nombre del propietario y `captain_included` (siempre verdadero, porque el capitán es servicio base). DEBE incluir además la fotografía y el estado operativo actual (pasos 1 y 2 del registro, menos la tarifa) y NO DEBE devolver la tarifa base, porque la cotiza el Módulo 3 y el Módulo 2 no maneja dinero. NO DEBE usar caché y DEBE tratar las embarcaciones inexistentes, eliminadas o en Borrador como recurso no encontrado (404).

---

### Requisitos No Funcionales

- **NFR-001** (Seguridad): La consulta en esta pantalla DEBE exigir sesión iniciada. Estas reglas de acceso aplican a la pantalla "Ver Detalles" de este spec y no a las consultas del Módulo 2 ("Ver detalle de embarcación" y "Brindar Información de Estado Operativo"), que tienen sus propias reglas de acceso. El servidor DEBE decidir si el usuario puede ver la embarcación y no devolver ningún dato cuando el propietario consulta una que no es suya. El servidor NO DEBE distinguir entre embarcación ajena e inexistente en su respuesta. **[S-01]**
- **NFR-002** (Rendimiento): La pantalla DEBE mostrar los datos de texto en menos de 1,5 segundos en el 95% de los accesos, en condiciones normales de red. La fotografía no se incluye en esta medición. **[S-12]**
- **NFR-003** (Accesibilidad): Contraste mínimo AA en textos, badges y botones; los mensajes de error se anuncian a los lectores de pantalla; ningún estado se comunica solo con color.
- **NFR-004** (Responsive): La vista DEBE funcionar desde 360 px de ancho, apilando las tarjetas en vertical cuando sea necesario.
- **NFR-005** (Idioma): Todos los textos, mensajes y botones se muestran en español.
- **NFR-006** (Coherencia): Los nombres de estado, el formato de tarifa y el formato de coordenadas DEBEN ser idénticos en la lista, en "Ver Detalles" y en "Editar".

---

## Key Entities *(include if feature involves data)*

- **Embarcación**: Vehículo fluvial registrado en la plataforma.
   - *Atributos que se consultan en este flujo*: Nombre, Matrícula Legal (única), Tipo de Embarcación, Capacidad máxima (pasajeros), Tarifa base (entero en COP), Estado operativo, Puerto de atraque (nombre), Latitud y Longitud (con signo, hasta 4 decimales en el dato guardado), Fotografía oficial, Servicios base (Capitán, Combustible) y Servicios adicionales (subconjunto de cinco opciones).
   - *Estados*: Borrador (no visible ni consultable), Disponible, Reservado, En Navegación, Mantenimiento.
- **Propietario**: Usuario autenticado dueño de la embarcación.
- **Administrador**: Usuario autenticado con permiso de supervisión global de la flota. **[S-16]**
- **Módulo 2 (consumidor)**: consume los datos de la embarcación (código, nombre, matrícula legal, tipo, capacidad máxima, puerto de salida con GPS, servicios incluidos y código del propietario), con fotografía y estado operativo, sin tarifa base, mediante `Proveer Información de Embarcación`; y consulta por separado solo el estado operativo mediante "Brindar Información de Estado Operativo". Sus contratos y reglas se definen en los specs del Módulo 2.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 95% de los accesos autorizados a "Ver Detalles" muestran la información de texto completa en menos de 1,5 segundos. **[S-12]**
- **SC-002**: En el 100% de las vistas de Administrador, el botón "Editar" no existe en la interfaz.
- **SC-003**: En el 100% de los regresos del Administrador con "Volver" desde un detalle, la tabla se muestra en su estado por defecto, sin filtros.
- **SC-004**: El 100% de las tarifas se muestran con puntos de miles, sin decimales, con `$ ` al inicio y sin menciones a periodicidad.
- **SC-005**: El 100% de las coordenadas se muestran, en sus tarjetas "Latitud" y "Longitud", con signo y exactamente 4 decimales.
- **SC-006**: El 100% de los intentos de un propietario de consultar una embarcación ajena, inexistente, eliminada o en Borrador terminan en la pantalla de error, sin que se muestre ningún dato de la embarcación.
- **SC-007**: El 100% de las veces que el servidor indica sesión no válida durante la carga, el usuario es llevado al inicio de sesión con destino de retorno, sin mostrar el error de red.
- **SC-008**: El 100% de los nombres de estado mostrados coinciden con el diccionario único de este spec.
- **SC-009**: En el 100% de las vistas de una embarcación en `Mantenimiento`, el botón de mantenimiento se llama "Terminar mantenimiento"; en el 100% de las vistas de embarcaciones en los demás estados se llama "Enviar a Mantenimiento".
- **SC-010**: El 100% de las capacidades se muestran como número seguido de la unidad (`12 pasajeros`, `1 pasajero`).
- **SC-011**: El 100% de las consultas de estado que recibe el Módulo 2 devuelven únicamente uno de los cuatro estados del diccionario único, sin otros datos de la embarcación.
- **SC-012**: El 100% de las consultas de información que recibe el Módulo 2 devuelven el contrato acordado, con estado operativo y fotografía, y nunca la tarifa base.

---

---

## Historial de cambios

| Fecha | Cambio |
|---|---|
| 7 de noviembre de 2026 | La tarifa máxima mostrable pasa a $ 9.999.999.999, igual que en "Registrar" (FR-016 y caso borde de tarifa alta). |
| 7 de noviembre de 2026 | Latitud y longitud pasan a tarjetas separadas, según el wireframe (FR-014, FR-017, historia 1 escenario 2, historia 3 escenario 4, SC-005). La tarjeta del puerto muestra solo el nombre. Se documenta la disposición de la pantalla **[S-25]**. Un identificador con formato inválido se trata como no encontrado (FR-013) **[S-26]**. |
| 10 de noviembre de 2026 | La etiqueta del estado pasa de "En Mantenimiento/Limpieza" a "Mantenimiento" en todo el spec (decisión del usuario); el botón se escribe "Terminar mantenimiento". |
| 10 de noviembre de 2026 | FR-026 y escenario 6 de la historia 1: el botón de mantenimiento y "Eliminar" se deshabilitan en Reservado y En Navegación (spec "Enviar a Mantenimiento", FR-001). Se ajusta el "Fuera de alcance". |
| 10 de noviembre de 2026 | Historia 2 implementada (listado del Administrador y detalle sin "Editar", sin tarjeta de propietario; botones solo visibles). Decisiones del usuario: los filtros **excluyen** filas (escenarios 8 y 9, FR-028, referencia) y "Volver" regresa a la tabla **sin filtros** (escenarios 4 a 6, FR-003, FR-004, SC-003, historia 2, fuera de alcance). |
| 10 de noviembre de 2026 | Caso borde de cambio de estado: tras enviar o terminar mantenimiento, la pantalla se actualiza con la respuesta del servidor. |
| 13 de noviembre de 2026 | **Corrección el mismo día**: la consulta de información del Módulo 2 devuelve todo lo de los pasos 1 y 2 del registro **incluida la fotografía (`photo_url`) y el estado operativo (`operational_status`)**, y solo excluye la tarifa base (FR-033, historia 5 escenario 1, SC-012). La consulta de solo estado sigue aparte (FR-031). |
| 13 de noviembre de 2026 | Historias 4 y 5 implementadas según el contrato del Módulo 2 (`GET /api/v1/vessels/{vessel_id}` y `/operational-status`). Decisiones de Jose: se entrega solo lo que existe en el modelo (sin `length_feet`, `cabin_count` ni `owner.verified`); el catálogo en lote del Módulo 2 queda **fuera de este spec** hasta definirlo con ellos (incluye `photo_url`, que FR-033 prohíbe). FR-031 y FR-033: sin autenticación entre módulos. FR-033 concreta zona horaria, servicios, `captain_included` y nombre del propietario. Los nombres del estado en la respuesta son los del diccionario del Módulo 1 (`AVAILABLE`, `RESERVED`, `NAVIGATION`, `MAINTENANCE`); pendiente de avisar al Módulo 2, que usa `IN_NAVIGATION` y `MAINTENANCE_CLEANING`. |
| 11 de noviembre de 2026 | La barra de búsqueda de propietario deja de filtrar la tabla y pasa a abrir la ventana de resultados del spec "Consultar información de propietario"; se quita la búsqueda de la tabla (FR-028, referencia, historia 2). |
