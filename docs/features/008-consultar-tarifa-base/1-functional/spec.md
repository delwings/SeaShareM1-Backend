# Feature Specification: Proveer Tarifa Base de Embarcaciones

**Created**: 12 de noviembre de 2026
**Last Updated**: 13 de noviembre de 2026

## Registro de cambios

**13 de noviembre de 2026** (ajustes decididos por Jose al implementar):

| # | Cambio | Motivo |
|---|---|---|
| 1 | FR-011 y NFR-001 cambian: la consulta es **abierta**, sin autenticación ni API key entre módulos; el Módulo 1 no verifica quién consulta | Decisión de Jose: no se maneja seguridad entre módulos |
| 2 | FR-003 y escenario 5 cambian: la tarifa se entrega como **texto con dos decimales** (`"350000.00"`), el mismo valor guardado, sin puntos de miles, símbolo ni periodicidad | Formato del contrato de ejemplo acordado por Jose |
| 3 | FR-007 concreta los nombres: `rates` (cada uno con `boat_id` y `base_rate`) y `not_found` (lista de identificadores) | El spec no fijaba nombres |
| 4 | Se fija el endpoint: `POST /api/v1/boats/base-rates` con cuerpo `{"boat_ids": [...]}` | El spec no fijaba ruta ni método |
| 5 | FR-008: los identificadores se comparan sin distinguir mayúsculas; la respuesta sigue el orden de llegada sin repetidos | Concreción |
| 6 | FR-009: el `400` indica el campo (`boat_ids` o `boat_ids[i]`) con el formato de errores común | Consistencia con el resto de la API |

**Módulo**: Módulo 1 – Gestión de Flota (SeaShare)

**Actor**: Ninguno humano. Es una consulta interna de sistema a sistema.

**Consumidor interno**: Módulo 3, mediante su caso de uso "Brindar Tarifa Base", que es invocado por "Solicitar estimación para reserva" y "Brindar información de reserva".

**Objetivo de negocio**: Entregar al Módulo 3 la tarifa base de cada embarcación de un lote (una lista de una o varias embarcaciones que el Módulo 3 consulta en una sola solicitud), tal como la fijó su propietario, para que el Módulo 3 realice sus propios cálculos. El Módulo 1 es la única fuente de verdad de este dato.

---

## Flujo y frontera entre módulos

La tarifa base es el precio que el propietario fija para su embarcación. Es un dato obligatorio: sin tarifa base no se puede registrar la embarcación ("Registrar embarcación") ni guardar una edición ("Editar información de embarcación"). Por eso toda embarcación registrada siempre tiene tarifa base, **en cualquier estado operativo**, y solo cambia si el propietario la modifica en "Editar información de embarcación".

El Módulo 3 envía un lote de embarcaciones, cada una identificada por su identificador único (el dato que identifica de forma exclusiva a cada embarcación en el sistema), y el Módulo 1 entrega la tarifa base de cada una.

**Consulta interna, no pantalla**: esta consulta no usa la pantalla "Ver Detalles" ni se rige por sus reglas de acceso (FR-005, FR-006 ni NFR-001 del spec "Consultar Detalles de Embarcación").

**Fuera de alcance de este spec**:

- Cómo el Módulo 3 interpreta, ajusta o utiliza la tarifa base, incluyendo cualquier unidad de tiempo, recargo o tarifa dinámica.
- Cómo el Módulo 3 arma el lote y con qué fecha calcula sus tarifas.
- Qué hace el Módulo 3 con las embarcaciones que el Módulo 1 reporta como no encontradas.
- Cómo el Módulo 3 entrega la tarifa al Módulo 2. El Módulo 2 no consume la tarifa base desde el Módulo 1.
- Timeouts, reintentos y manejo de fallas del lado del Módulo 3.
- Definición y edición de la tarifa base (specs "Registrar embarcación" y "Editar información de embarcación").
- Mecanismo técnico de autenticación entre módulos.

---

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Proveer la tarifa base de un lote de embarcaciones al Módulo 3 (Priority: P1)

Como Módulo 3, necesito enviar un lote de identificadores de embarcaciones y recibir la tarifa base de cada una, para usarla como insumo de mis propios cálculos.

**Why this priority**: El Módulo 3 no puede estimar ni calcular valores sin la tarifa base. El Módulo 1 es la única fuente de verdad de este dato.

**Independent Test**: Enviar un lote con una embarcación, un lote con varias y un lote que mezcle embarcaciones registradas con otras inexistentes, eliminadas o en Borrador. Verificar que cada embarcación registrada recibe la tarifa guardada, en cualquiera de sus estados, y que las demás se reportan aparte sin inventar valores.

**Acceptance Scenarios**:

1. **Scenario**: Consulta exitosa de un lote
   - **Given** un lote de 3 embarcaciones registradas
   - **When** el Módulo 3 envía el lote
   - **Then** el sistema entrega las 3 tarifas, cada una con el identificador único de su embarcación y su tarifa base exacta, y la lista de embarcaciones no encontradas queda vacía

2. **Scenario**: Lote de una sola embarcación
   - **Given** un lote con una sola embarcación registrada
   - **When** el Módulo 3 envía el lote
   - **Then** el sistema entrega la tarifa de esa embarcación con el mismo formato que para un lote de varias

3. **Scenario**: La tarifa se entrega en cualquier estado operativo
   - **Given** un lote con cuatro embarcaciones, una en cada estado: "Disponible", "Reservado", "En Navegación" y "En Mantenimiento/Limpieza"
   - **When** el Módulo 3 envía el lote
   - **Then** el sistema entrega la tarifa de las cuatro; el estado no altera el valor ni excluye a ninguna

4. **Scenario**: Tarifa editada por el propietario
   - **Given** que el propietario cambió la tarifa de una embarcación de `150000` a `180000` y guardó los cambios
   - **When** el Módulo 3 envía un lote que incluye esa embarcación
   - **Then** el sistema entrega `180000` para esa embarcación

5. **Scenario**: Valor sin transformaciones
   - **Given** una tarifa base guardada de `180000`
   - **When** el Módulo 3 la consulta dentro de un lote
   - **Then** recibe el texto `"180000.00"`, sin puntos de miles, sin símbolo de moneda, sin ajustes y sin mención de periodicidad

6. **Scenario**: Lote con embarcaciones no encontradas
   - **Given** un lote de 5 embarcaciones, de las cuales 4 están registradas y 1 no existe, fue eliminada o está en estado Borrador
   - **When** el Módulo 3 envía el lote
   - **Then** el sistema entrega las 4 tarifas y reporta aparte la embarcación no encontrada, sin indicar el motivo y sin entregar `0` ni un valor inventado; las 4 tarifas no se ven afectadas

7. **Scenario**: Ninguna embarcación del lote es encontrada
   - **Given** un lote en el que ninguna embarcación existe, o todas están eliminadas o en Borrador
   - **When** el Módulo 3 envía el lote
   - **Then** el sistema entrega la lista de tarifas vacía y todas las embarcaciones del lote en la lista de embarcaciones no encontradas

8. **Scenario**: Embarcación repetida en el lote
   - **Given** un lote en el que el mismo identificador único aparece dos veces
   - **When** el Módulo 3 envía el lote
   - **Then** el sistema trata esa embarcación como una sola y la entrega una única vez

9. **Scenario**: Lote mal armado
   - **Given** un lote vacío, un lote con más de 100 embarcaciones o un lote con un identificador único con formato inválido
   - **When** el Módulo 3 envía el lote
   - **Then** el sistema rechaza la solicitud completa y no entrega ninguna tarifa

10. **Scenario**: Falla del sistema
   - **Given** un lote bien armado, pero el sistema no puede completar la consulta por un problema técnico
   - **When** termina el intento
   - **Then** el sistema responde con error para todo el lote, sin entregar tarifas parciales ni valores inventados; el manejo posterior corresponde al Módulo 3

