# Feature Specification: Consultar Información de Propietario

**Created**: 2 de noviembre de 2026  
**Last Updated**: 11 de noviembre de 2026 (ver «Historial de cambios» al final)

**Módulo**: Módulo 1 – Gestión de Flota (SeaShare) **[S-19]**

**Actor**: Administrador (usuario autenticado)

**Objetivo de negocio**: Permitir que el administrador busque a un propietario por su nombre o por su cédula y consulte su información de contacto e identificación, para supervisar la base de propietarios de la plataforma. La funcionalidad es **solo de consulta**: no permite modificar ningún dato.

> **Cómo leer este documento.** Las decisiones ya confirmadas están escritas como reglas. Las marcas **[S-nn]** que quedan remiten a supuestos del documento original. Los datos que el sistema aún no guarda (tipo de documento, fecha de registro y estado de cuenta) se quitaron de este spec el 11 de noviembre de 2026; ver «Historial de cambios».

---

## Flujo y punto de origen

El flujo se origina en la pantalla **"Control General de Flota Fluvial"** (vista del administrador), en la barra **"Buscar propietario por nombre o cédula..."**:

1. El administrador escribe un nombre o una cédula.
2. Debajo de la barra aparece una **ventana desplegable** con los propietarios que coinciden. Cada resultado trae un botón **"Ver Detalles"**.
3. Al presionar "Ver Detalles", el sistema muestra la pantalla **"Información del Propietario"**.
4. Desde esa pantalla, el botón **"Volver"** regresa a "Control General de Flota Fluvial" en su estado por defecto, sin filtros.

**Quién ve qué**:

- Solo el **Administrador** puede buscar propietarios y consultar su información.
- La búsqueda incluye a **todos los propietarios registrados**, tengan o no embarcaciones. Los administradores no aparecen: solo se buscan usuarios con rol de Propietario.

**Relación con otros specs**:

- La búsqueda es **independiente** de los filtros de la tabla de "Control General de Flota Fluvial". Escribir en la barra no filtra ni reordena la tabla. El filtro por propietario y el filtro por estado pertenecen al spec "Consultar detalles de embarcación" y **excluyen** filas; el filtro por propietario solo ofrece a los propietarios que tienen embarcaciones.

**Datos que muestra la pantalla**: solo los que el sistema guarda del usuario: nombre completo, número de documento, correo electrónico y teléfono de contacto. El sistema **no guarda** tipo de documento, fecha de registro ni estado de cuenta, por lo que esta pantalla no los muestra (ver «Historial de cambios»).

**Fuera de alcance de este spec**:

- Modificar cualquier dato del propietario (nombre, correo, teléfono o documento).
- Tipo de documento, fecha de registro y estado de cuenta con sus restricciones: el sistema no los guarda hoy; se agregarán cuando exista el flujo que los use (requiere ampliar el modelo de datos).
- La lista de embarcaciones del propietario y cualquier dato que no aparezca en la pantalla de referencia.
- Que un propietario consulte su propio perfil.
- Los filtros de la tabla de "Control General de Flota Fluvial" y la tabla misma (spec "Consultar detalles de embarcación").
- La pantalla de inicio de sesión y su mensaje de sesión expirada (se coordina con el spec de inicio de sesión).
- Vista y funcionalidades del Módulo 2 (Reservas).

**Pantallas de referencia (Figma)**: (1) "Control General de Flota Fluvial" con la ventana de resultados de búsqueda abierta, (2) "Información del Propietario". Diferencias con el Figma: la ventana de resultados muestra solo el número de documento (sin `C.C.`) y la pantalla no tiene el badge de estado, la fecha de registro ni el bloque "Estado de cuenta y restricciones". Los siguientes elementos no están en el Figma y se definen en este spec: estados de carga, ventana sin resultados, error de búsqueda, aviso de más resultados y pantalla de error de carga.

