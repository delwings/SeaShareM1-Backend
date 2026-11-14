### Test Case Specification: Paso 2 – Datos Complementarios, Mapa, Multimedia y Registro Final
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/registrar-embarcacion/spec.md` | `docs/features/registrar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-003-Paso2DatosComplementarios
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la interfaz y comportamiento del "Paso 2 de 2: Datos Complementarios", incluyendo el selector de mapa (Leaflet centrado en Santa Marta), redondeo y solo lectura de coordenadas, validación y reutilización de puertos, validaciones de tarifa base (COP), carga/reemplazo/eliminación de fotografía oficial (validación por firma de bytes y límite de 10 MB), servicios obligatorios y adicionales, y la finalización exitosa del registro hacia el estado "Disponible" (`AVAILABLE`) con etiqueta "NUEVA".

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Registrar Embarcación | Specification Document | `docs/features/registrar-embarcacion/spec.md` |
| Implementation Plan: Registrar Embarcación | Plan Document | `docs/features/registrar-embarcacion/plan.md` |
| Contrato REST: Registrar Embarcación | API Contract | `docs/architecture/contracts/rest/registrar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* El comportamiento del botón "Cancelar" en el Paso 2 se documenta en un spec independiente.

##### Dependencies & Assumptions
* El Paso 1 (`TC-002`) debe haber finalizado dejando una embarcación en estado `DRAFT` para el propietario autenticado.
* Credenciales de Cloudinary configuradas en el backend (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`) para almacenar la fotografía oficial.
* Archivos de prueba preparados:
  * `lancha_valida.jpg` (imagen JPEG real < 10 MB).
  * `lancha_reemplazo.png` (imagen PNG real < 10 MB).
  * `documento_falso.jpg` (archivo PDF o TXT renombrado a `.jpg`).
  * `imagen_pesada.png` (archivo mayor a 10 MB / 10.485.760 bytes).

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado ubicado en el Paso 2 de 2 (`registerBoatDetails`) con un borrador activo.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Estructura visual, Mapa y Servicios (Escenarios US2-1 a US2-5)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar cabecera, indicador y navegación | Observar la pantalla al entrar al Paso 2 | N/A | Subtítulo **"Paso 2 de 2: Puerto de atraque, tarifa, servicios y fotografía oficial."**, Paso 1 con marca de completado, Paso 2 activo, botones **"Cancelar"** y **"Guardar Registro"**, y **sin botón para volver al Paso 1** (FR-010, FR-011). | [ ] | [ ] |
| 2 | Verificar vista inicial del mapa y campos | Revisar el componente de mapa y campos de ubicación | N/A | Mapa centrado en **Santa Marta** con zoom **12** y sin marcador; campo **"Puerto"** vacío (`Ej. Marina Magdalena`); campos **"Latitud"** y **"Longitud"** vacíos, sin placeholder y de **solo lectura** (no editables manualmente) (FR-040, FR-043). | [ ] | [ ] |
| 3 | Seleccionar y arrastrar marcador en el mapa | Hacer clic en un punto del mapa y luego arrastrar el marcador | Clic en cualquier ubicación | Aparece el marcador y se autocompletan **"Latitud"** y **"Longitud"** con signo y **máximo 4 decimales** (redondeados si el mapa da más). Al arrastrar el marcador, ambos campos se actualizan (FR-041, FR-042). | [ ] | [ ] |
| 4 | Verificar sección de Servicios | Observar los servicios base y adicionales | N/A | Sección **"Servicios base obligatorios (incluidos)"** con texto informativo y campos bloqueados **"Capitán (Obligatorio)"** y **"Combustible (Obligatorio)"**. Debajo, 5 checkboxes desmarcados: *Chalecos salvavidas*, *Nevera con hielo*, *Equipo de sonido*, *Equipo de pesca* y *Equipo de buceo* (FR-054, FR-055). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Validaciones de Puerto, Tarifa y Fotografía (Escenarios US2-6 a US2-16)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Intentar guardar con campos obligatorios vacíos | Sin seleccionar mapa ni llenar campos, hacer clic en **"Guardar Registro"** | Campos vacíos | No guarda y muestra mensajes debajo de cada campo: **"Las coordenadas son obligatorias"** (bajo Lat/Lon) y **"Este campo es obligatorio para continuar con el registro"** (bajo Puerto, Tarifa y Fotografía) (FR-044, FR-046, FR-048, FR-053). | [ ] | [ ] |
| 2 | Validar nombre de Puerto inválido | Escribir un nombre inválido en "Puerto" y salir del campo | `"12"` o `"Puerto@1"` | Muestra: **"El nombre del puerto debe tener entre 3 y 50 caracteres, contener al menos una letra y solo puede incluir letras, números, espacios, tildes y guiones"** (FR-046). | [ ] | [ ] |
| 3 | Validar Tarifa base (COP) | Probar letra, decimal, cero y valor superior a 9.999.999.999 | `"180a"`, `"180.5"`, `"0"`, `"10000000000"` | Muestra respectivamente: **"Solo se aceptan valores numéricos"**, **"Ingrese un número entero sin puntos ni comas"**, **"La tarifa debe ser mayor que 0"** y **"La tarifa excede el valor permitido"** (FR-048). | [ ] | [ ] |
| 4 | Validar archivo con formato falso y archivo > 10 MB | Intentar adjuntar `documento_falso.jpg` y luego `imagen_pesada.png` | `documento_falso.jpg` / `imagen_pesada.png` | Rechaza ambos archivos; para el formato falso muestra **"Formato de imagen inválido"** (validación por bytes) y para el de >10 MB muestra **"El tamaño de imagen se excede"** (FR-050). | [ ] | [ ] |
| 5 | Adjuntar, reemplazar y quitar fotografía válida | Adjuntar `lancha_valida.jpg`, reemplazar por `lancha_reemplazo.png` y hacer clic en la **"X"** | `lancha_valida.jpg` -> `lancha_reemplazo.png` | Muestra vista previa con botón **"X"** arriba a la derecha; al adjuntar la segunda, la reemplaza directamente; al pulsar **"X"**, la elimina de inmediato sin pedir confirmación (FR-051, FR-052). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 3: Guardado final exitoso y etiqueta "NUEVA" (Escenarios US2-17 a US2-21)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Completar Paso 2 y guardar registro | Seleccionar punto en mapa, llenar Puerto, Tarifa válida, adjuntar imagen válida (sin marcar servicios adicionales) y pulsar **"Guardar Registro"** | Puerto: `"Marina Magdalena"`, Tarifa: `"0180000"`, Foto: `lancha_valida.jpg` | El botón se deshabilita durante el envío (FR-033). Al terminar, se muestra el modal **"¡Embarcación registrada!"** con el texto **"El nuevo vehículo fluvial ha sido registrado con éxito y se encuentra disponible en tu flota."** y botón **"Aceptar"** (FR-056 a FR-058). | [ ] | [ ] |
| 2 | Confirmar modal y verificar lista actualizada | Hacer clic en **"Aceptar"** en el modal de éxito | N/A | Cierra el modal y redirige a **"Mis Embarcaciones Registradas"**, mostrando la nueva embarcación en **primer lugar**, con estado **"Disponible"** y la etiqueta **"NUEVA"** junto a su nombre (FR-059, FR-060). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Reutilización de Puerto)** : Si se ingresa el nombre de un puerto ya existente (ej. `"marina magdalena"` sin tildes/mayúsculas), la embarcación se asocia a ese puerto conservando las coordenadas originales del puerto en BD (FR-045).
* **EC-002 (Transferencia de etiqueta NUEVA)** : Al registrar una segunda embarcación nueva, la etiqueta **"NUEVA"** debe trasladarse a la más reciente y desaparecer de la anterior (FR-060).
* **EC-003 (No imágenes huérfanas)** : Si el usuario envía una foto válida pero falla la validación de otro campo en el backend, la imagen **no** debe subirse a Cloudinary (Decisión D9 del Plan).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |