# Feature Specification: Editar Información de Embarcación

**Created**: 25 de octubre de 2026  
**Última revisión**: 9 de noviembre de 2026 (ver «Registro de cambios» al final del documento)

**Módulo**: Módulo 1 – Gestión de Flota (SeaShare)  
**Actor**: Propietario (usuario autenticado)

**Objetivo de negocio**: Permitir que el propietario modifique y mantenga actualizada la información operativa de sus embarcaciones (nombre, capacidad de pasajeros, tarifa base, puerto de atraque con su ubicación GPS, fotografía oficial y servicios adicionales), garantizando la integridad de los datos con validaciones estrictas en tiempo real en el cliente y revalidación obligatoria en el servidor antes de aplicar los cambios.

**Estados operativos y edición**:
- **Disponible**: Visible para alquiler. ¿Se puede editar? Sí.
- **Reservado**: Bloqueo temporal por proceso de pago. ¿Se puede editar? No.
- **En Navegación**: Contrato activo y embarcación fuera del puerto. ¿Se puede editar? No.
- **En Mantenimiento/Limpieza**: Inhabilitada por reparaciones o adecuación. ¿Se puede editar? Sí.

El botón **"Editar"** existe únicamente en la pantalla "Ver Detalles" de la vista del propietario. En la vista del administrador el botón no existe. Si la embarcación está en Reservado o En Navegación, el botón aparece deshabilitado (sin tooltip, con el icono de prohibido, como el resto de botones de «Ver Detalles»).

**Fuera de alcance de este spec** (se documentan en specs separados):
- Pantalla "Ver Detalles" (spec "Consultar detalles de embarcación"). Este spec inicia cuando el propietario presiona "Editar".
- Botones "Solicitar Revisión / Enviar a Mantenimiento" y "Eliminar Embarcación".
- Edición de "Matrícula Legal" y "Tipo de Embarcación": permanecen bloqueados y de solo lectura.
- Proceso de registro inicial de una embarcación.
- Elección del proveedor del mapa interactivo (se define en el documento PLAN).


## User Scenarios & Testing *(mandatory)*

### User Story 1 - Acceder a la edición según rol y estado (Priority: P1)

Como propietario, quiero que el botón "Editar" solo me permita editar cuando la embarcación está en un estado editable, para no modificar una embarcación reservada o en navegación.

**Why this priority**: Es la puerta de entrada y protege la integridad operativa de las embarcaciones registradas. Sin la verificación adecuada de estado y rol, se podrían alterar datos operativos de embarcaciones en tránsito o comprometidas comercialmente.

**Independent Test**: Abrir la vista "Ver Detalles" con embarcaciones en cada uno de los distintos estados (Disponible, Reservado, En Navegación, En Mantenimiento/Limpieza) y con cada rol (Propietario, Administrador), verificando el comportamiento del botón "Editar" y los accesos por URL directa.

**Acceptance Scenarios**:

1. **Scenario**: Editar habilitado
   - **Given** un propietario está en "Ver Detalles" de una embarcación en estado "Disponible" o "En Mantenimiento/Limpieza"
   - **When** observa el botón "Editar"
   - **Then** el botón está habilitado y al presionarlo se abre el formulario de edición con los datos precargados

2. **Scenario**: Editar deshabilitado
   - **Given** que la embarcación está en estado "Reservado" o "En Navegación"
   - **When** el propietario observa el botón "Editar" o pasa el cursor sobre él
   - **Then** el botón aparece deshabilitado, no muestra tooltip y, al pasar el mouse, aparece el icono de prohibido (círculo rojo con línea diagonal), igual que los demás botones deshabilitados de "Ver Detalles"; al presionarlo no ocurre nada

3. **Scenario**: Vista del administrador
   - **Given** un usuario autenticado con rol de Administrador en la pantalla "Ver Detalles"
   - **When** se muestra la pantalla
   - **Then** el botón "Editar" no existe en la interfaz

4. **Scenario**: URL directa con estado no editable
   - **Given** que se intenta acceder a la pantalla de edición mediante URL directa y la embarcación se encuentra en estado "Reservado" o "En Navegación"
   - **When** se carga la pantalla
   - **Then** el sistema muestra una pantalla de bloqueo con el mensaje "Esta embarcación no se puede editar en su estado actual." y el botón "Volver a Ver Detalles"

