# Feature Specification: Registrar Embarcación

**Created**: 16 de octubre de 2026

**Última actualización**: 4 de noviembre de 2026. Cambios: tope técnico de la tarifa base (FR-048) y reutilización de puertos existentes (FR-045), con sus escenarios, casos límite y entidades.

**Módulo**: Módulo 1 – Gestión de Flota (SeaShare)
**Actor**: Propietario (usuario autenticado)

**Objetivo de negocio**: Permitir que los propietarios incorporen sus embarcaciones a la flota de la plataforma con información completa y confiable (identificación legal única, tipo y capacidad, puerto de atraque, tarifa base, fotografía oficial y servicios incluidos), de modo que queden disponibles para ser ofrecidas en alquiler.

**Definición de "Borrador"**: Borrador es un estado en el que queda una embarcación cuando el propietario completó el paso 1 (Datos Básicos), hizo clic en "Siguiente", pasó al paso 2 (Datos Complementarios) y no terminó el registro (por ejemplo, porque canceló). Una embarcación en estado Borrador:
- conserva únicamente los datos del paso 1;
- no aparece en la lista de embarcaciones;
- no vence con el tiempo y se conserva aunque la sesión del propietario expire;
- mantiene reservada su matrícula legal: mientras el borrador exista, ningún otro propietario puede usar esa matrícula, aunque el borrador haya quedado abandonado (comportamiento intencional);
- no permite editar los datos del paso 1: si el propietario se equivocó en alguno (por ejemplo, una letra de la matrícula), la única manera de corregirlo es eliminar el borrador y empezar un registro nuevo;
- pasa a estado "Disponible" cuando el propietario completa el paso 2 y guarda el registro.

Cada propietario tiene como máximo una embarcación en estado Borrador. Mientras la tenga, el sistema le exige continuarla o eliminarla antes de iniciar un registro nuevo.

**Fuera de alcance de este spec** (se documentan en specs separados):
- Comportamiento de los botones **"Cancelar"** de los formularios (spec "Cancelar registro"). Este spec solo describe el estado en que queda el registro después de cancelar, porque condiciona el modal "Borrador detectado".
- Botón **"Ver Detalles"** de la lista (spec "Consultar información de embarcación").
- Inicio de sesión del propietario. Se asume que las cuentas de propietario ya existen en el sistema y que el propietario solo inicia sesión (con su documento de identidad y contraseña); no crea cuentas. Este spec comienza cuando el propietario ya inició sesión.
- Editar, enviar a mantenimiento o eliminar embarcaciones ya registradas. Como regla de negocio relacionada, cuando una embarcación se elimina, su matrícula legal queda libre para usarse en otra embarcación.
- Paginación de la lista de embarcaciones.

**Pantallas de referencia (Figma)**: (1) Lista "Mis Embarcaciones Registradas", (2) Paso 1 de 2 "Datos Básicos", (3) Paso 2 de 2 "Datos Complementarios", (4) Modal "¡Embarcación registrada!", (5) Modal "Borrador detectado", (6) Modal "¿Iniciar nuevo registro?". Los siguientes elementos no están en el Figma actual y se definen en este spec: lista vacía, campos "Puerto", "Latitud" y "Longitud", etiqueta "Tarifa base (COP)", modal de error técnico "No se pudo guardar el registro", modal "El borrador ya no existe", modales de error "No se pudo eliminar el borrador" y "No se pudo cargar el registro pendiente", y los mensajes de error de carga de los tipos de embarcación y del mapa.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar los datos básicos de la embarcación, paso 1 de 2 (Priority: P1)

Como propietario, quiero iniciar el registro de una nueva embarcación desde mi lista de embarcaciones e ingresar su nombre, matrícula legal, tipo y capacidad máxima de pasajeros, para identificar y clasificar correctamente mi embarcación antes de completar el resto de su información.

**Why this priority**: Es la puerta de entrada al registro y la base del resto del flujo. Sin datos básicos válidos y únicos (especialmente la matrícula legal) no es posible identificar ni clasificar una embarcación. Además, al avanzar al paso 2 la embarcación queda en estado Borrador, lo que evita perder el trabajo ya realizado.

**Independent Test**: Se prueba ingresando a la lista, haciendo clic en "Registrar Nueva Embarcación", completando el paso 1 y haciendo clic en "Siguiente": el sistema guarda los datos, deja la embarcación en estado Borrador y muestra el paso 2. Entrega valor porque la identificación de la embarcación ya queda registrada y el propietario puede retomarla después.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de la lista de embarcaciones
    - **Given** el propietario inició sesión y tiene embarcaciones registradas
    - **When** ingresa al sistema
    - **Then** el sistema muestra la lista "Mis Embarcaciones Registradas" con el subtítulo "Gestiona el estado operativo y consulta los detalles de tus vehículos fluviales", el botón "Registrar Nueva Embarcación" y una tabla con las columnas "Nombre de la embarcación", "Matrícula", "Tipo", "Estado operativo" y "Acciones" (con el botón "Ver Detalles" en cada fila), ordenada de la embarcación más reciente a la más antigua según la fecha en que se completó su registro y sin incluir embarcaciones en estado Borrador

2. **Scenario**: Lista sin embarcaciones
    - **Given** el propietario inició sesión y no tiene ninguna embarcación registrada
    - **When** ingresa al sistema
    - **Then** el sistema muestra la lista "Mis Embarcaciones Registradas" con el mensaje "Aún no tienes embarcaciones registradas" en lugar de las filas, y el botón "Registrar Nueva Embarcación" sigue visible

3. **Scenario**: Acceso al paso 1 cuando el propietario no tiene un borrador
    - **Given** el propietario está en la lista y no tiene ninguna embarcación en estado Borrador
    - **When** hace clic en el botón "Registrar Nueva Embarcación"
    - **Then** el sistema muestra la pantalla con el título "Registrar Nueva Embarcación", el subtítulo "Paso 1 de 2: Información general y datos legales del vehículo fluvial." y el indicador de progreso con el paso 1 "Datos Básicos" activo y el paso 2 "Datos Complementarios" inactivo

4. **Scenario**: Campos y placeholders del paso 1
    - **Given** el propietario acaba de abrir el paso 1
    - **When** observa el formulario
    - **Then** el sistema muestra cuatro campos vacíos con las etiquetas y placeholders siguientes:
        - "Nombre de la embarcación": placeholder "Ej. Río Magdalena II"
        - "Matrícula legal": placeholder "Ej. CP-05-XXXX-A"
        - "Tipo de embarcación" (selector): placeholder "Seleccionar tipo (Lancha, Yate, etc.)"
        - "Capacidad máxima (pasajeros)": placeholder "Ej. 10"
    - y los botones "Cancelar" y "Siguiente"

5. **Scenario**: Avance al paso 2 con datos válidos
    - **Given** el propietario completó los cuatro campos del paso 1 con valores válidos (nombre válido, matrícula con formato correcto y no existente, tipo seleccionado, capacidad entera mayor que 0 que no supera el límite del tipo)
    - **When** hace clic en "Siguiente"
    - **Then** el sistema guarda los datos del paso 1, deja la embarcación en estado Borrador y muestra el paso 2 de 2 con el paso 1 marcado como completado en el indicador de progreso

