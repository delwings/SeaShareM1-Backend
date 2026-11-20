# Feature Specification: Enviar a Mantenimiento

**Created**: 2026-11-03  
**Última revisión**: 2026-11-10 (ver «Registro de cambios» al final)  
**Alcance implementado**: historias 1 y 2 (Propietario). Las historias 3, 4 y 5 (Administrador y notificaciones) quedan pendientes.
**Módulo**: Módulo 1 – Gestión de Flota y Activos P2P

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El propietario envía su embarcación a mantenimiento rutinario (Priority: P1)

Como propietario de una embarcación, quiero poder enviar mi vehículo a estado de mantenimiento de forma rápida y sin tener que dar explicaciones (por ejemplo, para limpiarlo o revisarlo), para evitar que me lleguen nuevas reservas mientras no esté en condiciones de ser usado.

**Why this priority**: Es la funcionalidad principal del día a día para los dueños. Sin esta opción, se arriesgan a recibir reservas sobre un vehículo que físicamente está sucio o en revisión mecánica, generando incumplimientos.

**Independent Test**: Se puede probar iniciando sesión como propietario, buscando una embarcación Disponible, haciendo clic en el botón "Enviar a Mantenimiento", confirmando en la ventana emergente, y comprobando que el estado cambie automáticamente a "En Mantenimiento" y quede bloqueada.

**Acceptance Scenarios**:

1. **Scenario**: Confirmación exitosa para enviar a mantenimiento
   - **Given** que el propietario está en la pantalla de detalles de su embarcación, la cual se encuentra en estado Disponible.
   - **When** hace clic en el botón "Enviar a Mantenimiento" y confirma la acción.
   - **Then** la etiqueta del estado cambia inmediatamente a Mantenimiento, el botón cambia su texto a "Terminar mantenimiento", el vehículo deja de estar disponible en el catálogo y no se pide ninguna justificación escrita.

   - **And** no aparece ninguna ventana de éxito: la pantalla se actualiza sola con el nuevo estado.
   - **And** el modal de confirmación muestra "¿Enviar a Mantenimiento?", el texto "La embarcación no recibirá reservas temporalmente." y los botones "Cancelar" y "Sí, enviar".

2. **Scenario**: El propietario se arrepiente y cancela la acción
   - **Given** que el propietario hace clic en el botón "Enviar a Mantenimiento".
   - **When** decide hacer clic en "Cancelar" en la ventana de confirmación.
   - **Then** la ventana emergente desaparece y el vehículo sigue estando Disponible.

3. **Scenario**: Embarcación ocupada
   - **Given** que la embarcación está en estado "Reservado" o "En Navegación"
   - **When** el propietario observa el botón "Enviar a Mantenimiento"
   - **Then** el botón aparece deshabilitado, sin tooltip, con el icono de prohibido al pasar el mouse

4. **Scenario**: El estado cambió mientras el propietario confirmaba
   - **Given** que la embarcación pasó a "Reservado" o "En Navegación" después de abrir la pantalla
   - **When** el propietario confirma "Sí, enviar"
   - **Then** el sistema no cambia nada y muestra el modal "No es posible enviar la embarcación a mantenimiento" con el estado actual y el botón "Recargar página"

5. **Scenario**: Error técnico
   - **Given** que ocurre una falla de red o del servidor (o pasan 15 segundos sin respuesta)
   - **When** el propietario confirma
   - **Then** el sistema muestra el modal "No se pudo enviar a mantenimiento" con los botones "Cerrar" y "Reintentar"; la embarcación no cambia de estado

---

### User Story 2 - El propietario devuelve su embarcación a disponibilidad (Priority: P1)

Como propietario, quiero poder indicar mediante un botón que la limpieza o revisión de mi embarcación ya terminó, para que vuelva a estar visible y pueda recibir reservas de clientes nuevamente.

**Why this priority**: Es el complemento necesario de la historia anterior. Además, cuando un barco termina un alquiler y el sistema automáticamente lo pone en "Mantenimiento", esta es la herramienta que usa el dueño para avisar que ya limpió el barco y está listo de nuevo.