5. **Scenario**: URL directa sin permisos o de una embarcación inexistente
   - **Given** que accede por URL directa un usuario que no es el propietario dueño de la embarcación, o la embarcación no existe o fue eliminada
   - **When** se carga la pantalla
   - **Then** el sistema muestra una pantalla de bloqueo con el mensaje "Esta embarcación no existe o no tienes permiso para editarla." y el botón "Volver a Ver Detalles" (el servidor responde igual en ambos casos para no revelar si la embarcación existe)
   - **And** si quien accede tiene otro rol (por ejemplo, Administrador), el sistema muestra la pantalla de acceso no autorizado del sistema, sin cargar ningún dato de la embarcación

6. **Scenario**: Carga inicial del formulario
   - **Given** que se abre el formulario de edición
   - **When** el sistema consulta los datos de la embarcación
   - **Then** muestra un estado de carga (skeleton o spinner); si la carga falla, muestra la pantalla de error "No se pudo cargar la información de la embarcación" con los botones "Reintentar" y "Volver a Ver Detalles", sin desplegar el formulario vacío

7. **Scenario**: Precarga de información
   - **Given** que los datos de la embarcación cargaron correctamente
   - **When** se despliega el formulario
   - **Then** todos los campos aparecen precargados con la información actual, "Matrícula Legal" y "Tipo de Embarcación" están bloqueados con un icono de candado, y el botón "Guardar cambios" se encuentra deshabilitado por defecto

---

### User Story 2 - Editar información general con validación en tiempo real (Priority: P1)

Como propietario, quiero modificar el nombre, la capacidad y la tarifa base con validaciones inmediatas y autoselección de texto, para corregir datos con rapidez sin errores de formato.

**Why this priority**: Es el núcleo de la edición de datos primarios. Asegura que cualquier modificación cumpla con todas las reglas de formato e integridad antes de permitir el envío al servidor.

**Independent Test**: Modificar individualmente los campos de nombre, capacidad y tarifa base con valores válidos e inválidos, verificando la aparición/desaparición de mensajes de error debajo del campo afectado y el estado del botón "Guardar cambios".

**Acceptance Scenarios**:

1. **Scenario**: Autoselección de texto al enfocar
   - **Given** el formulario precargado de edición
   - **When** el propietario hace clic, toca o llega con la tecla Tab a un campo editable
   - **Then** todo el texto del campo se selecciona automáticamente; un segundo clic ubica el cursor en la posición deseada

2. **Scenario**: Nombre de embarcación válido
   - **Given** que el propietario edita el nombre de la embarcación ingresando un texto permitido (3 a 50 caracteres, al menos una letra)
   - **When** el campo pierde el foco (on blur)
   - **Then** se aplica trim a los espacios extremos, se colapsan los espacios internos múltiples, no se muestra ningún error y, si existen cambios respecto al valor inicial y no hay otros errores, el botón "Guardar cambios" se habilita

3. **Scenario**: Nombre de embarcación inválido
   - **Given** un nombre vacío, compuesto solo por números (por ejemplo "123" o "12-3") o que incumple la sintaxis o longitud
   - **When** el usuario escribe o pega texto en el campo
   - **Then** el sistema muestra inmediatamente debajo del campo el mensaje de error correspondiente según la cascada de prioridad y el botón "Guardar cambios" permanece deshabilitado

4. **Scenario**: Validación en tiempo real de la capacidad
   - **Given** el campo "Capacidad máxima (pasajeros)"
   - **When** el usuario escribe o pega letras, decimales, signos, el valor 0, un número mayor al límite del tipo de embarcación o de 100 o más
   - **Then** se muestra debajo del campo el mensaje de error que corresponde a la regla de mayor prioridad; un valor con ceros a la izquierda como "007" se acepta y procesa como 7

5. **Scenario**: Validación en tiempo real de la tarifa base
   - **Given** el campo "Tarifa base (COP)"
   - **When** el usuario ingresa o pega letras, puntos, comas, signos, el valor 0 o un número superior a $9.999.999.999
   - **Then** se muestra debajo del campo el mensaje de error correspondiente según la cascada de prioridad

6. **Scenario**: Reversión de cambios
   - **Given** que el propietario modificó un campo en el formulario
   - **When** restaura el valor original precargado
   - **Then** el sistema detecta que no hay modificaciones reales y el botón "Guardar cambios" vuelve a deshabilitarse

---

### User Story 3 - Modificar ubicación, puerto, fotografía y servicios (Priority: P2)

Como propietario, quiero actualizar el puerto de atraque en el mapa, su nombre, la fotografía oficial y los servicios adicionales, para que los datos logísticos de mi embarcación sean precisos.

**Why this priority**: Asegura la precisión geográfica, visual y de equipamiento disponible para el alquiler de la embarcación.