6. **Scenario**: Campo obligatorio dejado vacío
    - **Given** el propietario está en el paso 1
    - **When** pasa a otro campo sin haber escrito nada en un campo obligatorio
    - **Then** el sistema muestra debajo de ese campo el mensaje "Este campo es obligatorio para continuar con el registro"

7. **Scenario**: Nombre de la embarcación inválido
    - **Given** el propietario escribe un nombre con menos de 3 o más de 50 caracteres, sin ninguna letra, o con caracteres distintos de letras, números, espacios, tildes y guiones
    - **When** el sistema valida el campo en tiempo real
    - **Then** muestra debajo del campo el mensaje "El nombre debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones"

8. **Scenario**: Matrícula legal con formato inválido
    - **Given** el propietario escribe en "Matrícula legal" un valor que no cumple el formato `CP-NN-NNNN-L` (por ejemplo "CP-5-1042-A" o "cp-05-1042-a")
    - **When** el sistema valida el campo en tiempo real
    - **Then** muestra debajo del campo el mensaje "Formato de matrícula legal inválido"; si el propietario hace clic en "Siguiente", el mensaje se mantiene y no avanza al paso 2

9. **Scenario**: Matrícula legal ya existente
    - **Given** el propietario escribe una matrícula con formato válido que ya pertenece a otra embarcación registrada, o al borrador de otro propietario
    - **When** el sistema valida el campo en tiempo real
    - **Then** muestra debajo del campo el mensaje "Matrícula existente en el sistema" y no permite avanzar ni guardar hasta que el propietario la cambie por una que no exista

10. **Scenario**: Matrícula de una embarcación eliminada
    - **Given** el propietario escribe una matrícula con formato válido que perteneció a una embarcación que ya fue eliminada
    - **When** el sistema valida el campo en tiempo real
    - **Then** el sistema acepta la matrícula, porque las matrículas de embarcaciones eliminadas quedan libres

11. **Scenario**: Dos propietarios intentan guardar la misma matrícula casi al mismo tiempo
    - **Given** dos propietarios ingresaron la misma matrícula, que en ese momento no existía, y ambos hacen clic en "Siguiente" casi al mismo tiempo
    - **When** el sistema procesa ambos guardados
    - **Then** el sistema acepta únicamente el primero; al segundo le muestra debajo del campo "Matrícula existente en el sistema" y no avanza al paso 2

12. **Scenario**: Capacidad con valores no numéricos o que no son un entero mayor que 0
    - **Given** el propietario escribe en "Capacidad máxima (pasajeros)"
    - **When** ingresa una letra, un punto, una coma, un signo o el valor 0
    - **Then** el sistema valida el campo en tiempo real, muestra el mensaje debajo del campo y no permite avanzar, según el caso:
        - letra u otro carácter que no es un dígito: el carácter permanece escrito y se muestra "Solo se aceptan valores numéricos"
        - punto, coma o signo: se muestra "Ingrese un número entero sin puntos ni comas"
        - el valor "0": se muestra "Ingrese un número entero mayor que 0"

13. **Scenario**: Capacidad mayor al máximo del tipo seleccionado
    - **Given** el propietario seleccionó el tipo "Lancha" (máximo 10 pasajeros)
    - **When** escribe una capacidad de 11
    - **Then** el sistema muestra debajo del campo "Capacidad máxima (pasajeros)" el mensaje "Este valor excede la capacidad máxima de la embarcación"; si hace clic en "Siguiente", el mensaje se mantiene y no avanza

14. **Scenario**: Capacidad escrita antes de seleccionar el tipo
    - **Given** el propietario no ha seleccionado el tipo de embarcación
    - **When** escribe un valor en "Capacidad máxima (pasajeros)"
    - **Then** el sistema muestra debajo del selector de tipo el mensaje "Este campo es obligatorio para continuar con el registro", que se mantiene (también al hacer clic en "Siguiente") hasta que el propietario seleccione un tipo

15. **Scenario**: Cambio de tipo con capacidad ya escrita
    - **Given** el propietario seleccionó "Catamarán" (máximo 30) y escribió una capacidad de 25
    - **When** cambia el tipo a "Lancha" (máximo 10)
    - **Then** el sistema vuelve a validar la capacidad con el límite del nuevo tipo y muestra "Este valor excede la capacidad máxima de la embarcación" debajo del campo de capacidad

16. **Scenario**: Intento de continuar con errores o campos vacíos
    - **Given** el paso 1 tiene al menos un campo vacío o con error
    - **When** el propietario hace clic en "Siguiente"
    - **Then** el sistema no avanza, muestra (o mantiene) el mensaje correspondiente debajo de cada campo con problema, y repite este comportamiento cada vez que el propietario haga clic en "Siguiente" mientras persistan los errores

17. **Scenario**: Botón "Siguiente" mientras se guarda
    - **Given** el propietario hizo clic en "Siguiente" con el paso 1 válido y el guardado está en proceso
    - **When** el guardado aún no termina
    - **Then** el botón "Siguiente" permanece deshabilitado (sin indicador de carga) y, si el propietario pasa el mouse por encima, aparece un pequeño ícono de prohibido (un círculo rojo con una línea diagonal)

---

### User Story 2 - Completar los datos complementarios y guardar el registro, paso 2 de 2 (Priority: P1)

Como propietario, quiero indicar el puerto de atraque, la tarifa base, la fotografía oficial y los servicios adicionales de mi embarcación y guardar el registro, para que mi embarcación quede disponible en mi flota.

**Why this priority**: Es el paso que completa el registro y deja la embarcación disponible en la flota del propietario. Sin él, el flujo no entrega ningún resultado de negocio.

**Independent Test**: Partiendo de una embarcación en estado Borrador, se completan los campos del paso 2 y se hace clic en "Guardar Registro": aparece el modal de éxito y, al hacer clic en "Aceptar", la lista muestra la nueva embarcación con estado "Disponible" y la etiqueta "NUEVA".

**Acceptance Scenarios**:

1. **Scenario**: Visualización del paso 2
    - **Given** el propietario avanzó desde el paso 1 con datos válidos
    - **When** se muestra el paso 2
    - **Then** el sistema muestra el título "Registrar Nueva Embarcación", el subtítulo "Paso 2 de 2: Puerto de atraque, tarifa, servicios y fotografía oficial." y el indicador de progreso con "Datos Básicos" marcado como completado y "Datos Complementarios" activo, además de los botones "Cancelar" y "Guardar Registro"; el paso 2 no tiene botón para volver al paso 1

2. **Scenario**: Campos y placeholders del paso 2
    - **Given** el propietario acaba de abrir el paso 2
    - **When** observa el formulario
    - **Then** el sistema muestra los campos siguientes, todos vacíos:
        - "Puerto de atraque (selección en mapa)": selector de coordenadas en mapa, sin ningún punto seleccionado
        - "Puerto": placeholder "Ej. Marina Magdalena"
        - "Latitud" y "Longitud": campos sin placeholder, no editables
        - "Tarifa base (COP)": placeholder "Ej. 180000"
        - "Fotografía oficial": botón "+ Adjuntar imagen de la embarcación" y leyenda "PNG, JPG hasta 10MB"

3. **Scenario**: Servicios base y adicionales
    - **Given** el propietario está en el paso 2
    - **When** observa la sección de servicios
    - **Then** el sistema muestra la sección "Servicios base obligatorios (incluidos)" con la descripción "Estos servicios vienen predeterminados y no se pueden remover." y los campos ya completados "Capitán (Obligatorio)" y "Combustible (Obligatorio)", y debajo la sección "Seleccionar servicios adicionales" con cinco casillas sin marcar: Chalecos salvavidas, Nevera con hielo, Equipo de sonido, Equipo de pesca y Equipo de buceo

