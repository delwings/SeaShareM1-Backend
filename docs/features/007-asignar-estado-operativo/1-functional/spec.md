# Feature Specification: Asignar Estado Operativo

**Created**: 2026-11-03
**Módulo**: Módulo 1 – Gestión de Flota y Activos P2P
**Last Updated**: 13 de noviembre de 2026

## Registro de cambios

**13 de noviembre de 2026** (decisiones de Jose al implementar):

| # | Cambio | Motivo |
|---|---|---|
| 1 | Los avisos se simulan con un endpoint REST que llama al mismo caso de uso que usará RabbitMQ; el consumidor real queda para la fase de integración (FR-002) | Cada módulo se presenta por separado por ahora |
| 2 | FR-001 y todo el spec usan «Mantenimiento» en vez de «Mantenimiento» (diccionario único del módulo) | Coherencia con los demás specs |
| 3 | Nuevo FR-008: nombres de estado aceptados y su equivalencia (M2 envía `IN_NAVIGATION` y `MAINTENANCE_CLEANING`) | Evitar errores de integración |
| 4 | Nuevo FR-009: solo se aplican las transiciones de los escenarios; las demás se ignoran, se registran y se confirman | El spec no definía transiciones |
| 5 | Nuevo FR-010: el mantenimiento por aviso queda sin origen, por lo que solo el propietario puede terminarlo | Historia 1, escenario 3 |
| 6 | FR-006 concreta qué cambios llegan al historial: los avisos aplicados y el mantenimiento del propietario y del administrador | Historial automático y manual |
| 7 | Migración V10: `vessel.status_changed_at`, `processed_event` y `vessel_status_history` | Cambio de modelo físico aprobado por Jose |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Actualización automática de la disponibilidad según las reservas (Priority: P1)

Como sistema de Gestión de Flota, quiero recibir e interpretar automáticamente los avisos del sistema de reservas para mantener actualizado el estado físico de las embarcaciones en tiempo real (si están disponibles, reservadas, navegando o en limpieza).

**Why this priority**: Es el puente principal entre la operación comercial y el inventario físico. Sin esto, un barco podría alquilarse dos veces para la misma fecha o aparecer como disponible cuando en realidad está en el agua con un cliente.

**Independent Test**: Se puede probar simulando el envío de mensajes desde el módulo de reservas informando distintos cambios (por ejemplo, que un pago se confirmó) y verificando que la embarcación en nuestro catálogo cambie su estado de inmediato sin que nadie deba hacerlo a mano.

**Acceptance Scenarios**:

1. **Scenario**: Bloqueo del barco al confirmarse un pago
   - **Given** una embarcación que se encuentra en estado Disponible en nuestro catálogo.
   - **When** nuestro sistema recibe un aviso de que la reserva se ha pagado y concretado.
   - **Then** el sistema actualiza automáticamente el estado del barco a Reservado, impidiendo que otras personas lo puedan alquilar.

2. **Scenario**: El cliente inicia su viaje
   - **Given** una embarcación que se encuentra en estado Reservado.
   - **When** nuestro sistema recibe un aviso de que el cliente hizo el abordaje y comenzó el servicio.
   - **Then** el sistema actualiza el estado del barco a En Navegación.

3. **Scenario**: El viaje termina y el barco necesita limpieza
   - **Given** una embarcación que se encuentra en estado En Navegación o Reservado.
   - **When** nuestro sistema recibe un aviso de que el servicio finalizó (o se canceló por un daño).
   - **Then** el sistema cambia el estado de la embarcación a Mantenimiento, dejándola bloqueada hasta que el dueño avise que ya está limpia y lista.

4. **Scenario**: Liberación del barco por cancelación de un cliente
   - **Given** una embarcación que se encuentra en estado Reservado.
   - **When** nuestro sistema recibe un aviso de que el cliente canceló su viaje de forma normal.
   - **Then** el sistema cambia el estado del barco de vuelta a Disponible, permitiendo que otros clientes lo vean en el catálogo.

---

### User Story 2 - Protección contra mensajes repetidos o desordenados (Priority: P1)

Como sistema de Gestión de Flota, quiero revisar la fecha exacta y el identificador de cada aviso que me llega, para ignorar mensajes que lleguen repetidos por fallas de internet o que correspondan a eventos viejos.

**Why this priority**: En los sistemas automáticos, a veces un mensaje se envía dos veces por error o llega tarde. Si no validamos esto, podríamos volver a bloquear un barco que ya había sido liberado.

**Independent Test**: Se puede probar enviando al sistema el mismo mensaje de aviso dos veces seguidas, o enviando un aviso con una fecha de ayer para un barco que hoy ya cambió de estado. El sistema debe ignorarlos sin mostrar errores graves.