**Colores**: este spec no define códigos de color; se toman de los tokens oficiales de la guía de diseño, reutilizando los de los otros specs. **[S-22]**

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Buscar un propietario por nombre o cédula (Priority: P1)

Como administrador, quiero buscar a un propietario escribiendo su nombre o su cédula y ver los resultados en una ventana debajo de la barra, para llegar rápido a su información sin salir de "Control General de Flota Fluvial".

**Why this priority**: Es la puerta de entrada a la consulta de propietarios. Sin una búsqueda confiable no hay forma de llegar a su información.

**Independent Test**: En "Control General de Flota Fluvial", escribir parte de un nombre y luego una cédula, verificar los resultados de la ventana, los casos sin resultados y de error, y comprobar que la tabla no cambia.

**Acceptance Scenarios**:

1. **Scenario**: Barra de búsqueda disponible
   - **Given** un administrador en "Control General de Flota Fluvial"
   - **When** observa la pantalla
   - **Then** ve la barra con el texto de ayuda "Buscar propietario por nombre o cédula..."

2. **Scenario**: Resultados mientras escribe
   - **Given** que el administrador escribe al menos 3 caracteres en la barra y deja de escribir por unos instantes (300 ms)
   - **When** el sistema encuentra propietarios que coinciden
   - **Then** aparece debajo de la barra una ventana con el encabezado "RESULTADOS DE BÚSQUEDA (PROPIETARIO)" y, por cada resultado, un avatar con las iniciales, el nombre, el número de documento (por ejemplo `1042389104`) y el botón "Ver Detalles" **[S-04]**

3. **Scenario**: Menos de 3 caracteres
   - **Given** que el administrador ha escrito menos de 3 caracteres
   - **When** observa la pantalla
   - **Then** no se realiza la búsqueda y no se muestra la ventana de resultados **[S-04]**

4. **Scenario**: Búsqueda parcial por nombre
   - **Given** un propietario llamado "Fernando Fernández"
   - **When** el administrador escribe "fernan" (sin tilde y en minúsculas)
   - **Then** el propietario aparece en los resultados **[S-05]**

5. **Scenario**: Búsqueda por cédula
   - **Given** un propietario con documento 1042389104
   - **When** el administrador escribe "1042389104" o "1.042.389.104"
   - **Then** el propietario aparece en los resultados **[S-05]**

6. **Scenario**: Propietarios sin embarcaciones
   - **Given** un propietario que no tiene embarcaciones
   - **When** el administrador escribe su nombre o su cédula
   - **Then** el propietario aparece en los resultados como cualquier otro

7. **Scenario**: Mismo nombre
   - **Given** dos propietarios con el mismo nombre
   - **When** el administrador los busca
   - **Then** se muestran ambos resultados, cada uno con su número de documento, sin agruparlos ni eliminar ninguno

8. **Scenario**: Más de 5 coincidencias
   - **Given** que hay más de 5 propietarios que coinciden
   - **When** se muestra la ventana
   - **Then** aparecen 5 resultados en orden alfabético por nombre (y por número de documento si hay empate) y un aviso "Hay más resultados. Escribe más caracteres para precisar la búsqueda." **[S-06] [S-07]**

9. **Scenario**: Sin resultados
   - **Given** que ningún propietario coincide con lo escrito
   - **When** termina la búsqueda
   - **Then** la ventana muestra "No se encontraron propietarios con esa búsqueda." **[S-07]**

10. **Scenario**: Error de búsqueda
   - **Given** que la búsqueda falla por un problema de red o de servidor
   - **When** termina el intento
   - **Then** la ventana muestra "No se pudo realizar la búsqueda" con el botón "Reintentar", que repite la búsqueda **[S-07]**

11. **Scenario**: Mientras se busca
   - **Given** que la búsqueda está en curso
   - **When** la ventana está visible
   - **Then** muestra un estado de carga tipo esqueleto, sin spinners ni indicadores llamativos