4. **Scenario**: Vista inicial del mapa
    - **Given** el propietario abre el paso 2
    - **When** se muestra el selector de coordenadas
    - **Then** el mapa se centra en Santa Marta con un nivel de zoom de 12, sin ningún punto seleccionado

5. **Scenario**: Selección de coordenadas en el mapa
    - **Given** el mapa no tiene ningún punto seleccionado
    - **When** el propietario hace clic en el mapa en cualquier ubicación, o arrastra el marcador ya colocado
    - **Then** el sistema muestra el marcador en ese punto y completa automáticamente los campos "Latitud" y "Longitud" con un valor con signo y máximo 4 decimales (el signo indica norte, sur, este u oeste; si el mapa entrega más decimales, se redondea a 4); si el propietario mueve el marcador, ambos campos se actualizan, y no puede editarlos a mano

6. **Scenario**: Coordenadas no seleccionadas
    - **Given** el propietario no ha seleccionado ningún punto en el mapa
    - **When** hace clic en "Guardar Registro"
    - **Then** el sistema muestra debajo de los campos "Latitud" y "Longitud" el mensaje "Las coordenadas son obligatorias" y no guarda el registro

7. **Scenario**: Nombre del puerto vacío o inválido
    - **Given** el propietario deja vacío el campo "Puerto", o escribe un nombre con menos de 3 o más de 50 caracteres, sin ninguna letra, o con caracteres distintos de letras, números, espacios, tildes y guiones
    - **When** sale del campo o hace clic en "Guardar Registro"
    - **Then** si está vacío, el sistema muestra debajo del campo "Este campo es obligatorio para continuar con el registro"; si no cumple las reglas, muestra "El nombre del puerto debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones"; en ambos casos no guarda el registro

8. **Scenario**: Tarifa base vacía o inválida
    - **Given** el propietario deja vacía la tarifa base, escribe 0 o escribe un valor mayor a 9.999.999.999
    - **When** sale del campo o hace clic en "Guardar Registro"
    - **Then** si está vacía, el sistema muestra "Este campo es obligatorio para continuar con el registro"; si es 0, muestra "La tarifa debe ser mayor que 0"; si supera 9.999.999.999, muestra "La tarifa excede el valor permitido"; en todos los casos no guarda el registro

9. **Scenario**: Tarifa base con caracteres no numéricos
    - **Given** el propietario escribe en "Tarifa base (COP)"
    - **When** ingresa una letra u otro carácter que no es un dígito
    - **Then** el sistema deja el carácter escrito en el campo, muestra en tiempo real debajo del campo el mensaje "Solo se aceptan valores numéricos" y no permite guardar mientras el valor no sea numérico; si el carácter ingresado es un punto, una coma o un signo, el mensaje es "Ingrese un número entero sin puntos ni comas"; la tarifa no tiene un máximo de negocio (el propietario define el valor), solo el límite técnico de 9.999.999.999 COP

10. **Scenario**: Adjuntar una fotografía válida
    - **Given** el propietario hace clic en "+ Adjuntar imagen de la embarcación"
    - **When** selecciona una imagen en formato PNG, JPG o JPEG de máximo 10MB
    - **Then** el sistema muestra una vista previa de la imagen dentro del campo, con una "X" en la esquina superior derecha de la vista previa

11. **Scenario**: Fotografía con formato o tamaño inválido
    - **Given** el propietario selecciona un archivo
    - **When** el archivo no es una imagen PNG, JPG o JPEG real (se valida el contenido del archivo y no solo su extensión), o pesa más de 10MB
    - **Then** el sistema no acepta el archivo y muestra debajo del área de carga "Formato de imagen inválido" (si el formato no es válido) o "El tamaño de imagen se excede" (si supera los 10MB)

12. **Scenario**: Reemplazar la fotografía adjuntada
    - **Given** el propietario ya adjuntó una imagen válida
    - **When** adjunta otra imagen válida
    - **Then** el sistema reemplaza la imagen anterior por la nueva y muestra la vista previa de la nueva, sin que el propietario tenga que quitar primero la anterior

13. **Scenario**: Quitar la fotografía adjuntada
    - **Given** el propietario adjuntó una imagen y ve su vista previa
    - **When** hace clic en la "X" de la vista previa
    - **Then** el sistema quita la imagen del campo de inmediato, sin pedir confirmación (la imagen aún no forma parte del registro de la embarcación), y permite adjuntar otra

14. **Scenario**: Fotografía no adjuntada
    - **Given** el propietario no ha adjuntado ninguna imagen
    - **When** hace clic en "Guardar Registro"
    - **Then** el sistema muestra debajo del área de carga el mensaje "Este campo es obligatorio para continuar con el registro" y no guarda el registro

15. **Scenario**: Guardar sin servicios adicionales
    - **Given** el propietario completó correctamente los demás campos y no marcó ninguna casilla de servicios adicionales
    - **When** hace clic en "Guardar Registro"
    - **Then** el sistema guarda el registro sin error, con los servicios Capitán y Combustible incluidos

16. **Scenario**: Intento de guardar con errores o campos vacíos
    - **Given** el paso 2 tiene al menos un campo obligatorio vacío o con error
    - **When** el propietario hace clic en "Guardar Registro"
    - **Then** el sistema no guarda, muestra (o mantiene) el mensaje correspondiente en cada campo con problema, y repite este comportamiento en cada intento mientras persistan los errores

17. **Scenario**: Botón "Guardar Registro" mientras se guarda
    - **Given** el propietario hizo clic en "Guardar Registro" con el paso 2 válido y el guardado está en proceso
    - **When** el guardado aún no termina
    - **Then** el botón "Guardar Registro" permanece deshabilitado (sin indicador de carga) y, si el propietario pasa el mouse por encima, aparece un pequeño ícono de prohibido (un círculo rojo con una línea diagonal)

18. **Scenario**: Guardado exitoso
    - **Given** todos los campos obligatorios del paso 2 están completos y válidos
    - **When** el propietario hace clic en "Guardar Registro"
    - **Then** el sistema registra la embarcación con estado operativo "Disponible" y muestra el modal "¡Embarcación registrada!" con el texto "El nuevo vehículo fluvial ha sido registrado con éxito y se encuentra disponible en tu flota." y el botón "Aceptar"

19. **Scenario**: Regreso a la lista tras el registro
    - **Given** el modal "¡Embarcación registrada!" está visible
    - **When** el propietario hace clic en "Aceptar"
    - **Then** el sistema cierra el modal y muestra la lista "Mis Embarcaciones Registradas", donde la nueva embarcación aparece en primer lugar, con estado "Disponible" y la etiqueta "NUEVA" junto a su nombre

20. **Scenario**: La etiqueta "NUEVA" pasa a la embarcación más reciente
    - **Given** una embarcación muestra la etiqueta "NUEVA"
    - **When** el propietario completa el registro de otra embarcación
    - **Then** la etiqueta "NUEVA" pasa a la embarcación registrada más recientemente y la anterior deja de mostrarla