**Acceptance Scenarios**:

1. **Scenario**: El sistema ignora un aviso repetido
   - **Given** que nuestro sistema ya recibió y procesó un aviso sobre un barco.
   - **When** llega un segundo aviso exactamente igual (mismo identificador único).
   - **Then** el sistema se da cuenta de que es un duplicado, lo ignora silenciosamente y mantiene la embarcación tal como está.

2. **Scenario**: El sistema ignora un aviso antiguo que llegó tarde
   - **Given** una embarcación cuyo estado se actualizó hoy a las 3:00 PM.
   - **When** llega un aviso informando un cambio que ocurrió a las 2:00 PM.
   - **Then** el sistema nota que la información es vieja, ignora el mensaje para no dañar el estado actual y guarda un registro de lo sucedido.

---

### Edge Cases

- **¿Qué pasa si llega un aviso para un barco que no está publicado o no existe?**
  El sistema ignora el cambio, guarda un reporte del error para que los técnicos lo revisen y confirma que recibió el mensaje para que no se lo sigan enviando.
- **¿Qué pasa si el módulo de reservas nos envía datos de dinero o cobros por error?**
  Nuestro sistema tiene prohibido manejar información financiera. Si el mensaje incluye precios o tarifas, el sistema simplemente ignora esos datos y solo lee qué estado operativo se debe asignar.
- **¿Qué pasa si nuestro sistema está apagado cuando llega el aviso?**
  El mecanismo de comunicación guarda el mensaje en una fila de espera. Apenas nuestro sistema vuelva a encenderse, leerá los mensajes pendientes en orden.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE administrar y ser la única fuente de verdad de los cuatro estados físicos de la embarcación: Disponible, Reservado, En Navegación y Mantenimiento.
- **FR-002**: El sistema DEBE contar con un receptor automático de mensajes trabajando en segundo plano para escuchar los avisos del sistema de reservas.
- **FR-003**: El sistema DEBE exigir que cada mensaje recibido incluya el barco afectado, el nuevo estado, la fecha y hora exacta del suceso y un identificador único para el mensaje.
- **FR-004**: El sistema DEBE guardar un registro de los identificadores de mensajes ya procesados para no aplicar un mismo cambio dos veces.
- **FR-005**: El sistema DEBE comparar la hora del mensaje nuevo con la hora del último cambio del barco (automático o manual); si el mensaje es más antiguo que el último cambio registrado, debe ser descartado y registrado en el log.
- **FR-006**: El sistema DEBE guardar un historial detallado de cada vez que cambia el estado de un barco, anotando estado anterior, estado nuevo, momento y si el cambio fue automático (por un aviso aplicado, con su identificador) o manual (Enviar a Mantenimiento y Terminar mantenimiento, del propietario o del administrador).
- **FR-008**: El estado del aviso DEBE aceptarse con los nombres de M2 y los del módulo: `AVAILABLE`, `RESERVED`, `IN_NAVIGATION` o `NAVIGATION`, `MAINTENANCE_CLEANING` o `MAINTENANCE`. Un aviso incompleto o con valores inválidos se rechaza con error de validación (400) indicando el campo.
- **FR-009**: Solo DEBEN aplicarse estas transiciones: Disponible→Reservado, Reservado→En Navegación, Reservado→Mantenimiento, En Navegación→Mantenimiento y Reservado→Disponible. Cualquier otra (por ejemplo sobre una embarcación en Mantenimiento por el propietario o el administrador) se ignora, se registra y se confirma. Un aviso con el estado que la embarcación ya tiene no cambia nada.
- **FR-010**: Al pasar a Mantenimiento por un aviso, la embarcación NO DEBE tener origen de mantenimiento; solo el propietario puede terminarlo.
- **FR-007**: El sistema NO DEBE guardar, procesar ni solicitar información de pagos, tarifas o multas dentro de este proceso de actualización de estados.

### Key Entities

- **Embarcación**: Representa el vehículo en nuestro catálogo. Debe saber cuál es su estado actual y guardar la fecha y hora exacta de la última vez que dicho estado cambió.
- **Registro de Avisos**: Una libreta de control interna del sistema donde se anotan los identificadores de los mensajes que ya se leyeron, para evitar repetirlos.
- **Historial de Estados**: Una bitácora que anota qué estado tenía el barco, a cuál cambió, cuándo lo hizo y por qué motivo (si fue por una reserva o por acción de un usuario).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los avisos recibidos cambian el estado de la embarcación en nuestro sistema en menos de medio segundo.
- **SC-002**: Cero casos de información corrupta por culpa de mensajes repetidos o desordenados.
- **SC-003**: Cero datos financieros guardados en la base de datos de Gestión de Flota durante este proceso.