12. **Scenario**: Cierre de la ventana
   - **Given** que la ventana de resultados está abierta
   - **When** el administrador presiona Esc, hace clic fuera de ella o borra el texto de la barra
   - **Then** la ventana se cierra **[S-08]**

13. **Scenario**: Uso con teclado
   - **Given** que la ventana de resultados está abierta
   - **When** el administrador usa las flechas arriba y abajo y presiona Enter sobre un resultado
   - **Then** el resultado se resalta con las flechas y Enter abre su "Información del Propietario" **[S-09]**

14. **Scenario**: La búsqueda no afecta la tabla
   - **Given** que el administrador escribe en la barra
   - **When** aparecen o cambian los resultados
   - **Then** la tabla de "Control General de Flota Fluvial", con sus filtros, no se filtra ni se reordena

---

### User Story 2 - Consultar la información de un propietario (Priority: P1)

Como administrador, quiero ver la información de contacto e identificación de un propietario, para conocer su situación en la plataforma sin poder modificarla.

**Why this priority**: Es el resultado de negocio de la funcionalidad: la información del propietario en una sola pantalla de solo lectura.

**Independent Test**: Desde la ventana de resultados, abrir propietarios con y sin embarcaciones, y verificar los datos, los formatos y la ausencia de botones de edición.

**Acceptance Scenarios**:

1. **Scenario**: Acceso desde un resultado
   - **Given** un administrador con la ventana de resultados abierta
   - **When** hace clic en "Ver Detalles" de un propietario
   - **Then** el sistema muestra la pantalla "Información del Propietario" con los datos de ese propietario

2. **Scenario**: Contenido de la pantalla
   - **Given** que la pantalla carga correctamente
   - **When** se muestra
   - **Then** aparecen el avatar con las iniciales, el nombre completo, el subtítulo "Propietario de Flota Fluvial" y los bloques "Correo electrónico", "Teléfono de contacto" y "Documento de identidad"

3. **Scenario**: Solo lectura
   - **Given** que la pantalla "Información del Propietario" está visible
   - **When** el administrador la observa
   - **Then** no hay ningún botón ni campo para editar datos; el único botón es "Volver"

4. **Scenario**: Propietario sin embarcaciones
   - **Given** un propietario que no tiene ninguna embarcación registrada
   - **When** se muestra su información
   - **Then** la pantalla se ve igual que la de cualquier otro propietario, con el mismo subtítulo "Propietario de Flota Fluvial"

5. **Scenario**: Formato de los datos
   - **Given** un propietario con teléfono 3104567890 y documento 1042389104
   - **When** se muestra su información
   - **Then** el teléfono se lee `+57 310 456 7890` y el documento se lee `1042389104`, sin puntos de miles; un teléfono que no tenga 10 dígitos se muestra tal como está guardado **[S-12]**

6. **Scenario**: Iniciales del avatar
   - **Given** un propietario llamado "Fernando Fernández"
   - **When** se muestra su avatar
   - **Then** las iniciales son "FF" **[S-10]**

---

### User Story 3 - Volver, carga, errores y acceso (Priority: P2)

Como administrador, quiero regresar a la tabla y que la pantalla responda con orden ante fallos de red, de sesión o de acceso, para no quedar bloqueado.

**Why this priority**: Garantiza que la consulta sea utilizable cuando algo falla y que los datos personales no se expongan a quien no corresponde.

**Independent Test**: Abrir un propietario, presionar "Volver" y simular fallo de red, propietario inexistente, sesión vencida y acceso de un usuario que no es administrador.

**Acceptance Scenarios**:

1. **Scenario**: Volver a la tabla
   - **Given** que el administrador está en "Información del Propietario"
   - **When** presiona "Volver"
   - **Then** regresa a "Control General de Flota Fluvial" en su estado por defecto: barra de búsqueda vacía, ventana de resultados cerrada y sin filtros en la tabla