21. **Scenario**: Vencimiento de la etiqueta "NUEVA"
    - **Given** una embarcación muestra la etiqueta "NUEVA" y el propietario no ha completado el registro de otra
    - **When** transcurren 168 horas exactas desde el momento en que se completó su registro (cuando apareció por primera vez como "Disponible" en la lista)
    - **Then** la etiqueta deja de mostrarse

22. **Scenario**: El borrador fue eliminado desde otra sesión
    - **Given** el propietario tiene abierto el paso 2 y el borrador fue eliminado desde otra pestaña o dispositivo (al empezar un registro nuevo)
    - **When** hace clic en "Guardar Registro"
    - **Then** el sistema no registra la embarcación, muestra un modal con el texto "El borrador ya no existe" y un botón "Aceptar", y al hacer clic en "Aceptar" lleva al propietario a la lista "Mis Embarcaciones Registradas"

---

### User Story 3 - Retomar o descartar una embarcación en estado Borrador (Priority: P2)

Como propietario, quiero ver el modal "Borrador detectado" al hacer clic en "Registrar Nueva Embarcación" cuando tengo un registro incompleto, y poder elegir en ese modal entre continuar el registro pendiente o empezar uno nuevo, para no perder los datos que ya ingresé ni dejar registros a medias en el sistema.

**Why this priority**: Evita que el propietario pierda su trabajo y que queden registros incompletos. No es indispensable para el registro completo en un solo intento, pero sí para una experiencia sin pérdida de datos.

**Independent Test**: Con una embarcación en estado Borrador, hacer clic en "Registrar Nueva Embarcación" y verificar que aparece el modal "Borrador detectado" con el nombre correcto y que cada botón lleva al destino esperado.

**Acceptance Scenarios**:

1. **Scenario**: Detección del borrador
    - **Given** el propietario tiene una embarcación en estado Borrador, por ejemplo con el nombre "Río Magdalena II"
    - **When** hace clic en "Registrar Nueva Embarcación" en la lista
    - **Then** el sistema muestra el modal "Borrador detectado" con el texto "Tienes un registro incompleto guardado bajo el nombre:", el nombre del borrador en un recuadro ("Río Magdalena II"), la pregunta "¿Cómo deseas continuar?", los botones "Empezar un registro nuevo" y "Continuar registro pendiente", y una "X" para cerrar en la esquina superior derecha

2. **Scenario**: Continuar el registro pendiente
    - **Given** el modal "Borrador detectado" está visible
    - **When** el propietario hace clic en "Continuar registro pendiente"
    - **Then** el sistema lo lleva directamente al paso 2 de 2 con los campos del paso 2 vacíos, conservando únicamente los datos del paso 1 (cualquier dato que hubiera dejado a medias en el paso 2 no se conserva)

3. **Scenario**: Empezar un registro nuevo (advertencia)
    - **Given** el modal "Borrador detectado" está visible
    - **When** el propietario hace clic en "Empezar un registro nuevo"
    - **Then** el sistema muestra el modal "¿Iniciar nuevo registro?" con el texto "Si inicias un formulario nuevo, el borrador actual se eliminará de forma permanente y no podrás recuperar la información diligenciada." (con "se eliminará de forma permanente" resaltado) y los botones "Regresar" y "Sí, eliminar y empezar"

4. **Scenario**: Regresar desde la advertencia
    - **Given** el modal "¿Iniciar nuevo registro?" está visible
    - **When** el propietario hace clic en "Regresar"
    - **Then** el sistema vuelve al modal "Borrador detectado" y el borrador se conserva sin cambios

5. **Scenario**: Confirmar la eliminación del borrador
    - **Given** el modal "¿Iniciar nuevo registro?" está visible
    - **When** el propietario hace clic en "Sí, eliminar y empezar"
    - **Then** el sistema elimina el borrador de forma permanente y muestra el paso 1 de 2 con todos los campos vacíos

6. **Scenario**: Cerrar el modal de borrador con la "X"
    - **Given** el modal "Borrador detectado" está visible
    - **When** el propietario hace clic en la "X"
    - **Then** el sistema cierra el modal y muestra la lista "Mis Embarcaciones Registradas" (la misma vista que ve al ingresar al sistema), y el borrador se conserva

7. **Scenario**: Corregir un dato del paso 1 en una embarcación en estado Borrador
    - **Given** el propietario tiene un borrador y se equivocó en un dato del paso 1 (por ejemplo, una letra de la matrícula, que sigue teniendo formato válido)
    - **When** quiere corregirlo
    - **Then** el sistema no permite editar los datos del paso 1 del borrador; la única manera de corregirlos es hacer clic en "Empezar un registro nuevo" y confirmar "Sí, eliminar y empezar", lo que elimina el borrador y abre un paso 1 vacío

8. **Scenario**: Borrador tras expirar la sesión
    - **Given** el propietario tiene un borrador y su sesión expiró mientras estaba en el paso 2
    - **When** inicia sesión de nuevo y hace clic en "Registrar Nueva Embarcación"
    - **Then** el sistema muestra el modal "Borrador detectado", porque el borrador no vence y se conserva

9. **Scenario**: Cierre de los modales
    - **Given** cualquiera de los modales del flujo está visible ("Borrador detectado", "¿Iniciar nuevo registro?", "¡Embarcación registrada!", "No se pudo guardar el registro", "El borrador ya no existe", "No se pudo eliminar el borrador" o "No se pudo cargar el registro pendiente")
    - **When** el propietario hace clic fuera del modal o presiona la tecla Esc
    - **Then** el modal no se cierra; solo se cierra con sus propios botones (y con la "X" en el modal "Borrador detectado")

### Edge Cases