**Independent Test**: Mover el marcador en el mapa interactivo, editar el nombre del puerto, reemplazar/quitar la fotografía oficial y marcar/desmarcar casillas de servicios adicionales, validando que los datos se actualicen y se reflejen correctamente.

**Acceptance Scenarios**:

1. **Scenario**: Mapa precargado
   - **Given** que se abre el formulario de edición
   - **When** se muestra el selector en mapa
   - **Then** aparece centrado en el punto guardado actualmente, con el marcador colocado en esa posición exacta (zoom 15) y los campos "Latitud" y "Longitud" muestran las coordenadas correspondientes

2. **Scenario**: Mover el marcador en el mapa
   - **Given** el mapa cargado correctamente
   - **When** el propietario hace clic o arrastra el marcador
   - **Then** "Latitud" y "Longitud" se actualizan de inmediato con signo y hasta 4 decimales sin ceros de relleno; el cambio se cuenta como modificación del formulario solo si los valores redondeados difieren de los precargados

3. **Scenario**: Longitud fuera de rango
   - **Given** que el selector de mapa entrega un valor de longitud fuera de rango (por ejemplo 190)
   - **When** se completa el campo
   - **Then** el sistema normaliza automáticamente el valor al rango válido (en el ejemplo, a -170)

4. **Scenario**: Fallo de carga del mapa
   - **Given** que el componente de mapa interactivo no logra cargar
   - **When** se muestra su contenedor
   - **Then** aparece el mensaje "No se pudo cargar el mapa interactivo en este momento." con un botón interno "Reintentar", permitiendo que el resto del formulario siga completamente operativo

5. **Scenario**: Nombre del puerto de atraque
   - **Given** el campo "Puerto"
   - **When** el propietario lo edita manualmente
   - **Then** se aplican las mismas reglas de validación que para el nombre de la embarcación (mostrando los mensajes propios del puerto); mover el marcador en el mapa no altera el nombre escrito en el campo "Puerto"

6. **Scenario**: Carga de fotografía oficial válida
   - **Given** que el propietario presiona "Cambiar fotografía" y selecciona un archivo PNG, JPG o JPEG real de hasta 10 MB
   - **When** el archivo es aceptado
   - **Then** la vista previa reemplaza la imagen anterior y muestra una "X" en su esquina superior derecha

7. **Scenario**: Fotografía oficial inválida
   - **Given** un archivo con formato no permitido, animado, corrupto, de 0 bytes o mayor a 10 MB
   - **When** se intenta adjuntar
   - **Then** el sistema no acepta el archivo, muestra el mensaje de error debajo del área de carga y conserva la imagen que estaba precargada o seleccionada previamente

8. **Scenario**: Quitar la fotografía oficial
   - **Given** que el propietario hace clic en la "X" de la vista previa (sea la imagen guardada o una recién seleccionada)
   - **When** la imagen se elimina del campo
   - **Then** el campo queda vacío, se despliega de inmediato el mensaje "Este campo es obligatorio" y el botón "Guardar cambios" se deshabilita

9. **Scenario**: Selección de servicios
   - **Given** la sección de servicios en el formulario
   - **When** el propietario observa los campos
   - **Then** los "Servicios base obligatorios" (Capitán y Combustible) aparecen fijos y bloqueados, mientras que los "Servicios adicionales" se pueden marcar y desmarcar libremente mediante casillas de verificación

10. **Scenario**: Servicios adicionales precargados
   - **Given** que los datos de la embarcación cargaron correctamente
   - **When** se muestra la sección de servicios
   - **Then** las casillas de servicios adicionales aparecen marcadas según lo guardado; los servicios llegan en la misma consulta que el resto de los datos, por lo que si esa consulta falla se muestra la pantalla de error de carga (FR-004) y no una sección de servicios aparte

---

### User Story 4 - Guardar, descartar y manejar errores (Priority: P3)

Como propietario, quiero confirmaciones al guardar o descartar, protección contra doble envío y mensajes claros ante errores, para no perder datos ni generar peticiones duplicadas.

**Why this priority**: Otorga robustez, resiliencia y estabilidad técnica al flujo de edición ante problemas de red o interacciones concurrentes.

**Independent Test**: Ejecutar solicitudes de guardado con distintas respuestas del backend (éxito, error técnico, timeout, cambio de estado, embarcación inexistente), cancelar/descartar la edición con datos modificados y probar la edición desde dos pestañas a la vez.

**Acceptance Scenarios**:

1. **Scenario**: Guardar y prevención de doble envío
   - **Given** el formulario con al menos una modificación válida y sin errores
   - **When** el propietario presiona "Guardar cambios"
   - **Then** el botón se deshabilita de inmediato mostrando un icono de carga (spinner), la tecla Enter deja de emitir envíos y el sistema procesa una única Petición

2. **Scenario**: Guardado exitoso
   - **Given** que el servidor procesa y guarda la actualización correctamente
   - **When** responde la Petición
   - **Then** se muestra el modal "¡Cambios guardados!" con el texto "La información de tu embarcación ha sido actualizada exitosamente en el sistema." y el botón "Aceptar", que redirige a "Ver Detalles" reflejando los datos actualizados

3. **Scenario**: Descarte con cambios sin guardar
   - **Given** que existen modificaciones en el formulario
   - **When** el propietario presiona "Descartar", "Volver", intenta cambiar de ruta o cerrar la pestaña
   - **Then** el sistema interrumpe la navegación y muestra el modal "¿Descartar cambios?" con el texto "Tienes modificaciones sin guardar que se perderán. ¿Estás seguro de que deseas volver sin guardar?" y los botones "Seguir editando" (cierra el modal y mantiene el formulario intacto) y "Sí, descartar" (regresa a "Ver Detalles" sin guardar)

4. **Scenario**: Descarte sin modificaciones
   - **Given** que no existe ninguna modificación en los campos respecto a los valores precargados
   - **When** el propietario sale del formulario
   - **Then** el sistema navega directamente a "Ver Detalles" sin mostrar modales de advertencia

5. **Scenario**: Error técnico o timeout al guardar
   - **Given** que ocurre un fallo de red, el servidor responde con error técnico o la petición supera los 15 segundos de espera
   - **When** se presiona "Guardar cambios"
   - **Then** la solicitud se cancela y se muestra el modal "No se pudo guardar los cambios" con el texto "Ocurrió un problema técnico al guardar la información de la embarcación. Verifique su conexión a internet e inténtelo nuevamente." y los botones "Cerrar" y "Reintentar"; todos los datos digitados en el formulario permanecen intactos

6. **Scenario**: Cambio de estado de la embarcación durante la edición
   - **Given** que la embarcación cambió a estado "Reservado" o "En Navegación" en otra sesión mientras el propietario editaba
   - **When** presiona "Guardar cambios"
   - **Then** el sistema detiene el guardado y muestra el modal "No se pudieron guardar los cambios" con el texto "La embarcación pasó a estado [Estado] mientras editabas. No es posible guardar los cambios." y el botón "Recargar página"

7. **Scenario**: Edición desde dos sesiones (sin control de versión)
   - **Given** que el mismo propietario tiene abierto el formulario de edición de una embarcación en dos pestañas o dispositivos y guarda en ambos
   - **When** presiona "Guardar cambios" en la segunda sesión
   - **Then** el sistema guarda sin avisar de conflicto y prevalece el último guardado; no existe control de versión, porque solo el propietario titular puede editar su embarcación

8. **Scenario**: Embarcación eliminada durante la edición
   - **Given** que la embarcación fue eliminada en el sistema mientras el formulario estaba abierto
   - **When** se presiona "Guardar cambios"
   - **Then** el sistema muestra el modal "Esta embarcación ya no existe." con el botón "Aceptar", que redirige a la lista "Mis Embarcaciones Registradas"

9. **Scenario**: Pérdida de permisos al guardar
   - **Given** que el usuario perdió el rol de Propietario durante la edición (respuesta 403 del servidor)
   - **When** presiona "Guardar cambios"
   - **Then** muestra el modal "No tienes permiso para editar esta embarcación." con el botón "Volver a Ver Detalles"

10. **Scenario**: Expiración de sesión al guardar
   - **Given** que la sesión del usuario expiró mientras editaba
   - **When** presiona "Guardar cambios"
   - **Then** el sistema redirige a la pantalla de inicio de sesión y los datos no guardados se pierden

11. **Scenario**: Orden de verificaciones en el servidor
   - **Given** un intento de guardado enviado al servidor
   - **When** se procesa la solicitud
   - **Then** el servidor evalúa en este orden estricto y se detiene en el primer fallo: (1) sesión activa, (2) permisos y existencia, (3) estado operativo, (4) validación estricta de campos y archivos, (5) persistencia de datos

12. **Scenario**: Errores de validación devueltos por el servidor
   - **Given** que el servidor rechaza la solicitud por fallos de validación en campos específicos (HTTP 400)
   - **When** responde la API
   - **Then** cada error se despliega debajo de su campo correspondiente, el botón "Guardar cambios" se rehabilita y el foco se traslada al primer campo con error