2. **Scenario**: Carga inicial
   - **Given** que el administrador presiona "Ver Detalles" en un resultado
   - **When** la información se está consultando
   - **Then** se muestra un estado de carga tipo esqueleto, sin spinners ni indicadores llamativos

3. **Scenario**: Datos siempre actualizados
   - **Given** que el administrador abre "Información del Propietario"
   - **When** la pantalla se muestra
   - **Then** los datos provienen de una consulta nueva al servidor, no de datos guardados de una visita anterior ni de la ventana de resultados

4. **Scenario**: Fallo de red o de servidor
   - **Given** que la consulta de la información falla por error de red o de servidor
   - **When** termina el intento de carga
   - **Then** se muestra "No se pudo cargar la información del propietario" con los botones "Reintentar" y "Volver"; "Reintentar" vuelve al estado de carga y repite la consulta **[S-14]**

5. **Scenario**: Propietario inexistente
   - **Given** que se abre una dirección con un propietario que no existe, que corresponde a un administrador o cuyo identificador no tiene formato válido
   - **When** el sistema evalúa la solicitud
   - **Then** se muestra "No se pudo cargar la información del propietario" únicamente con el botón "Volver", porque no hay nada que reintentar **[S-14]**

6. **Scenario**: Sesión expirada durante la carga
   - **Given** que la sesión del administrador expiró antes o durante la carga
   - **When** el servidor indica que no hay sesión válida
   - **Then** el sistema redirige al inicio de sesión y no se muestra el error de red *(pendiente: el aviso "Tu sesión ha expirado. Por favor, ingresa de nuevo." y el destino de retorno se harán para todas las pantallas a la vez; ver plan general)*

7. **Scenario**: Sesión expirada con la pantalla ya abierta
   - **Given** que la sesión expira mientras el administrador está en la pantalla
   - **When** presiona un botón o intenta navegar
   - **Then** el sistema lo redirige al inicio de sesión

8. **Scenario**: Acceso de un usuario que no es administrador
   - **Given** que un usuario que no es administrador abre la dirección de "Información del Propietario"
   - **When** el sistema evalúa la solicitud
   - **Then** no se carga ningún dato del propietario y se muestra la pantalla de acceso no autorizado del sistema, con su botón hacia la pantalla principal del usuario **[S-15]**

---

## Edge Cases

- **¿Qué pasa si el nombre tiene una sola palabra?** El avatar muestra una sola inicial. **[S-10]**
- **¿Qué pasa si el nombre tiene tres o más palabras?** El avatar usa la inicial de la primera y de la última palabra. **[S-10]**
- **¿Qué pasa si se escriben espacios antes o después del texto buscado, o varios espacios seguidos?** Se ignoran. **[S-05]**
- **¿Qué pasa si la cédula se escribe con puntos, comas o espacios?** Se ignoran, y se busca solo por los dígitos. **[S-05]**
- **¿Qué pasa si se escribe un carácter especial en la barra?** Se trata como texto normal y no genera error.
- **¿Qué pasa si el texto buscado mezcla letras y números?** Se busca el texto como nombre y, si tiene dígitos, también esos dígitos como parte del documento.
- **¿Qué pasa si falta un dato del propietario (por ejemplo, el teléfono)?** El bloque muestra "—" y el resto de la pantalla se muestra normalmente. **[S-21]**
- **¿Qué pasa si los datos del propietario cambian mientras el administrador mira la pantalla?** La pantalla no se actualiza sola; muestra los datos tal como se consultaron.
- **¿Qué pasa si un administrador intenta buscarse o abrirse a sí mismo?** No aparece: la búsqueda solo incluye propietarios, y abrir su identificador responde "no encontrado".
- **¿Qué pasa si el propietario buscado no tiene embarcaciones?** Aparece en los resultados y su pantalla se muestra completa. Tampoco aparece en el filtro por propietario de la tabla, que solo ofrece a quienes tienen embarcaciones.
- **¿Qué pasa en una pantalla de 360 px?** La ventana de resultados ocupa el ancho de la barra y la lista se desplaza dentro de ella si hace falta; nada se corta. **[S-08]**