- **¿Qué pasa si ocurre un error técnico (por ejemplo, falla de conexión) al hacer clic en "Siguiente" en el paso 1?** El sistema muestra el modal "No se pudo guardar el registro" con el texto "Ocurrió un problema técnico al guardar la información de la embarcación. Verifique su conexión a internet e inténtelo nuevamente." y los botones "Cerrar" y "Reintentar". Los datos de los campos permanecen intactos.
- **¿Qué pasa si ocurre un error técnico al hacer clic en "Guardar Registro" en el paso 2?** El sistema muestra el mismo modal "No se pudo guardar el registro". Los datos del paso 2 y los del paso 1 permanecen intactos.
- **¿Qué pasa si falla la verificación de la matrícula por un problema técnico mientras el propietario la escribe?** El sistema muestra el modal "No se pudo guardar el registro" y no permite avanzar hasta que la verificación funcione.
- **¿Qué pasa si el propietario hace clic en "Reintentar" en el modal de error?** El sistema vuelve a intentar el guardado con los mismos datos. Si funciona, continúa el flujo normal (paso 2 o modal de éxito). Si falla otra vez, muestra de nuevo el mismo modal, tantas veces como falle.
- **¿Qué pasa si el propietario hace clic en "Cerrar" en el modal de error?** El sistema cierra el modal y lo deja en el mismo formulario con todos los datos ingresados intactos (los datos del paso 1 siempre se conservan). Puede volver a hacer clic en "Siguiente" o en "Guardar Registro" cuando se solucione el problema.
- **¿Qué pasa si un intento de guardado se completó pero la confirmación no llegó al propietario, y este reintenta?** El sistema no crea una embarcación duplicada; el resultado final es un único registro. Lo mismo aplica si hace doble clic en "Guardar Registro" (el botón se deshabilita mientras se guarda).
- **¿Qué pasa si el propietario escribe espacios al inicio o al final del nombre de la embarcación, de la matrícula legal o del puerto?** El sistema los elimina automáticamente; en los nombres no cuentan para el mínimo de 3 ni el máximo de 50 caracteres.
- **¿Qué pasa si el nombre de la embarcación coincide con el de otra?** Se permite; el nombre puede repetirse entre embarcaciones. La unicidad se exige solo en la matrícula legal.
- **¿Qué pasa si el nombre incluye números o numerales romanos, ñ, ü o tildes?** Se permiten (por ejemplo "Río Magdalena II", "Río Magdalena 2" o "Peñón del Norte"), siempre que el nombre contenga al menos una letra. Un nombre como "123" o "---" no es válido.
- **¿Qué pasa si la matrícula tiene formato inválido y además ya existe?** El sistema muestra primero el mensaje de formato inválido; el mensaje de matrícula existente solo se evalúa cuando el formato es válido.
- **¿Qué pasa si el propietario hace clic en "Siguiente" con todos los campos vacíos?** No avanza y muestra "Este campo es obligatorio para continuar con el registro" debajo de cada campo vacío.
- **¿Qué pasa si cambia el tipo después de escribir la capacidad?** La capacidad se vuelve a validar con el límite del nuevo tipo.
- **¿Qué pasa si adjunta un archivo de más de 10MB que además no es una imagen válida?** No se acepta y se muestra "Formato de imagen inválido".
- **¿Qué pasa si quita la imagen y luego hace clic en "Guardar Registro"?** Se trata como fotografía no adjuntada: "Este campo es obligatorio para continuar con el registro".
- **¿Qué pasa si el propietario selecciona un punto del mapa fuera de Santa Marta?** Se acepta: el mapa inicia en Santa Marta (zoom 12) solo como vista inicial, pero el propietario elige libremente la ubicación y los campos "Latitud" y "Longitud" se completan con ese punto.
- **¿Qué pasa si el nombre del puerto ya existe en el sistema?** La embarcación se asocia al puerto existente (sin distinguir mayúsculas ni tildes) y se conservan las coordenadas originales de ese puerto; las coordenadas elegidas en el mapa para esta embarcación no se guardan aparte. Un mismo puerto puede tener varias embarcaciones.
- **¿Qué pasa si la tarifa supera 9.999.999.999?** Se muestra "La tarifa excede el valor permitido" debajo del campo y no se guarda el registro.
- **¿Qué pasa si el propietario ya tiene un borrador e intenta empezar otro registro?** El sistema muestra siempre el modal "Borrador detectado": debe continuar el borrador o eliminarlo antes de empezar un registro nuevo.
- **¿Qué pasa si el propietario cancela estando en el paso 2?** La embarcación queda en estado Borrador con los datos del paso 1. El comportamiento del botón "Cancelar" se especifica en el spec "Cancelar registro".
- **¿Qué pasa si la sesión del propietario expira con el paso 2 abierto?** El borrador se conserva y el propietario lo encuentra al volver a hacer clic en "Registrar Nueva Embarcación".
- **¿Qué pasa si el propietario recarga la página (o la cierra) en el paso 1 sin haber hecho clic en "Siguiente"?** Se pierde la información escrita: los campos quedan vacíos y no se crea ningún borrador.
- **¿Qué pasa si el propietario recarga la página (o la cierra) en el paso 2?** El borrador se conserva con los datos del paso 1; los datos escritos en el paso 2 no se conservan.
- **¿Qué pasa si la sesión expira cuando el propietario hace clic en "Siguiente" o en "Guardar Registro"?** El sistema lo lleva al inicio de sesión conservando el borrador existente.
- **¿Qué pasa si el propietario abandona un borrador y nunca lo retoma?** El borrador no vence y su matrícula legal sigue reservada, por lo que ningún otro propietario puede usarla (comportamiento intencional).
- **¿Qué pasa si el mapa entrega una coordenada con más de 4 decimales?** El sistema redondea el valor a 4 decimales.
- **¿Qué pasa si el propietario escribe ceros a la izquierda en la capacidad o la tarifa (por ejemplo "007")?** Se aceptan y se guarda el valor numérico (7).
- **¿Qué pasa si falla la eliminación del borrador al hacer clic en "Sí, eliminar y empezar"?** El sistema muestra el modal "No se pudo eliminar el borrador" y el borrador se conserva (ver FR-073).
- **¿Qué pasa si falla la carga del borrador al hacer clic en "Continuar registro pendiente"?** El sistema muestra el modal "No se pudo cargar el registro pendiente" (ver FR-074).
- **¿Qué pasa si no cargan los tipos de embarcación o el mapa?** El sistema muestra el mensaje de error correspondiente (ver FR-075 y FR-076).

## Requirements *(mandatory)*

### Functional Requirements

#### Acceso y lista de embarcaciones

- **FR-001**: Al iniciar sesión, el propietario DEBE ver la lista "Mis Embarcaciones Registradas" con el subtítulo "Gestiona el estado operativo y consulta los detalles de tus vehículos fluviales", mostrando únicamente sus propias embarcaciones.
- **FR-002**: La lista DEBE mostrar por cada embarcación: nombre de la embarcación, matrícula, tipo, estado operativo (por ejemplo "Disponible" o "En Mantenimiento") y una columna "Acciones" con el botón "Ver Detalles".
- **FR-003**: La lista DEBE ordenarse de la embarcación más reciente a la más antigua según la fecha en que se completó su registro: la última en completar su registro aparece primero.
- **FR-004**: Si el propietario no tiene embarcaciones registradas, la lista DEBE mostrar el mensaje "Aún no tienes embarcaciones registradas".
- **FR-005**: La lista DEBE incluir el botón "Registrar Nueva Embarcación".
- **FR-006**: Las embarcaciones en estado Borrador NO DEBEN mostrarse en la lista.
- **FR-007**: Al hacer clic en "Registrar Nueva Embarcación", el sistema DEBE mostrar el paso 1 si el propietario no tiene un borrador, o el modal "Borrador detectado" si lo tiene.

#### Estructura del formulario y progreso

- **FR-008**: El formulario DEBE tener el título "Registrar Nueva Embarcación" y un indicador de progreso de dos pasos: "1 – Datos Básicos" y "2 – Datos Complementarios".
- **FR-009**: En el paso 1, el subtítulo DEBE ser "Paso 1 de 2: Información general y datos legales del vehículo fluvial." y el indicador DEBE mostrar el paso 1 activo y el paso 2 inactivo.
- **FR-010**: En el paso 2, el subtítulo DEBE ser "Paso 2 de 2: Puerto de atraque, tarifa, servicios y fotografía oficial." y el indicador DEBE mostrar el paso 1 como completado (con marca de verificación) y el paso 2 activo.
- **FR-011**: El indicador de progreso es solo informativo: no permite navegar entre pasos, y el paso 2 no tiene botón para volver al paso 1.
- **FR-012**: Cada paso DEBE mostrar el botón "Cancelar" y el botón de avance ("Siguiente" en el paso 1, "Guardar Registro" en el paso 2). El comportamiento de "Cancelar" queda fuera de este spec.

#### Paso 1 – Datos Básicos