---

### Edge Cases

- **¿Qué pasa si el propietario escribe espacios al inicio o al final, o múltiples espacios dentro del nombre?** Se normalizan (trim y colapso de espacios internos) únicamente al perder el foco (on blur) o al guardar. Tabulaciones y espacios no estándar pegados se convierten a un espacio normal.
- **¿Qué pasa si el nombre de la embarcación coincide con el de otra?** Se permite; el nombre no requiere ser único en la flota.
- **¿Qué pasa si el nombre incluye números, numerales romanos, ñ, ü o tildes?** Son válidos siempre que contengan al menos una letra. Valores como "123", "12-3" o "---" son inválidos, cada uno mostrando el mensaje de su prioridad.
- **¿Qué pasa si se ingresan entradas mixtas en campos numéricos (ej. 12a, 1.5, -5, 1 2)?** Se mantienen escritas en el campo y muestran el mensaje de mayor prioridad según la cascada correspondiente.
- **¿Qué pasa si se escriben ceros a la izquierda (ej. 007)?** Se aceptan y procesan como el número entero 7; "000" se trata como 0.
- **¿Qué pasa si se ingresa una capacidad de 100 o más?** Muestra el mensaje de límite según el tipo de embarcación.
- **¿Qué pasa si las coordenadas del mapa entregan más de 4 decimales?** Se redondean automáticamente a 4 decimales. Si el marcador se mueve una distancia mínima que no altera los valores redondeados, no se contabiliza como modificación.
- **¿Qué pasa si el mapa entrega una longitud fuera de rango (ej. 190)?** Se normaliza a un valor dentro del rango válido (ej. -170). Cualquier punto en el mapa es aceptado.
- **¿Qué pasa si se adjunta un archivo de más de 10 MB que además no es una imagen válida?** Prioriza mostrar "Formato de imagen inválido".
- **¿Qué pasa si se cancela el selector de archivos del sistema?** Se conserva la imagen que estaba cargada previamente.
- **¿Qué pasa si se quita la fotografía con la "X"?** El mensaje "Este campo es obligatorio" se despliega de inmediato y el botón "Guardar cambios" se deshabilita.
- **¿Qué pasa si el usuario presiona Enter repetidamente o hace doble clic en "Guardar cambios"?** El botón se deshabilita tras el primer clic y se emite únicamente una petición al servidor.
- **¿Qué pasa si el propietario abre la edición en dos pestañas y guarda en ambas?** Se guardan ambos cambios en orden de llegada; prevalece el último. No hay aviso de conflicto.
- **¿Qué pasa si se cambia solo la latitud o la longitud y se conserva el nombre del puerto?** Los puertos son compartidos por nombre: se actualizan las coordenadas de ese puerto y las demás embarcaciones que lo usan pasan a mostrar la nueva ubicación. Si el nombre del puerto es nuevo, se crea otro puerto y el anterior no se modifica. El nombre se compara sin distinguir mayúsculas.
- **¿Qué pasa si el propietario no cambia la fotografía?** La solicitud no la incluye y el servidor conserva la imagen guardada.
- **¿Qué pasa si la sesión expira con el formulario abierto?** Al intentar guardar se redirige a inicio de sesión y se pierden los datos no guardados.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Acceso y estructura

- **FR-001**: El botón "Editar" DEBE existir exclusivamente en la vista "Ver Detalles" del propietario; en la vista del administrador NO DEBE existir.
- **FR-002**: El botón "Editar" DEBE estar habilitado solo si el estado operativo es Disponible o En Mantenimiento/Limpieza; en Reservado o En Navegación DEBE mostrarse deshabilitado, sin tooltip y con el icono de prohibido al pasar el mouse (FR-024 de "Consultar detalles").
- **FR-003**: Ante accesos por URL directa, el sistema DEBE mostrar la pantalla de bloqueo "Esta embarcación no se puede editar en su estado actual." si el estado no es editable, o "Esta embarcación no existe o no tienes permiso para editarla." si la embarcación no existe o no pertenece al usuario; ambas con el botón "Volver a Ver Detalles". Un usuario con otro rol DEBE ver la pantalla de acceso no autorizado del sistema.
- **FR-004**: Mientras se consultan los datos de la embarcación, el sistema DEBE mostrar un estado de carga. Si la carga falla, DEBE mostrar "No se pudo cargar la información de la embarcación" con los botones "Reintentar" y "Volver a Ver Detalles".
- **FR-005**: El sistema DEBE precargar la información actual de la embarcación en el formulario.
- **FR-006**: "Matrícula Legal" y "Tipo de Embarcación" DEBEN permanecer bloqueados, de solo lectura y con un icono de candado.
- **FR-007**: Al recibir el foco por clic, toque o Tab en un campo editable precargado, el sistema DEBE seleccionar automáticamente todo el texto existente.