**Independent Test**: Se puede probar entrando a los detalles de una embarcación que está en mantenimiento (ya sea porque el dueño la puso así o porque acaba de terminar un viaje), haciendo clic en "Terminar mantenimiento", y comprobando que el estado vuelva a Disponible.

**Acceptance Scenarios**:

1. **Scenario**: Finalización exitosa del mantenimiento
   - **Given** que el propietario visualiza su embarcación en estado Mantenimiento.
   - **When** hace clic en "Terminar mantenimiento" y confirma la acción.
   - **Then** el estado de la embarcación vuelve a cambiar a Disponible, haciéndose visible en el catálogo para nuevos clientes.

   - **And** el modal de confirmación muestra "¿Terminar Mantenimiento?", el texto "La embarcación podrá recibir reservas nuevamente." y los botones "Cancelar" y "Sí, terminar"; no aparece ventana de éxito.

2. **Scenario**: Suspensión del administrador
   - **Given** que la embarcación fue enviada a mantenimiento por el administrador
   - **When** el propietario intenta terminar el mantenimiento (por ejemplo, desde una pantalla desactualizada)
   - **Then** el sistema no cambia nada y muestra el modal "Embarcación suspendida por el administrador" con el texto "Esta embarcación fue suspendida por el administrador. Solo él puede habilitarla de nuevo." *(el botón "Solicitar revisión" pertenece a la historia 4, pendiente)*

3. **Scenario**: Reintento o cambio previo
   - **Given** que la embarcación ya está Disponible (otra pestaña o un reintento tras un corte)
   - **When** el propietario confirma "Sí, terminar"
   - **Then** el sistema responde con el estado actual sin error; igual para "Enviar a Mantenimiento" si ya está en mantenimiento

---

### User Story 3 - El administrador suspende una embarcación obligatoriamente (Priority: P1)

Como administrador general, quiero tener la autoridad para suspender una embarcación (enviarla a mantenimiento) si noto irregularidades o daños, escribiendo obligatoriamente un motivo, para asegurar la calidad de la plataforma y notificar al dueño del problema.

**Why this priority**: Es una función esencial para el control de calidad. El administrador debe poder intervenir y retirar del catálogo a los vehículos que no cumplan los estándares.

**Independent Test**: Se puede probar entrando como administrador, seleccionando una embarcación Disponible, haciendo clic en "Enviar a Mantenimiento", escribiendo un motivo, confirmando la acción, y verificando que al propietario le llegue una alerta.

**Acceptance Scenarios**:

1. **Scenario**: Suspensión operativa con motivo obligatorio
   - **Given** que el administrador visualiza una embarcación en estado Disponible.
   - **When** hace clic en "Enviar a Mantenimiento", escribe obligatoriamente la razón en el cuadro de texto y confirma.
   - **Then** el estado de la embarcación cambia a Mantenimiento, el vehículo sale del catálogo, y se envía automáticamente un aviso a la cuenta del propietario con el motivo.

2. **Scenario**: Motivo vacío
   - **Given** el modal de "Enviar a Mantenimiento" del administrador
   - **When** presiona "Sí, enviar" sin escribir el motivo
   - **Then** se muestra "Escribe el motivo del mantenimiento." y no se envía

3. **Scenario**: El administrador no puede terminar el mantenimiento del propietario
   - **Given** una embarcación que el propietario envió a mantenimiento
   - **When** el administrador abre su detalle
   - **Then** "Terminar Mantenimiento" está deshabilitado (cursor de bloqueo)

---

### User Story 4 - El propietario atiende la sanción y solicita revisión (Priority: P2)

Como propietario, quiero recibir un aviso claro cuando el administrador bloquee mi embarcación. Quiero leer su mensaje, arreglar el daño, y luego poder apretar un botón para avisarle al administrador que ya terminé y que por favor revise mi vehículo, ya que yo mismo no puedo quitar la sanción.

**Why this priority**: Mantiene la comunicación y garantiza que los bloqueos por seguridad no puedan ser saltados por el dueño sin supervisión.

**Independent Test**: Se prueba entrando como dueño, leyendo el aviso de suspensión, notando que en lugar de "Terminar mantenimiento" hay un botón "Solicitar revisión", y al hacerle clic y confirmar, verificando que el administrador reciba el aviso.

**Acceptance Scenarios**:

1. **Scenario**: Lectura del aviso del administrador
   - **Given** que el administrador acaba de suspender la embarcación de un propietario.
   - **When** el propietario entra a la plataforma, abre sus notificaciones y lee el motivo de la sanción.
   - **Then** ve el nombre de su embarcación y "Revisar completamente"; al presionarlo, el sistema le muestra el motivo exacto escrito por la administración (FR-016, FR-017).

2. **Scenario**: El propietario pide que revisen su arreglo
   - **Given** que el propietario va a la pantalla de su vehículo suspendido por el administrador.
   - **When** hace clic en el botón "Solicitar revisión" y confirma.
   - **Then** la pantalla sigue mostrando el estado Mantenimiento, pero se envía un aviso directo al administrador pidiendo que revise el caso.

---

### User Story 5 - El administrador aprueba o rechaza la reparación (Priority: P2)

Como administrador general, quiero recibir un aviso cuando el dueño diga que ya arregló su vehículo. Al revisarlo, quiero decidir si le devuelvo el estado a Disponible o si le sigo pidiendo más arreglos.

**Why this priority**: Cierra el ciclo de las sanciones. Garantiza que solo el administrador tenga el poder de levantar una restricción que él mismo impuso.

**Acceptance Scenarios**:

1. **Scenario**: El administrador aprueba y habilita la embarcación
   - **Given** que el administrador revisa la solicitud del propietario.
   - **When** hace clic en "Terminar mantenimiento".
   - **Then** el estado de la embarcación pasa a Disponible y el propietario recibe un aviso de que ya puede operar.

2. **Scenario**: El administrador rechaza y pide más arreglos
   - **Given** que el administrador revisa la solicitud pero considera que el problema persiste.
   - **When** hace clic en el botón de "Reportar nuevamente".
   - **Then** el estado sigue siendo Mantenimiento y se le envía un nuevo aviso al dueño repitiendo el ciclo.

---

### Edge Cases

- **¿Qué ocurre si la embarcación está ocupada por un cliente?**
  El sistema bloquea la acción. Tanto el propietario como el administrador solo pueden enviar una embarcación a mantenimiento si no está en estado "Reservado" ni "En Navegación".
- **¿Qué ocurre si el propietario intenta habilitar un barco suspendido por el administrador?**
  El sistema lo impide bloqueando los botones normales; el dueño ve el botón "Solicitar revisión" en lugar de "Terminar mantenimiento" (FR-014, FR-020).
- **¿Qué pasa si el propietario repite la acción (doble clic, reintento)?**
  Solo sale una solicitud por confirmación, y si el estado ya es el pedido el sistema lo acepta sin cambiar nada ni sobrescribir quién inició el mantenimiento.
- **¿Qué pasa si la embarcación fue eliminada o es de otro propietario?**
  El sistema responde que no existe (sin revelar si existe) y la pantalla lleva a "Mis Embarcaciones".
- **¿Qué pasa con la propiedad del vehículo durante el mantenimiento?**
  El dueño original no pierde acceso a la información de su barco ni se desvincula de él durante este proceso.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir usar los botones de mantenimiento manual solo si el barco no está Reservado ni En Navegación.
- **FR-002**: El sistema DEBE cambiar el estado a "Mantenimiento" inmediatamente cuando el Propietario lo solicite, sin pedirle motivos.
- **FR-003**: El sistema DEBE ocultar la embarcación del catálogo público mientras esté en estado de mantenimiento.
- **FR-004**: El sistema DEBE exigir al Administrador que escriba una justificación para poder suspender un barco.
- **FR-005**: El sistema DEBE enviar notificaciones visuales internas a los usuarios para comunicar motivos de sanción y solicitudes de revisión.
- **FR-006**: El sistema DEBE recordar quién inició el mantenimiento (si fue el dueño, el sistema automático tras un alquiler, o el administrador por sanción) para saber si le permite al dueño desbloquear el barco por sí mismo o si requiere aprobación.
- **FR-007**: El sistema DEBE permitir al Administrador elegir si levanta una sanción o si la mantiene y solicita nuevas reparaciones.