- **FR-013**: El sistema DEBE mostrar el campo **"Nombre de la embarcación"**, obligatorio, con placeholder "Ej. Río Magdalena II".
- **FR-014**: El nombre DEBE tener entre 3 y 50 caracteres, contener al menos una letra y solo incluir letras (incluidas ñ, ü y tildes), números (incluidos numerales romanos), espacios y el guion "-". Si no cumple, el sistema DEBE mostrar debajo del campo: "El nombre debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones".
- **FR-015**: El sistema DEBE eliminar los espacios sobrantes al inicio y al final del nombre antes de validarlo y guardarlo.
- **FR-016**: El nombre PUEDE repetirse entre embarcaciones.
- **FR-017**: El sistema DEBE mostrar el campo **"Matrícula legal"**, obligatorio, con placeholder "Ej. CP-05-XXXX-A".
- **FR-018**: La matrícula legal DEBE cumplir el formato: las letras "CP", un guion, 2 dígitos, un guion, 4 dígitos, un guion y 1 letra mayúscula (por ejemplo "CP-05-1042-A"). Si no lo cumple, el sistema DEBE mostrar en tiempo real debajo del campo: "Formato de matrícula legal inválido", y mantener el mensaje si el propietario intenta continuar. El sistema DEBE eliminar los espacios al inicio y al final de la matrícula antes de validarla y guardarla.
- **FR-019**: La matrícula legal DEBE ser única en el sistema. El sistema DEBE verificar en tiempo real, mientras el propietario escribe y una vez que el formato es válido, que la matrícula no pertenezca a otra embarcación registrada ni al borrador de otro propietario; si ya existe, DEBE mostrar debajo del campo: "Matrícula existente en el sistema" y NO DEBE permitir avanzar ni guardar hasta que se cambie. La matrícula de un borrador permanece reservada para su propietario mientras el borrador exista, aunque haya quedado abandonado (comportamiento intencional).
- **FR-020**: Las matrículas de embarcaciones eliminadas DEBEN quedar libres para usarse en otra embarcación.
- **FR-021**: Si dos propietarios intentan guardar la misma matrícula casi al mismo tiempo, el sistema DEBE aceptar únicamente al primero y mostrar al segundo "Matrícula existente en el sistema".
- **FR-022**: El sistema DEBE mostrar el campo **"Tipo de embarcación"** como un selector con los tipos previamente cargados (Lancha, Yate, Catamarán y Velero), con placeholder "Seleccionar tipo (Lancha, Yate, etc.)". El campo es obligatorio.
- **FR-023**: El sistema DEBE mostrar el campo **"Capacidad máxima (pasajeros)"**, obligatorio, con placeholder "Ej. 10", que solo admite números enteros mayores que 0.
- **FR-024**: La capacidad máxima permitida DEBE depender del tipo seleccionado: Lancha ≤ 10, Yate ≤ 20, Catamarán ≤ 30, Velero ≤ 12 (en todos los casos mayor que 0).
- **FR-025**: Si la capacidad escrita supera el máximo del tipo seleccionado, el sistema DEBE mostrar debajo del campo de capacidad: "Este valor excede la capacidad máxima de la embarcación", mantener el mensaje si el propietario intenta continuar y NO permitir avanzar.
- **FR-026**: La capacidad DEBE ser un valor numérico entero, sin puntos ni comas. El sistema DEBE validarla en tiempo real y mostrar debajo del campo, según el caso: si el propietario ingresa una letra u otro carácter que no es un dígito, DEBE dejarlo escrito y mostrar "Solo se aceptan valores numéricos"; si ingresa un punto, una coma o un signo, "Ingrese un número entero sin puntos ni comas"; si el valor es 0, "Ingrese un número entero mayor que 0". En todos los casos NO DEBE permitir avanzar mientras el valor no sea válido. Los ceros a la izquierda se aceptan y se guarda el valor numérico (por ejemplo, "007" se guarda como 7).
- **FR-027**: Si el propietario escribe la capacidad sin haber seleccionado el tipo, el sistema DEBE mostrar debajo del selector de tipo "Este campo es obligatorio para continuar con el registro" y mantenerlo (incluso al intentar continuar) hasta que se seleccione un tipo.
- **FR-028**: Al cambiar el tipo, el sistema DEBE volver a validar la capacidad ya escrita con el límite del nuevo tipo.

#### Validaciones generales del formulario

- **FR-029**: Las validaciones DEBEN ejecutarse en tiempo real, mostrando el mensaje de error debajo del campo afectado.
- **FR-030**: Cuando el propietario pasa a otro campo sin haber escrito nada en un campo obligatorio, el sistema DEBE mostrar debajo de ese campo: "Este campo es obligatorio para continuar con el registro".
- **FR-031**: Mientras exista un campo vacío o con error, los botones "Siguiente" (paso 1) y "Guardar Registro" (paso 2) NO DEBEN permitir continuar ni guardar; en cada intento, el sistema DEBE mostrar o mantener el mensaje correspondiente en cada campo con problema. Los botones permanecen activos (no se deshabilitan por errores del formulario).
- **FR-032**: Un mensaje de error DEBE desaparecer cuando el valor del campo pasa a ser válido.
- **FR-033**: Mientras un guardado está en proceso (al hacer clic en "Siguiente" o en "Guardar Registro"), el botón correspondiente DEBE permanecer deshabilitado, sin indicador de carga, y al pasar el mouse por encima DEBE mostrar un pequeño ícono de prohibido (un círculo rojo con una línea diagonal).

#### Estado Borrador

- **FR-034**: Al hacer clic en "Siguiente" con el paso 1 válido, el sistema DEBE guardar los datos del paso 1, dejar la embarcación en estado Borrador y mostrar el paso 2.
- **FR-035**: La embarcación DEBE permanecer en estado Borrador hasta que el propietario complete el registro (paso 2) o elimine el borrador de forma permanente al empezar un registro nuevo.
- **FR-036**: Cada propietario DEBE tener como máximo una embarcación en estado Borrador a la vez, y el sistema DEBE exigirle continuarla o eliminarla antes de iniciar un registro nuevo.
- **FR-037**: Una embarcación en estado Borrador DEBE conservar únicamente los datos del paso 1. Los datos que el propietario haya dejado a medias en el paso 2 NO DEBEN conservarse al retomar el borrador.
- **FR-038**: Los datos del paso 1 de una embarcación en estado Borrador NO DEBEN ser editables; la única manera de corregirlos es eliminar el borrador y empezar un registro nuevo.
- **FR-039**: El estado Borrador NO DEBE vencer con el tiempo y DEBE conservarse aunque la sesión del propietario expire. Si la sesión expira cuando el propietario hace clic en "Siguiente" o en "Guardar Registro", el sistema DEBE llevarlo al inicio de sesión conservando el borrador existente.

#### Paso 2 – Datos Complementarios