#### Validaciones del formulario

- **FR-008**: "Nombre de la embarcación" DEBE ser obligatorio, de 3 a 50 caracteres, contener al menos una letra y solo incluir caracteres permitidos (letras, dígitos, espacios, tildes, guion medio), aplicando la cascada de mensajes definida.
- **FR-009**: El sistema DEBE aplicar trim, colapso de espacios internos múltiples y conversión de espacios no estándar a espacio simple en el nombre de la embarcación y del puerto, al perder el foco (on blur) y al guardar.
- **FR-010**: "Puerto" DEBE ser de ingreso manual, cumplir las reglas de sintaxis de nombres y desplegar sus mensajes específicos.
- **FR-011**: "Capacidad máxima (pasajeros)" DEBE ser obligatoria, un entero mayor que 0 de máximo 2 dígitos (tras remover ceros a la izquierda) y no superar el límite del tipo (Lancha 10, Yate 20, Catamarán 30, Velero 12), aplicando la cascada de mensajes definida.
- **FR-012**: "Tarifa base (COP)" DEBE ser obligatoria, un entero mayor que 0 de hasta 10 dígitos (máximo $9.999.999.999, el mismo tope del registro), aplicando la cascada de mensajes definida. NINGÚN texto en la pantalla DEBE mencionar periodicidad por hora ni por día.
- **FR-013**: Los valores pegados desde el portapapeles DEBEN validarse inmediatamente con las mismas reglas que los tecleados.
- **FR-014**: Cada mensaje de validación DEBE mostrarse debajo del campo afectado, uno solo por campo, en menos de 1 segundo.

#### Ubicación, fotografía y servicios

- **FR-015**: "Latitud" y "Longitud" DEBEN ser de solo lectura, mostrar las coordenadas actuales al abrir y actualizarse inmediatamente al mover el marcador en el mapa, en formato WGS 84 (EPSG:4326), con signo negativo para Sur y Oeste, entre -90 y 90 (latitud) y entre -180 y 180 (longitud), con hasta 4 decimales sin ceros de relleno.
- **FR-016**: El mapa interactivo DEBE abrir centrado en las coordenadas guardadas con el marcador en esa posición (zoom 15) y aceptar cualquier punto seleccionado.
- **FR-017**: El sistema DEBE normalizar automáticamente longitudes fuera de rango (ej. 190 a -170) y, ante una selección inválida, DEBE mostrar "Selección de ubicación inválida. Por favor, marca un punto válido en el mapa.".
- **FR-018**: Si el mapa falla al cargar, el sistema DEBE mostrar "No se pudo cargar el mapa interactivo en este momento." con un botón interno "Reintentar", sin bloquear el resto del formulario.
- **FR-019**: "Fotografía oficial" DEBE ser obligatoria y aceptar solo archivos PNG, JPG o JPEG reales de hasta 10 MB, validados por su firma real de archivo; DEBE rechazar archivos renombrados, corruptos, de 0 bytes o animados con "Formato de imagen inválido" o "El tamaño de imagen se excede", mostrando la leyenda "PNG, JPG hasta 10 MB (Recomendado 400 × 400 px)". Las dimensiones recomendadas NO DEBEN bloquear la carga.
- **FR-020**: Un archivo rechazado NO DEBE reemplazar la imagen existente. Cancelar el selector NO DEBE cambiar la imagen. La "X" DEBE dejar el campo vacío y mostrar de inmediato "Este campo es obligatorio".
- **FR-021**: Los servicios base (Capitán y Combustible) DEBEN permanecer fijos y bloqueados; los servicios adicionales DEBEN poder marcarse y desmarcarse libremente.
- **FR-022**: *(Eliminado el 9 de noviembre de 2026.)* Los servicios llegan en la misma consulta de la embarcación, por lo que no existe un fallo de carga aislado; la consulta completa se cubre con FR-004. El servidor igualmente conserva los servicios guardados si la solicitud no incluye ninguno.

#### Guardado, modales y errores