- **FR-008**: El sistema DEBE verificar, en este orden: sesión y rol Propietario, existencia y pertenencia (404), estado y origen (409), y por último cambiar el estado de forma atómica, de modo que una reserva simultánea no pueda quedar en mantenimiento.
- **FR-009**: Las respuestas 409 DEBEN informar el estado actual y, si está en mantenimiento, quién lo inició.
- **FR-010**: El botón de mantenimiento DEBE estar deshabilitado, sin tooltip, en Reservado y En Navegación; su texto es "Enviar a Mantenimiento" (Disponible) o "Terminar mantenimiento" (Mantenimiento).
- **FR-011**: El estado se muestra con la etiqueta "Mantenimiento" en todo el sistema.
- **FR-012**: El Propietario solo cambia sus propias embarcaciones; el Administrador puede cambiar cualquiera con su propio flujo (FR-013 a FR-018).
- **FR-013**: **Solo termina el mantenimiento quien lo inició.** El Administrador solo termina el que él inició (origen `ADMIN`); el del Propietario (o el automático, origen vacío) lo termina el Propietario. Si lo intenta el otro, el servidor responde 409 con `maintenance_by`.
- **FR-014**: Quien no inició el mantenimiento no ve "Terminar" habilitado. Para el Administrador, cuando lo inició el Propietario o el sistema, el botón "Terminar Mantenimiento" se ve deshabilitado (cursor de bloqueo, sin tooltip). Para el Propietario, cuando lo inició el Administrador, el botón es "Solicitar revisión" (FR-020).
- **FR-015**: El Administrador DEBE escribir un motivo (obligatorio, 1 a 300 caracteres) al enviar a mantenimiento; sin él no se envía y se muestra "Escribe el motivo del mantenimiento." El servidor responde 400 con el campo `reason`.
- **FR-016**: Al enviar el Administrador a mantenimiento, el sistema DEBE crear un aviso `MAINTENANCE_NOTICE` para el propietario: "Tu embarcación [nombre] ([matrícula]) fue enviada a mantenimiento por un administrador el [fecha]. Motivo: [motivo]". Repetir la acción no lo duplica; si el aviso falla, el cambio se conserva.
- **FR-017**: En la campana el aviso muestra el nombre de la embarcación, "El administrador ha enviado una notificación sobre el estado de la embarcación." y el botón "Revisar completamente", que abre "Detalle de Notificación" (embarcación afectada con su estado, motivo registrado por administración y "Entendido y cerrar").
- **FR-018**: Al terminar el Administrador su mantenimiento, el propietario recibe un aviso `VESSEL_AVAILABLE`: "Tu embarcación [nombre] ([matrícula]) fue habilitada por un administrador el [fecha]. Ya puede recibir reservas."
- **FR-020**: El botón "Solicitar revisión" (Propietario, embarcación suspendida por el Administrador) DEBE abrir "¿Enviar a revisión?" con el texto "Se enviará una notificación al administrador." y los botones "Cancelar" y "Sí, enviar". Al confirmar, la embarcación sigue En Mantenimiento, el Propietario ve "Solicitud enviada" y cada Administrador recibe un aviso `REVIEW_REQUEST`.
- **FR-021**: En la campana del Administrador ("Notificaciones Administrativas") el aviso muestra el nombre de la embarcación, "El propietario ha solicitado la revisión de la embarcación." y el botón "Ver detalles", que lo marca como leído y abre el detalle normal de la embarcación, donde puede verificar los cambios y presionar "Terminar Mantenimiento" (modal "¿Terminar Mantenimiento?").
- **FR-022**: Solicitar la revisión más de una vez mientras el Administrador no haya leído el aviso no lo duplica. Solo es posible si el Administrador envió la embarcación a mantenimiento; en otro caso el servidor responde 409.
- **FR-019**: El detalle de la embarcación (propietario y administrador) DEBE incluir `maintenance_by`.

### Key Entities