- **FR-040**: El sistema DEBE mostrar la sección **"Puerto de atraque (selección en mapa)"** con un selector de coordenadas en mapa que inicia centrado en Santa Marta, con nivel de zoom 12 y sin ningún punto seleccionado.
- **FR-041**: El propietario DEBE poder seleccionar el punto haciendo clic en el mapa o arrastrando el marcador. No existe un área restringida: puede seleccionar cualquier ubicación del mapa.
- **FR-042**: Al seleccionar o mover un punto, el sistema DEBE mostrar un marcador en ese punto y completar automáticamente los campos "Latitud" y "Longitud". Ambos valores DEBEN mostrarse y guardarse con signo (el signo indica norte, sur, este u oeste) y con un máximo de 4 decimales. Si el mapa entrega más de 4 decimales, el sistema DEBE redondear el valor a 4 decimales.
- **FR-043**: Los campos "Latitud" y "Longitud" DEBEN ser de solo lectura y no tienen placeholder: el propietario no puede editarlos a mano, solo mediante el selector de coordenadas.
- **FR-044**: Si no se ha seleccionado ningún punto, el sistema DEBE mostrar debajo de los campos "Latitud" y "Longitud": "Las coordenadas son obligatorias" y NO permitir guardar.
- **FR-045**: El sistema DEBE mostrar el campo de texto **"Puerto"**, obligatorio, donde el propietario escribe el nombre del puerto, con placeholder "Ej. Marina Magdalena". Si ya existe un puerto con ese nombre (sin distinguir mayúsculas ni tildes), el sistema DEBE asociar la embarcación a ese puerto y conservar sus coordenadas originales: un mismo puerto puede tener varias embarcaciones atracadas.
- **FR-046**: El nombre del puerto DEBE cumplir las mismas reglas que el nombre de la embarcación (FR-014 y FR-015): entre 3 y 50 caracteres; al menos una letra; solo letras, números, espacios, tildes y guiones; sin espacios sobrantes al inicio ni al final. Si no cumple, el sistema DEBE mostrar debajo del campo: "El nombre del puerto debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones". Si está vacío, DEBE mostrar: "Este campo es obligatorio para continuar con el registro".
- **FR-047**: El sistema DEBE mostrar el campo **"Tarifa base (COP)"**, obligatorio, con placeholder "Ej. 180000". En pantalla la etiqueta se muestra como "Tarifa base (COP)"; el dato se identifica como "tarifa base".
- **FR-048**: La tarifa base DEBE ser un número entero mayor que 0, sin decimales ni separadores de miles. La tarifa NO tiene un máximo de negocio (el propietario define el valor); solo se aplica el límite técnico de 9.999.999.999 COP y, si lo supera, el sistema DEBE mostrar debajo del campo "La tarifa excede el valor permitido" y NO permitir guardar. Si el propietario ingresa un carácter que no es un dígito (por ejemplo, una letra), el sistema DEBE dejarlo escrito en el campo, mostrar en tiempo real debajo del campo "Solo se aceptan valores numéricos" y NO permitir guardar mientras el valor no sea numérico. Si el carácter ingresado es un punto, una coma o un signo, el mensaje DEBE ser "Ingrese un número entero sin puntos ni comas". Si el valor es 0, DEBE mostrar "La tarifa debe ser mayor que 0"; si está vacía, "Este campo es obligatorio para continuar con el registro". Los ceros a la izquierda se aceptan y se guarda el valor numérico (por ejemplo, "0180000" se guarda como 180000).
- **FR-049**: El sistema DEBE mostrar el campo **"Fotografía oficial"**, obligatorio, con el botón "+ Adjuntar imagen de la embarcación" y la leyenda "PNG, JPG hasta 10MB".
- **FR-050**: La fotografía DEBE ser una imagen PNG, JPG o JPEG real de máximo 10MB (10 × 1024 × 1024 bytes, es decir, 10.485.760 bytes), y solo se puede adjuntar una imagen a la vez; el sistema DEBE validar el contenido del archivo y no solo su extensión. Si el formato no es válido, el sistema DEBE mostrar debajo del área de carga "Formato de imagen inválido"; si el tamaño supera el máximo, "El tamaño de imagen se excede". En ambos casos el archivo no se acepta.
- **FR-051**: Al adjuntar una imagen válida, el sistema DEBE mostrar una vista previa con una "X" en su esquina superior derecha. Si el propietario adjunta otra imagen válida, DEBE reemplazar la anterior.
- **FR-052**: Al hacer clic en la "X", el sistema DEBE quitar la imagen del campo inmediatamente, sin solicitar confirmación, y permitir adjuntar otra.
- **FR-053**: Si no hay imagen adjunta, el sistema DEBE mostrar debajo del área de carga "Este campo es obligatorio para continuar con el registro" y NO permitir guardar.
- **FR-054**: El sistema DEBE mostrar la sección **"Servicios base obligatorios (incluidos)"** con la descripción "Estos servicios vienen predeterminados y no se pueden remover." y los campos de solo lectura "Capitán (Obligatorio)" y "Combustible (Obligatorio)". Toda embarcación DEBE quedar registrada con estos dos servicios incluidos.
- **FR-055**: El sistema DEBE mostrar la sección **"Seleccionar servicios adicionales"** con cinco casillas que el propietario puede marcar y desmarcar: Chalecos salvavidas, Nevera con hielo, Equipo de sonido, Equipo de pesca y Equipo de buceo. Al abrir el paso 2 ninguna está marcada.
- **FR-056**: Los servicios adicionales son opcionales: NO tienen validación y el propietario puede guardar sin marcar ninguno.

#### Guardado final y confirmación

- **FR-057**: Al hacer clic en "Guardar Registro" con todos los datos válidos, el sistema DEBE registrar la embarcación con el estado operativo "Disponible", sustituyendo el estado Borrador.
- **FR-058**: Tras el registro exitoso, el sistema DEBE mostrar el modal "¡Embarcación registrada!" con el texto "El nuevo vehículo fluvial ha sido registrado con éxito y se encuentra disponible en tu flota." y el botón "Aceptar".
- **FR-059**: Al hacer clic en "Aceptar", el sistema DEBE cerrar el modal y mostrar la lista "Mis Embarcaciones Registradas" con la nueva embarcación en primer lugar y la etiqueta "NUEVA" junto a su nombre.
- **FR-060**: La etiqueta "NUEVA" DEBE mostrarse únicamente en la embarcación registrada más recientemente. DEBE desaparecer cuando transcurran 168 horas exactas desde el momento en que se completó su registro (cuando apareció por primera vez como "Disponible" en la lista), o antes si el propietario completa el registro de otra embarcación, caso en el que pasa a la más reciente.
- **FR-061**: Si el borrador fue eliminado desde otra sesión y el propietario hace clic en "Guardar Registro", el sistema NO DEBE registrar la embarcación y DEBE mostrar un modal con el texto "El borrador ya no existe" y un botón "Aceptar", que lleva al propietario a la lista "Mis Embarcaciones Registradas".

#### Errores técnicos

- **FR-062**: Si ocurre un error técnico al guardar (al hacer clic en "Siguiente" en el paso 1 o en "Guardar Registro" en el paso 2), o falla la verificación de la matrícula, el sistema DEBE mostrar un modal con el título "No se pudo guardar el registro", el texto "Ocurrió un problema técnico al guardar la información de la embarcación. Verifique su conexión a internet e inténtelo nuevamente." y los botones "Cerrar" y "Reintentar".
- **FR-063**: En caso de error técnico, el sistema DEBE conservar todos los datos ingresados en el formulario; los datos del paso 1 siempre se conservan.
- **FR-064**: "Reintentar" DEBE volver a intentar el guardado con los mismos datos; si falla de nuevo, DEBE mostrarse el mismo modal tantas veces como falle. "Cerrar" DEBE cerrar el modal y dejar al propietario en el mismo formulario con sus datos intactos.
- **FR-065**: El sistema NO DEBE crear embarcaciones duplicadas cuando un intento de guardado se completó pero la confirmación no llegó al propietario, ni cuando se hace clic varias veces en el botón de guardado.

#### Modales de borrador