- **FR-023**: El botón "Guardar cambios" DEBE permanecer deshabilitado por defecto y habilitarse solo cuando exista al menos una modificación respecto a los valores precargados Y todos los campos sean válidos. Si se revierten todos los cambios, DEBE deshabilitarse nuevamente.
- **FR-024**: Al hacer clic en "Guardar cambios", el botón DEBE deshabilitarse inmediatamente con un icono de carga (spinner), la tecla Enter NO DEBE reenviar el formulario y DEBE emitirse únicamente una petición al servidor.
- **FR-025**: Antes de persistir, el servidor DEBE verificar en este orden estricto y detenerse en la primera falla: (1) sesión activa, (2) permisos y existencia, (3) estado operativo, (4) validación de campos y archivos, (5) guardado. Una embarcación ajena, eliminada, inexistente o en borrador responde igual (404).
- **FR-026**: *(Eliminado el 9 de noviembre de 2026.)* No hay control de versión. El cambio de estado se verifica en el momento de guardar, de forma atómica con el guardado (FR-027).
- **FR-027**: Si el estado cambió a Reservado o En Navegación, el sistema NO DEBE guardar y DEBE mostrar el modal "No se pudieron guardar los cambios" con "La embarcación pasó a estado [Estado] mientras editabas. No es posible guardar los cambios." y el botón "Recargar página".
- **FR-028**: *(Eliminado el 9 de noviembre de 2026.)* Sin control de versión: ante dos guardados del mismo propietario prevalece el último (ver historia 4, escenario 7).
- **FR-029**: Ante embarcación inexistente DEBE mostrarse el modal "Esta embarcación ya no existe." con "Aceptar" (lleva a la lista); ante pérdida del rol de Propietario (403), "No tienes permiso para editar esta embarcación." con "Volver a Ver Detalles"; ante sesión expirada, DEBE redirigirse al inicio de sesión perdiendo los datos no guardados.
- **FR-030**: Si la petición falla por red, error de servidor o supera 15 segundos de espera, el sistema DEBE cancelarla y mostrar el modal "No se pudo guardar los cambios" con los botones "Cerrar" y "Reintentar", conservando los datos del formulario. Un fallo en la subida de imagen DEBE revertir toda la operación atómica y mostrar este mismo modal.
- **FR-031**: Los errores de validación del servidor devueltos por campo (400) DEBEN desplegarse debajo de su campo correspondiente, rehabilitar el botón y llevar el foco al primer campo con error.
- **FR-032**: Tras un guardado exitoso, el sistema DEBE mostrar el modal "¡Cambios guardados!" con "La información de tu embarcación ha sido actualizada exitosamente en el sistema." y el botón "Aceptar", que redirige a "Ver Detalles" con la información actualizada.
- **FR-033**: Si el usuario intenta salir existiendo cambios sin guardar (Descartar, Volver, cambiar de ruta o cerrar la ventana), el sistema DEBE mostrar el modal "¿Descartar cambios?" con "Tienes modificaciones sin guardar que se perderán. ¿Estás seguro de que deseas volver sin guardar?" y los botones "Seguir editando" y "Sí, descartar". Sin modificaciones, DEBE navegar directamente.
- **FR-034**: Ningún modal del flujo DEBE llevar botón "X" ni cerrarse con la tecla Esc o clic fuera; únicamente con sus botones explícitos.

---

### Requisitos No Funcionales

- **NFR-001** (Regla de Edición): El servidor DEBE permitir la edición de una embarcación únicamente al usuario con rol de Propietario que sea titular de la misma, rechazando cualquier intento de edición desde el rol de Administrador u otros roles.
- **NFR-002** (Integridad): El guardado es atómico (datos e imagen juntos) y no genera registros ni cargas duplicadas ante reintentos o clics repetidos.
- **NFR-003** (Rendimiento): Los mensajes de validación aparecen en menos de 1 segundo; la petición de guardado se cancela a los 15 segundos si no responde.
- **NFR-004** (Accesibilidad): Etiquetas asociadas a cada campo, mensajes de error vinculados con `aria-describedby` y anunciados con `aria-live`, navegación completa por teclado, contraste mínimo AA y errores no indicados solo por color.
- **NFR-005** (Responsive): Usable desde 360 px de ancho, con botones de al menos 44 px de área táctil y teclado numérico en campos numéricos en dispositivos móviles.
- **NFR-006** (Idioma): Todos los textos, mensajes de error y modales se muestran en español.

---

## Key Entities *(include if feature involves data)*