- **Embarcación**: Tiene un registro interno de "Origen del mantenimiento" (`maintenance_by`): `OWNER` (rutina del propietario), `ADMIN` (sanción) o vacío (sin origen: lo pone el sistema al terminar un alquiler). Con origen vacío u `OWNER` el propietario puede terminar el mantenimiento; con `ADMIN` no. Se borra al volver a Disponible.
- **Notificación**: Mensajes del sistema que guardan a quién van dirigidos, el texto de explicación y un enlace directo a la embarcación afectada.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las embarcaciones que pasen a mantenimiento quedan bloqueadas inmediatamente para nuevas reservas.
- **SC-002**: El 100% de las suspensiones iniciadas por un administrador obligan al propietario a pedir permiso, con cero casos donde el dueño logre saltarse el bloqueo por su cuenta.
- **SC-003**: Todas las notificaciones del sistema deben entregarse de forma inmediata en las pantallas de los usuarios afectados.

---

## Registro de cambios

**10 de noviembre de 2026** (revisión al implementar la parte del Propietario):

| # | Cambio | Motivo |
|---|---|---|
| 1 | La etiqueta del estado pasa de "En Mantenimiento/Limpieza" a "Mantenimiento" | Decisión de Jose al ver la pantalla |
| 2 | Se agregan textos exactos de los modales y la ausencia de modal de éxito | Wireframes entregados |
| 3 | Nuevos escenarios: embarcación ocupada, estado cambiado, error técnico, suspensión del administrador y reintento | Completar los casos no cubiertos |
| 4 | FR-008 a FR-012 y dos edge cases (repetición, embarcación ajena) | Orden de verificación, idempotencia y consistencia con Editar/Eliminar |
| 5 | Origen "vacío" = sistema (fin de alquiler) | El modelo físico solo tiene `OWNER` y `ADMIN`; evita cambiarlo. Si se quiere un valor `SYSTEM` habría que aprobarlo |
| 6 | Historias 3, 4 y 5 marcadas como pendientes | Se harán con la vista del Administrador y las notificaciones |

**12 de noviembre de 2026** (parte del Administrador, historias 3 y 5 en lo que respecta a terminar):

| # | Cambio | Motivo |
|---|---|---|
| 7 | Nueva regla FR-013/FR-014: solo termina quien inició el mantenimiento; el botón se ve pero deshabilitado para el otro rol | Decisión de Jose |
| 8 | El administrador escribe un motivo obligatorio (FR-015) y el propietario lo lee en la campana con "Revisar completamente" (FR-016/FR-017) | Wireframes del administrador |
| 9 | Al terminar el administrador, el propietario recibe "ya puede recibir reservas" (FR-018) | Historia 5, escenario 1 |
| 10 | Pendientes: "Solicitar revisión" del propietario (historia 4, escenario 2) y "Reportar nuevamente" del administrador (historia 5, escenario 2); el administrador no recibe avisos todavía | Fuera del alcance de esta entrega |
| 11 | El detalle expone `maintenance_by` (FR-019); sin migración nueva | Necesario para deshabilitar el botón |

**12 de noviembre de 2026** (segunda revisión, "Solicitar revisión"):

| # | Cambio | Motivo |
|---|---|---|
| 12 | FR-014 cambia: al Propietario de una embarcación suspendida por el Administrador ya no se le muestra "Terminar" deshabilitado, sino "Solicitar revisión" (FR-020) | Decisión de Jose |
| 13 | Nuevos FR-020 a FR-022: modal "¿Enviar a revisión?", campana del Administrador con "Ver detalles" y sin avisos duplicados | Wireframes entregados |
| 14 | La campana ahora también la tiene el Administrador ("Notificaciones Administrativas"); se agrega el modal "Solicitud enviada" tras confirmar | La solicitud necesita una respuesta visible; no estaba en el wireframe |
| 16 | FR-020 cambia: "Solicitar revisión" se puede usar **una sola vez** por mantenimiento; después el botón queda deshabilitado (cursor de bloqueo, sin tooltip) y el servidor ignora repeticiones sin crear avisos. Se reemplaza el "no duplicar mientras no esté leído" de FR-022 | Decisión de Jose (evitar avisos repetidos) |
| 17 | Migración V9: columna `vessel.review_requested BOOLEAN NOT NULL DEFAULT FALSE`; se reinicia cuando el administrador envía o termina el mantenimiento | Persistir la marca entre recargas y pestañas. Cambio de modelo físico aprobado por Jose |
| 15 | Pendiente: "Reportar nuevamente" del Administrador (historia 5, escenario 2) | No solicitado |