---

## Requirements *(mandatory)*

### Functional Requirements

#### Búsqueda de propietarios

- **FR-001**: El sistema DEBE mostrar, solo al administrador, la barra "Buscar propietario por nombre o cédula..." en "Control General de Flota Fluvial".
- **FR-002**: El sistema DEBE buscar mientras el administrador escribe, a partir de 3 caracteres, esperando 300 ms después de la última tecla para no consultar en cada pulsación. Con menos de 3 caracteres NO DEBE buscar ni mostrar la ventana de resultados. El servidor DEBE rechazar con error de validación (400) un texto de menos de 3 o de más de 100 caracteres. **[S-04]**
- **FR-003**: La búsqueda DEBE coincidir con cualquier parte del nombre o del número de documento, sin distinguir mayúsculas, tildes ni espacios sobrantes. En el documento DEBE ignorar además puntos, comas y espacios (se buscan solo los dígitos). **[S-05]**
- **FR-004**: Los resultados DEBEN mostrarse en una ventana desplegable debajo de la barra, con el encabezado "RESULTADOS DE BÚSQUEDA (PROPIETARIO)". Cada resultado DEBE incluir un avatar con las iniciales, el nombre, el número de documento (por ejemplo `1042389104`) y el botón "Ver Detalles".
- **FR-005**: La búsqueda DEBE incluir a todos los propietarios registrados, con o sin embarcaciones, y NO DEBE incluir a los administradores.
- **FR-006**: Cuando varios propietarios tengan el mismo nombre, el sistema DEBE mostrar todos los resultados, sin agruparlos ni eliminar ninguno.
- **FR-007**: La ventana DEBE mostrar como máximo 5 resultados, en orden alfabético por nombre (y por número de documento si hay empate); el servidor indica si había más. Si hay más coincidencias, DEBE mostrar el aviso "Hay más resultados. Escribe más caracteres para precisar la búsqueda." **[S-06] [S-07]**
- **FR-008**: Si no hay coincidencias, la ventana DEBE mostrar "No se encontraron propietarios con esa búsqueda." **[S-07]**
- **FR-009**: Si la búsqueda falla, la ventana DEBE mostrar "No se pudo realizar la búsqueda" con el botón "Reintentar", que repite la búsqueda. **[S-07]**
- **FR-010**: Mientras se busca, la ventana DEBE mostrar un estado de carga tipo esqueleto, sin spinners ni indicadores llamativos.
- **FR-011**: La ventana de resultados DEBE cerrarse con la tecla Esc, al hacer clic fuera de ella o al borrar el texto de la barra. Esta ventana no es un modal, por lo que no le aplica la regla de los otros specs de no cerrarse con Esc ni con clic fuera. **[S-08]**
- **FR-012**: La ventana DEBE poder recorrerse con las flechas arriba y abajo, y abrir el resultado resaltado con Enter. **[S-09]**
- **FR-013**: La búsqueda NO DEBE filtrar ni reordenar la tabla de "Control General de Flota Fluvial".
- **FR-014**: El nombre o la cédula buscados NO DEBEN viajar en la dirección de ninguna pantalla. **[S-16]**
- **FR-015**: La pantalla "Información del Propietario" DEBE abrirse únicamente desde el botón "Ver Detalles" de la ventana de resultados. El nombre de la columna "Propietario" de la tabla NO es un acceso a esta pantalla. **[S-17]**

#### Información del propietario