- **FR-066**: Si el propietario tiene una embarcación en estado Borrador, al hacer clic en "Registrar Nueva Embarcación" el sistema DEBE mostrar el modal "Borrador detectado" con el texto "Tienes un registro incompleto guardado bajo el nombre:", el nombre del borrador en un recuadro, la pregunta "¿Cómo deseas continuar?", los botones "Empezar un registro nuevo" y "Continuar registro pendiente", y una "X" de cierre.
- **FR-067**: "Continuar registro pendiente" DEBE llevar al propietario al paso 2 de 2, conservando únicamente los datos del paso 1.
- **FR-068**: "Empezar un registro nuevo" DEBE abrir el modal "¿Iniciar nuevo registro?" con el texto "Si inicias un formulario nuevo, el borrador actual se eliminará de forma permanente y no podrás recuperar la información diligenciada." y los botones "Regresar" y "Sí, eliminar y empezar".
- **FR-069**: "Regresar" DEBE volver al modal "Borrador detectado" sin modificar el borrador.
- **FR-070**: "Sí, eliminar y empezar" DEBE eliminar el borrador de forma permanente y mostrar el paso 1 con todos los campos vacíos.
- **FR-071**: La "X" del modal "Borrador detectado" DEBE cerrarlo y mostrar la lista "Mis Embarcaciones Registradas" (la misma vista inicial al ingresar al sistema), conservando el borrador.
- **FR-072**: Ningún modal del flujo DEBE cerrarse al hacer clic fuera de él ni al presionar la tecla Esc; solo se cierran con sus propios botones (y con la "X" en el modal "Borrador detectado").

#### Errores técnicos en otras acciones

- **FR-073**: Si falla la eliminación del borrador al hacer clic en "Sí, eliminar y empezar", el sistema DEBE mostrar un modal con el título "No se pudo eliminar el borrador", el texto "Ocurrió un problema técnico al eliminar el borrador. Verifique su conexión a internet e inténtelo nuevamente." y los botones "Cerrar" y "Reintentar". El borrador DEBE conservarse. "Reintentar" DEBE volver a intentar la eliminación y "Cerrar" DEBE regresar al modal "Borrador detectado".
- **FR-074**: Si falla la carga del borrador al hacer clic en "Continuar registro pendiente", el sistema DEBE mostrar un modal con el título "No se pudo cargar el registro pendiente", el texto "Ocurrió un problema técnico al cargar el borrador. Verifique su conexión a internet e inténtelo nuevamente." y los botones "Cerrar" y "Reintentar". "Reintentar" DEBE volver a cargar el borrador y "Cerrar" DEBE regresar al modal "Borrador detectado".
- **FR-075**: Si no se pueden cargar los tipos de embarcación, el sistema DEBE mostrar debajo del selector el mensaje "No se pudieron cargar los tipos de embarcación. Inténtelo nuevamente.", volver a intentar la carga cada vez que el propietario haga clic en el selector y NO permitir avanzar mientras no haya un tipo seleccionado.
- **FR-076**: Si no se puede cargar el mapa, el sistema DEBE mostrar en el área del mapa el mensaje "No se pudo cargar el mapa. Verifique su conexión a internet e inténtelo nuevamente." con un botón "Reintentar", y NO permitir guardar el registro mientras no haya coordenadas seleccionadas.

#### Requisitos no funcionales

- **NFR-001** (Seguridad): Solo un propietario autenticado DEBE poder acceder a este flujo, y cada propietario DEBE ver y operar únicamente sobre sus propias embarcaciones y su propio borrador.
- **NFR-002** (Integridad): La unicidad de la matrícula legal DEBE garantizarse aunque dos propietarios intenten guardarla al mismo tiempo (FR-021), y no DEBEN existir embarcaciones duplicadas por reintentos (FR-065).
- **NFR-003** (Persistencia): Los datos del paso 1 DEBEN quedar guardados al avanzar al paso 2, de modo que no se pierdan si el propietario cierra la aplicación o su sesión expira.
- **NFR-004** (Rendimiento): Los mensajes de validación DEBEN mostrarse en menos de 1 segundo desde que el propietario escribe o sale de un campo.
- **NFR-005** (Usabilidad): Todos los textos, mensajes de error y modales DEBEN mostrarse en español, claros y breves, debajo del campo afectado cuando se trate de validaciones.

### Key Entities *(include if feature involves data)*

- **Embarcación**: Vehículo fluvial registrado por un propietario. Atributos: nombre (3–50 caracteres), matrícula legal (única en el sistema), tipo, capacidad máxima de pasajeros, puerto de atraque (nombre, latitud y longitud con signo y hasta 4 decimales), tarifa base (entero en COP, mayor que 0 y hasta 9.999.999.999), fotografía oficial, servicios incluidos (base y adicionales), estado operativo (Borrador al completar solo el paso 1; Disponible al completar el registro) y fecha en que se completó el registro (la primera vez que pasó a Disponible). Pertenece a un propietario.
- **Propietario**: Usuario autenticado dueño de las embarcaciones que registra. Atributos: nombre, correo electrónico, teléfono de contacto, documento de identidad y fecha de registro en el sistema. Es dueño de una o varias embarcaciones y tiene como máximo una embarcación en estado Borrador a la vez.
- **Tipo de embarcación**: Categoría preconfigurada (Lancha, Yate, Catamarán, Velero) que define la capacidad máxima de pasajeros permitida: Lancha 10, Yate 20, Catamarán 30, Velero 12.
- **Servicio**: Prestación incluida con la embarcación. Servicios base (Capitán, Combustible): se asignan siempre y no se pueden quitar. Servicios adicionales opcionales: Chalecos salvavidas, Nevera con hielo, Equipo de sonido, Equipo de pesca, Equipo de buceo.

- **Puerto**: Lugar de atraque (nombre, latitud y longitud). El nombre es único en el sistema: varias embarcaciones pueden atracar en el mismo puerto, y al registrar una embarcación con el nombre de un puerto existente se reutiliza ese puerto.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un propietario puede completar el registro completo (pasos 1 y 2, hasta ver el modal de éxito) en menos de 5 minutos, sin ayuda externa, con los datos a la mano.
- **SC-002**: El 100% de los intentos de avanzar o guardar con campos vacíos o inválidos son bloqueados y muestran el mensaje de error correspondiente debajo del campo afectado.
- **SC-003**: Los mensajes de validación aparecen en menos de 1 segundo tras salir de un campo o escribir en él.
- **SC-004**: 0 embarcaciones con matrícula legal duplicada en el sistema (incluso con guardados simultáneos), y 0 registros duplicados tras reintentos de guardado por errores técnicos.
- **SC-005**: El 100% de las embarcaciones en estado Borrador pueden retomarse desde el modal "Borrador detectado" conservando los datos del paso 1, incluso después de que la sesión expire, y el 100% de los borradores eliminados con "Sí, eliminar y empezar" dejan de existir de inmediato.
- **SC-006**: El 100% de las embarcaciones registradas con éxito aparecen en la lista del propietario con estado "Disponible", etiqueta "NUEVA" y en primer lugar inmediatamente después de hacer clic en "Aceptar".
- **SC-007**: Ante un error técnico al guardar, el 100% de los datos ingresados se conservan y el propietario puede reintentar sin volver a llenar el formulario.
- **SC-008**: El 90% de los propietarios completa el registro en su primer intento sin recibir un mensaje de error que no pueda corregir por sí mismo.