- **Embarcación**: Vehículo fluvial registrado por un propietario.
   - *Atributos editables en este flujo*: Nombre de la embarcación (3–50 caracteres), Capacidad máxima de pasajeros (entero 1 a límite del tipo), Tarifa base (entero en COP, 1 a 9.999.999.999), Puerto de atraque (nombre, latitud y longitud con signo y hasta 4 decimales), Fotografía oficial (imagen PNG/JPG real hasta 10 MB) y Servicios adicionales (subconjunto opcional de 5 opciones).
   - *Atributos inmutables en este flujo*: Matrícula legal (única en el sistema), Tipo de embarcación (Lancha, Yate, Catamarán, Velero), Estado operativo (Disponible, Reservado, En Navegación, En Mantenimiento/Limpieza) y Servicios base obligatorios (Capitán, Combustible).
- **Puerto**: Registro compartido por nombre (único sin distinguir mayúsculas) con su latitud y longitud. Editar la ubicación de un puerto existente la cambia para todas las embarcaciones que lo usan.
- **Propietario**: Usuario autenticado dueño de la embarcación. Atributos: ID de usuario, rol, documento de identidad, nombre.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las validaciones de campo muestran su mensaje de error debajo del campo afectado en menos de 1 segundo desde la interacción.
- **SC-002**: En una prueba automatizada de 20 clics rápidos sobre "Guardar cambios", el servidor recibe y procesa exactamente 1 petición de guardado.
- **SC-003**: El 100% de los datos ingresados en el formulario se conservan intactos ante un error técnico, de red o de servidor al guardar.
- **SC-004**: El 100% de los intentos de guardar con la embarcación en estado Reservado o En Navegación (por cambio de estado concurrente) son bloqueados sin modificar ningún dato.
- **SC-005**: Toda regla de validación del cliente tiene su equivalente probado y validado en el servidor.
- **SC-006**: Un propietario puede modificar y guardar su embarcación en menos de 2 minutos desde que ingresa a la pantalla de edición.
- **SC-007**: El formulario es operable completamente mediante teclado y supera auditorías de accesibilidad sin errores críticos.

---

## Registro de cambios

**9 de noviembre de 2026** (revisión hecha al implementar la funcionalidad, con autorización del usuario para ajustar este documento):

| # | Cambio | Motivo |
|---|---|---|
| 1 | Se elimina el tooltip del botón "Editar" deshabilitado (historia 1, escenario 2; FR-002; edge cases) | Contradecía el FR-024 de "Consultar detalles" (ningún botón lleva tooltip; el deshabilitado muestra el icono de prohibido). Se aplica la misma regla que «Eliminar» |
| 2 | Tope de la tarifa pasa de $99.999.999 a $9.999.999.999 (historia 2 escenario 5, FR-012, entidades) | Igualar el tope de «Registrar embarcación» y de la base de datos (`DECIMAL(12,2)`); con dos topes distintos una embarcación registrada podría quedar imposible de editar |
| 3 | URL directa sin permisos (historia 1 escenario 5, FR-003): el mensaje es "Esta embarcación no existe o no tienes permiso para editarla."; otro rol ve la pantalla de acceso no autorizado | El servidor responde 404 igual para embarcación ajena, eliminada o inexistente (no revelar que existe), por lo que el cliente no puede distinguirlos. El mensaje "No tienes permiso…" queda para el 403 por rol |
| 4 | Se eliminan el control de versión y el modal de conflicto (historia 4 escenario 7 reescrito; FR-025 reordenado; FR-026 y FR-028 eliminados; atributo "Versión" fuera de las entidades) | Decisión del usuario: solo el propietario titular edita; se acepta que prevalezca el último guardado. Evita una columna nueva en el modelo físico |
| 5 | Se elimina el fallo aislado de carga de servicios (historia 3 escenario 10; FR-022; edge case) | Los servicios vienen en la misma consulta que los demás datos de la embarcación; no existe una carga aparte que pueda fallar sola |
| 6 | La "X" de la fotografía también aparece sobre la imagen ya guardada (historia 3 escenario 8) | Sin esto, el escenario de quitar la foto no se podía ejecutar al abrir la pantalla |
| 7 | Errores de validación del servidor: solo HTTP 400 (no 422) | El backend responde 400 para todo error de validación |
| 8 | Se documentan las reglas del puerto compartido y de la foto opcional en la solicitud (edge cases, entidad Puerto) | Aclaran el comportamiento acordado el 9 de noviembre de 2026 |
| 9 | Escenario 9 de la historia 4: la pérdida de permisos se entiende como pérdida del rol (403) | Una embarcación que dejó de ser del usuario llega como 404 («ya no existe») |