11. **Scenario**: Valor en tiempo real
   - **Given** una consulta de tarifas
   - **When** el sistema responde
   - **Then** los valores son los guardados en el momento exacto de la consulta, no una copia anterior (sin caché)

---

## Edge Cases

- **¿Qué pasa si el propietario edita la tarifa mientras el Módulo 3 la está usando?** El Módulo 1 siempre entrega la tarifa guardada al momento de cada consulta. Si el Módulo 3 necesita conservar un valor para una operación en curso, esa decisión es suya. Nota: "Editar" está deshabilitado en estado `Reservado` y `En Navegación`.
- **¿Qué pasa si una embarcación del lote está en `Borrador`?** Se trata como inexistente: aparece en la lista de embarcaciones no encontradas, sin entregar ningún dato.
- **¿Qué pasa si una embarcación del lote está en `Reservado`, `En Navegación` o `En Mantenimiento/Limpieza`?** Su tarifa se entrega con normalidad. El estado no condiciona esta consulta.
- **¿Qué pasa si un identificador no existe, la embarcación fue eliminada o está en Borrador?** Los tres casos se reportan igual en la lista de embarcaciones no encontradas; el sistema no distingue el motivo.
- **¿Qué pasa si el sistema falla al consultar?** Se responde error para todo el lote. Nunca se entrega un resultado parcial como si fuera completo.
- **¿Qué pasa si el lote es de una sola embarcación?** Se procesa igual que cualquier otro lote.
- **¿Qué pasa si el Módulo 2 necesita la tarifa base?** No la consulta al Módulo 1: la recibe del Módulo 3, que se la entrega con su tarifa dinámica ya aplicada.

---

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer a consumidores internos autorizados (Módulo 3) una consulta de solo lectura que reciba un lote de embarcaciones, identificadas por su identificador único, y entregue la tarifa base de cada una. La consulta no modifica ningún dato.
- **FR-002**: Por cada embarcación encontrada, el sistema DEBE entregar únicamente su identificador único y su tarifa base. NO DEBE entregar estado operativo, tipo, capacidad, matrícula, puerto, servicios, fotografía ni datos del propietario.
- **FR-003**: La tarifa base DEBE entregarse como texto con dos decimales (por ejemplo `"350000.00"`) en pesos colombianos (COP), con el mismo valor con que está guardada. NO DEBE llevar puntos de miles, símbolo de moneda, ajustes, recargos ni mención de unidad de tiempo o periodicidad.
- **FR-004**: La consulta DEBE funcionar para embarcaciones en cualquiera de los cuatro estados operativos (`Disponible`, `Reservado`, `En Navegación`, `En Mantenimiento/Limpieza`). El estado NO DEBE alterar ni impedir la entrega de la tarifa.
- **FR-005**: La consulta DEBE entregar siempre la tarifa guardada en el momento de la consulta. Si el propietario la modificó con "Editar información de embarcación", DEBE entregar el valor nuevo. NO DEBE usar caché (copias anteriores).
- **FR-006**: Una embarcación del lote queda en la lista de **embarcaciones no encontradas** cuando se cumple cualquiera de estos casos: (a) su identificador único no existe; (b) la embarcación fue eliminada; (c) está en estado `Borrador`. Estos casos NO DEBEN generar error del lote, DEBEN reportarse sin indicar el motivo y de forma idéntica entre sí, y NO DEBEN entregar `0`, un valor por defecto ni un valor inventado.
- **FR-007**: Lo que el sistema entrega DEBE tener dos listas separadas: **tarifas** (identificador único y tarifa base de cada embarcación encontrada) y **embarcaciones no encontradas** (solo su identificador único). Cada embarcación válida del lote DEBE aparecer en exactamente una de las dos listas.
- **FR-008**: Si una misma embarcación aparece más de una vez en el lote, el sistema DEBE tratarla como una sola y entregarla una única vez.
- **FR-009**: El sistema DEBE rechazar la solicitud completa, sin entregar ninguna tarifa, cuando el lote esté vacío, tenga más de 100 embarcaciones (ver NFR-002) o incluya un identificador único con formato inválido.
- **FR-010**: Si el sistema no puede completar una consulta bien armada por un problema técnico, DEBE responder con error para todo el lote, sin tarifas parciales y sin valores inventados.
- **FR-011**: La consulta es de uso interno del Módulo 3 y no lleva autenticación. NO DEBE aparecer en ninguna pantalla del Propietario ni del Administrador.
- **FR-012**: Corresponden al Módulo 3 y se definen en su spec: qué embarcaciones incluir en el lote, el significado de la tarifa por unidad de tiempo, la tarifa dinámica, el tratamiento de las embarcaciones no encontradas, los reintentos, los tiempos de espera y la entrega de la tarifa al Módulo 2. Este spec solo garantiza la entrega de la tarifa base de cada embarcación del lote.

---

### Requisitos No Funcionales

- **NFR-001** (Seguridad): Sin autenticación entre módulos (decisión del proyecto). La consulta es de solo lectura y solo entrega identificador y tarifa (FR-002).
- **NFR-002** (Tamaño del lote): Un lote DEBE aceptar como máximo 100 embarcaciones por solicitud.
- **NFR-003** (Rendimiento): La consulta DEBE responder en menos de 1 segundo en el 95% de las solicitudes (en 95 de cada 100), incluso con un lote de 100 embarcaciones, en condiciones normales de red.
- **NFR-004** (Coherencia): La tarifa entregada DEBE ser el mismo valor que se muestra en "Ver Detalles" y en "Editar" (esas pantallas solo agregan formato visual) y el mismo que se guardó en "Registrar embarcación" o en la última edición.
- **NFR-005** (Trazabilidad): Los errores y las embarcaciones reportadas como no encontradas DEBEN quedar registrados en el sistema para diagnóstico, sin incluir datos de otras embarcaciones.

---

## Key Entities _(include if feature involves data)_

- **Embarcación**: Vehículo fluvial registrado en la plataforma.
   - _Atributos que se consultan en este flujo_: Identificador único y Tarifa base (entero positivo en COP, obligatorio al registrar y al editar).
   - _Estados_: Borrador (no consultable, se trata como inexistente), Disponible, Reservado, En Navegación, En Mantenimiento/Limpieza.
- **Lote de embarcaciones**: Lista de una o varias embarcaciones, identificadas por su identificador único, que el Módulo 3 envía en una consulta. No se guarda en el Módulo 1.
- **Módulo 3 (consumidor)**: Envía el lote, interpreta los valores recibidos, aplica su tarifa dinámica y entrega el resultado al Módulo 2. Su contrato y reglas se definen en los specs del Módulo 3.

---

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: En 95 de cada 100 consultas, con lotes de hasta 100 embarcaciones, el sistema responde en menos de 1 segundo.
- **SC-002**: El 100% de las tarifas entregadas coincide exactamente con el valor guardado de cada embarcación, sin ajustes ni transformaciones.
- **SC-003**: El 100% de las tarifas entregadas contiene únicamente el identificador único de la embarcación y su tarifa base.
- **SC-004**: El 100% de las embarcaciones registradas del lote recibe su tarifa, sin importar si están en `Disponible`, `Reservado`, `En Navegación` o `En Mantenimiento/Limpieza`.
- **SC-005**: El 100% de las embarcaciones inexistentes, eliminadas o en `Borrador` aparece en la lista de embarcaciones no encontradas, sin ningún otro dato y sin afectar a las demás embarcaciones del lote.
- **SC-006**: El 0% de las respuestas entrega `0`, un valor por defecto o un valor inventado como tarifa.
- **SC-007**: El 100% de las embarcaciones válidas de un lote aparece en exactamente una de las dos listas (tarifas o embarcaciones no encontradas).
- **SC-008**: Tras una edición de tarifa, el 100% de las consultas posteriores entrega el valor nuevo.