- **FR-016**: Al presionar "Ver Detalles" en un resultado, el sistema DEBE mostrar la pantalla "Información del Propietario" con los datos de ese propietario.
- **FR-017**: La pantalla DEBE mostrar un encabezado con el avatar con las iniciales, el nombre completo, el subtítulo fijo "Propietario de Flota Fluvial". El subtítulo es igual para todos los propietarios, tengan o no embarcaciones.
- **FR-018**: La pantalla DEBE mostrar los bloques "Correo electrónico", "Teléfono de contacto" y "Documento de identidad". El servidor NO DEBE entregar la contraseña ni otros datos internos.
- **FR-019**: La pantalla DEBE ser de solo lectura: NO DEBE ofrecer ningún botón ni campo para modificar datos. El único botón es "Volver".
- **FR-020**: Las iniciales del avatar (en la ventana de resultados y en la pantalla) DEBEN formarse con la primera letra de la primera palabra y la primera letra de la última palabra del nombre; si el nombre tiene una sola palabra, con una sola letra, siempre en mayúscula. **[S-10]**
- **FR-021**: Los datos DEBEN mostrarse con estos formatos: teléfono con prefijo del país y espacios (`+57 310 456 7890`; si no tiene 10 dígitos, tal como está guardado) y documento sin puntos de miles (`1042389104`). **[S-12]**
- **FR-022**: Si algún dato del propietario llegara vacío, el bloque DEBE mostrar "—" y el resto de la pantalla DEBE seguir mostrándose. **[S-21]**
- **FR-023**: Si el propietario no existe, el identificador no tiene formato válido o corresponde a un administrador, el servidor DEBE responder recurso no encontrado y la pantalla DEBE tratarlo como en FR-029.

#### Navegación, carga y errores

- **FR-025**: Al presionar "Volver", el sistema DEBE llevar al administrador a "Control General de Flota Fluvial" en su estado por defecto: barra de búsqueda vacía, ventana de resultados cerrada y sin filtros en la tabla.
- **FR-026**: Mientras se obtienen los datos, el sistema DEBE mostrar un estado de carga tipo esqueleto, sin spinners ni indicadores llamativos.
- **FR-027**: Cada vez que se abre "Información del Propietario", el sistema DEBE consultar la información al servidor y NO DEBE usar datos guardados de la ventana de resultados ni de una visita anterior.
- **FR-028**: Si la consulta falla por error de red o de servidor, el sistema DEBE mostrar en texto plano "No se pudo cargar la información del propietario" con los botones "Reintentar" y "Volver". "Reintentar" DEBE volver al estado de carga y repetir la consulta, sin límite de reintentos. **[S-14]**
- **FR-029**: Si el propietario no existe, el sistema DEBE responder con el código estándar de recurso no encontrado (404) y mostrar "No se pudo cargar la información del propietario" únicamente con el botón "Volver". **[S-14]**
- **FR-030**: Si la sesión no es válida al cargar o al ejecutar una acción, el sistema DEBE redirigir al inicio de sesión sin mostrar el error de red; también al reintentar. *(Pendiente, común a todas las pantallas: guardar la dirección como destino de retorno y mostrar "Tu sesión ha expirado. Por favor, ingresa de nuevo.")*
- **FR-031**: Si un usuario que no es administrador abre la pantalla, el sistema NO DEBE cargar ningún dato del propietario y DEBE mostrar la pantalla de acceso no autorizado del sistema; el servidor DEBE responder prohibido (403). **[S-15]**
- **FR-032**: El sistema DEBERÍA registrar quién consultó la información de un propietario y cuándo. Esta decisión requiere confirmación y posible revisión legal; **no está implementado**. **[S-18]**

---

### Requisitos No Funcionales

- **NFR-001** (Control de Acceso): La búsqueda y la consulta DEBEN validar el rol del usuario activo. El servidor DEBE restringir la entrega de datos personales completos únicamente a los usuarios con rol de Administrador.
- **NFR-002** (Rendimiento): Los resultados de búsqueda DEBEN aparecer en menos de 1 segundo y la pantalla del propietario DEBE mostrar sus datos en menos de 1,5 segundos, ambos en el 95% de los casos y en condiciones normales de red. **[S-20]**
- **NFR-003** (Accesibilidad): Contraste mínimo AA; la ventana de resultados y los mensajes de error se anuncian a los lectores de pantalla; navegación completa con teclado; ningún estado se comunica solo con color.
- **NFR-004** (Responsive): La búsqueda y la pantalla DEBEN funcionar desde 360 px de ancho; la ventana de resultados ocupa el ancho de la barra. **[S-08]**
- **NFR-005** (Idioma): Todos los textos, mensajes y botones se muestran en español.
- **NFR-006** (Coherencia): Los formatos y los mensajes de sesión DEBEN ser consistentes con los demás specs del Módulo 1.

---

## Key Entities *(include if feature involves data)*

- **Propietario**: Usuario con rol de Propietario, dueño de una o varias embarcaciones o con cuenta registrada aunque aún no tenga ninguna.
   - *Atributos que se consultan en este flujo*: Nombre completo, número de documento (obligatorio y único), correo electrónico y teléfono de contacto.
- **Administrador**: Usuario autenticado con permiso de supervisión global. Es el único actor de este spec.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 95% de las búsquedas válidas, la ventana de resultados aparece en menos de 1 segundo. **[S-20]**
- **SC-002**: El 100% de los propietarios registrados son encontrables por nombre o por cédula, sin importar si tienen embarcaciones o el estado de su cuenta.
- **SC-003**: El 100% de las coincidencias de igual nombre o igual documento se muestran como resultados separados, hasta el máximo de 5.
- **SC-004**: En el 100% de las búsquedas, la tabla de "Control General de Flota Fluvial" no se filtra ni se reordena.
- **SC-005**: El 100% de las pantallas de "Información del Propietario" carecen de botones o campos de edición y muestran únicamente "Volver".
- **SC-006**: En el 100% de los regresos con "Volver", la tabla se muestra en su estado por defecto, con la barra vacía y la ventana cerrada.
- **SC-007**: El 100% de los intentos de un usuario que no es administrador de ver información de un propietario terminan en la pantalla de error, sin que se muestre ningún dato.
- **SC-008**: El 100% de las veces que el servidor indica sesión no válida, el usuario es llevado al inicio de sesión, sin mostrar el error de red.

---

---

## Historial de cambios

| Fecha | Cambio |
|---|---|
| 11 de noviembre de 2026 | Se quitan el tipo de documento (`C.C.`, «Cédula de Ciudadanía»), la fecha de registro y el estado de cuenta (badge, mensajes y bloque «Estado de cuenta y restricciones»), porque `users` no los guarda. Decisión del usuario. Afecta flujo, historia 2 (escenarios de cuenta activa, por verificar e inactiva, y de formato), FR-018, FR-021, FR-022, entidades y SC. |
| 11 de noviembre de 2026 | «Volver» regresa a la tabla en su estado por defecto, sin filtros (FR-025, historia 3, SC), de acuerdo con «Consultar detalles de embarcación». |
| 11 de noviembre de 2026 | La búsqueda excluye a los administradores; espera 300 ms; el servidor exige entre 3 y 100 caracteres (FR-002, FR-005). |
| 11 de noviembre de 2026 | El propietario inexistente, un administrador o un identificador inválido se tratan igual (404, FR-023); un usuario sin rol de administrador ve la pantalla de acceso no autorizado (FR-031). |
| 11 de noviembre de 2026 | El aviso de sesión expirada y el destino de retorno quedan pendientes, comunes a todas las pantallas (FR-030). FR-032 (registro de consultas) sigue sin implementar. |
| 11 de noviembre de 2026 | El filtro por propietario de la tabla excluye filas y solo ofrece propietarios con embarcaciones (sección de relación con otros specs). |
