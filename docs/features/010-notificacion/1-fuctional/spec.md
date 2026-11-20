# Feature Specification: Notificaciones del Propietario (campana)

**Created**: 11 de noviembre de 2026

**Módulo**: Módulo 1 – Gestión de Flota (SeaShare)

**Input**: Que el propietario vea en la plataforma los avisos que le llegan, empezando por el de una embarcación eliminada por un administrador, con el motivo.

---

## Alcance

**Incluye**
- Campana en la barra superior del **Propietario**, con el contador de notificaciones sin leer.
- Panel con la lista de notificaciones (las 50 más recientes), marcar una o todas como leídas.
- Notificación de eliminación por el administrador con el **motivo** que él escribió.
- Motivo obligatorio en el modal de eliminar del administrador.

**Fuera de alcance** (se definen con sus funcionalidades)
- Notificaciones de mantenimiento, de reseñas y de disponibilidad (los tipos ya existen en la base de datos, pero nada las crea todavía).
- Campana del Administrador.
- Actualización en tiempo real (push, WebSockets), correo o mensajes al celular.
- Borrar notificaciones, filtros y paginación.

---

## User Scenarios & Testing

### Historia 1 - Ver el motivo de una eliminación (Prioridad: P1)

Como propietario, quiero ver en la campana por qué un administrador eliminó mi embarcación, para entender la decisión.

**Independent Test**: Con un administrador, eliminar una embarcación escribiendo un motivo; entrar como su propietario y abrir la campana.

**Acceptance Scenarios**:

1. **Scenario**: Aviso con motivo
    - **Given** que un administrador eliminó la embarcación "Río Magdalena I" (CP-05-1042-A) con el motivo "Las fotos no corresponden a la embarcación"
    - **When** el propietario abre la campana
    - **Then** ve "Tu embarcación Río Magdalena I (CP-05-1042-A) fue eliminada por un administrador el 3 de noviembre de 2026. Motivo: Las fotos no corresponden a la embarcación" y la fecha y hora de la notificación

2. **Scenario**: Aunque no estuviera conectado
    - **Given** que el propietario no tenía sesión cuando se eliminó la embarcación
    - **When** inicia sesión
    - **Then** la campana muestra el contador con la notificación sin leer

3. **Scenario**: El propietario elimina su propia embarcación
    - **Given** que el propietario elimina su embarcación
    - **When** abre la campana
    - **Then** no aparece ninguna notificación nueva por eso

### Historia 2 - Administrador escribe el motivo (Prioridad: P1)

Como administrador, quiero indicar el motivo al eliminar una embarcación, para que el propietario sepa la razón.

**Acceptance Scenarios**:

1. **Scenario**: Motivo obligatorio
    - **Given** el modal "¿Eliminar embarcación?" del administrador
    - **When** presiona "Sí, eliminar" sin escribir el motivo
    - **Then** se muestra "Escribe el motivo de la eliminación." y no se elimina nada

2. **Scenario**: Límite
    - **Given** el campo "Motivo de la eliminación"
    - **When** escribe más de 300 caracteres
    - **Then** el campo no admite más y muestra el contador `n/300`

3. **Scenario**: El propietario no ve el campo
    - **Given** el modal de eliminar del propietario
    - **Then** no tiene campo de motivo

### Historia 3 - Gestionar las notificaciones (Prioridad: P2)

Como propietario, quiero marcar mis notificaciones como leídas, para saber cuáles ya vi.

**Acceptance Scenarios**:

1. **Scenario**: Marcar una
    - **Given** una notificación sin leer (resaltada)
    - **When** la toca
    - **Then** deja de estar resaltada y el contador baja en uno

2. **Scenario**: Marcar todas
    - **Given** varias sin leer
    - **When** presiona "Marcar todas como leídas"
    - **Then** el contador desaparece

3. **Scenario**: Sin notificaciones
    - **Given** un propietario sin notificaciones
    - **When** abre la campana
    - **Then** ve "No tienes notificaciones."

4. **Scenario**: Error al cargar
    - **Given** que falla la consulta con el panel abierto
    - **Then** ve "No se pudieron cargar las notificaciones." con "Reintentar"

### Edge Cases

- **Panel**: se cierra con Esc y al hacer clic fuera; no navega a ninguna pantalla (la embarcación eliminada ya no se puede abrir).
- **Privacidad**: un propietario solo ve y marca las suyas; intentar marcar una ajena responde "no encontrada".
- **Más de 50**: solo se muestran las 50 más recientes; el contador cuenta todas las no leídas.
- **Fallo al crear el aviso**: la eliminación se conserva igual; el aviso se pierde y queda en el registro del servidor.
- **Sesión vencida**: el cliente lleva al inicio de sesión, sin mostrar el error de la campana.

---

## Requirements

- **FR-001**: Al eliminar un administrador una embarcación, el sistema DEBE crear una notificación para su propietario con el texto de la historia 1, que incluye el nombre, la matrícula, la fecha (hora de Colombia) y el motivo.
- **FR-002**: El motivo DEBE ser obligatorio para el administrador, se recorta de espacios y tiene entre 1 y 300 caracteres; el servidor DEBE rechazar con error de validación (400, campo `reason`) un motivo vacío o más largo, sin eliminar nada.
- **FR-003**: El sistema NO DEBE crear notificación cuando el propietario elimina su propia embarcación.
- **FR-004**: La campana DEBE mostrarse solo al Propietario, con el contador de no leídas (máximo "99+").
- **FR-005**: El panel DEBE listar las 50 más recientes, de la más nueva a la más antigua, con el mensaje completo y la fecha; las no leídas resaltadas.
- **FR-006**: El contador DEBE actualizarse al cargar cada pantalla, al abrir el panel y cada minuto.
- **FR-007**: Tocar una no leída DEBE marcarla como leída; "Marcar todas como leídas" DEBE marcar todas. Repetir la acción no cambia nada.
- **FR-008**: Un usuario solo DEBE ver y marcar sus propias notificaciones; las ajenas responden "no encontrada".
- **FR-009**: Solo el rol Propietario DEBE poder consultar notificaciones; el Administrador recibe prohibido (403).

## Key Entities

- **Notificación**: destinatario, embarcación, tipo (`VESSEL_DELETED`, y los ya existentes `MAINTENANCE_NOTICE`, `REVIEW_REQUEST`, `VESSEL_AVAILABLE`), mensaje, leída o no y fecha de creación. El motivo se guarda dentro del mensaje.

## Success Criteria

- **SC-001**: El 100% de las eliminaciones del administrador generan una notificación con su motivo; el 0% de las del propietario.
- **SC-002**: El 100% de los intentos sin motivo, o con más de 300 caracteres, se rechazan sin eliminar.
- **SC-003**: El 100% de las notificaciones son visibles solo para su destinatario.
- **SC-004**: Marcar como leída actualiza el contador al instante.

---

## Historial de cambios

| Fecha | Cambio |
|---|---|
| 11 de noviembre de 2026 | Spec nuevo. No requiere migración: usa la tabla `notification` y el tipo `VESSEL_DELETED` (V8). |
