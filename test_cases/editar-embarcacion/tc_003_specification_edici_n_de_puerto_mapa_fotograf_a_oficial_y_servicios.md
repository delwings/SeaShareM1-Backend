### Test Case Specification: Edición de Puerto, Mapa, Fotografía Oficial y Servicios
**Created** : 2026-11-13  **Spec/Plan Link** : `docs/features/editar-embarcacion/spec.md` | `docs/features/editar-embarcacion/plan.md`  **Status** : Draft

#### Test Overview  *(mandatory)*
**Test Case ID** : TC-003-PuertoMapaFotoServicios
**Product Name & Version** : SeaShare – Módulo 1: Gestión de Flota v1.0
**Author** : Juan Pablo
**Current Owner** : QA TEAM
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
Este caso de prueba verifica la precarga y actualización del mapa interactivo (Leaflet), el redondeo y solo lectura de coordenadas WGS 84, la validación y comportamiento de puertos compartidos por nombre, la carga/rechazo/eliminación de la fotografía oficial mediante la "X" (tanto en foto precargada como nueva) y el comportamiento de los servicios base obligatorios frente a los servicios adicionales.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| Feature Specification: Editar Embarcación | Specification Document | `docs/features/editar-embarcacion/spec.md` |
| Implementation Plan: Editar Embarcación | Plan Document | `docs/features/editar-embarcacion/plan.md` |
| Contrato REST: Editar Embarcación | API Contract | `docs/architecture/contracts/rest/editar-embarcacion.md` |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La "X" de eliminación aparece tanto sobre la fotografía oficial precargada como sobre la recién seleccionada (FR-019, registro de cambios #6).
* Los servicios base obligatorios (`Capitán` y `Combustible`) permanecen siempre incluidos y bloqueados en el formulario (FR-021, Plan).

##### Dependencies & Assumptions
* Formulario de edición cargado con puerto, coordenadas, fotografía oficial y servicios precargados.
* Archivos de prueba disponibles: `lancha_valida.jpg` (PNG/JPG/JPEG válido ≤ 10 MB), `documento_falso.jpg` (archivo renombrado/corrupto/0 bytes/animado) y `imagen_pesada.png` (> 10 MB).

##### Preconditions & Default Setup
* **Initial State** : Propietario autenticado ubicado en `/owner/boats/:id/edit` con datos precargados.
* **Environment** : Staging / Desarrollo Local (Backend puerto `8081` / Frontend puerto `5500`).
* **Test Accounts** : `propietario.test@seashare.com` (rol `OWNER`).

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

##### Section 1: Mapa interactivo, puerto, fotografía válida y servicios (Escenarios US3-1 a US3-5 y US3-9)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Verificar precarga del mapa y coordenadas | Abrir el formulario de edición e inspeccionar el mapa y los campos de coordenadas | Coordenadas guardadas (ej. `11.2408`, `-74.1990`) | El mapa se centra en la posición guardada con zoom `15` y marcador visible; **"Latitud"** y **"Longitud"** son de solo lectura y muestran formato WGS 84 con signo y hasta 4 decimales sin ceros de relleno (FR-015, FR-016). | [ ] | [ ] |
| 2 | Mover marcador y editar nombre del puerto | Arrastrar o clicar el marcador en el mapa; luego editar el campo **"Puerto"** manualmente | Nuevo punto en mapa; Puerto: `"Marina Magdalena"` | **"Latitud"** y **"Longitud"** actualizan con signo y hasta 4 decimales; mover el marcador no sobrescribe el campo **"Puerto"**; el campo **"Puerto"** valida su formato y muestra sus propios mensajes de error (FR-016, FR-017). | [ ] | [ ] |
| 3 | Cargar fotografía oficial válida | Presionar **"Cambiar fotografía"** y seleccionar `lancha_valida.jpg` | `lancha_valida.jpg` (≤ 10 MB, cualquier dimensión) | La vista previa reemplaza la imagen anterior, muestra la **"X"** en la esquina superior derecha y habilita **"Guardar cambios"** si hay modificación (FR-018, FR-019). | [ ] | [ ] |
| 4 | Verificar servicios base y servicios adicionales | Revisar la sección de servicios y alternar checkboxes adicionales | Servicios base + adicionales (ej. Chalecos, Nevera) | **"Capitán"** y **"Combustible"** aparecen fijos y bloqueados; los servicios adicionales se pueden marcar o desmarcar libremente y habilitan **"Guardar cambios"** si difieren de lo guardado (FR-021, FR-023). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Section 2: Rechazo de foto, eliminación con "X", normalización y fallo de mapa (Escenarios US3-6 a US3-8 y US3-10)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Adjuntar archivo inválido, > 10 MB o cancelar selector | Intentar subir `documento_falso.jpg`, `imagen_pesada.png` (y combinación inválida + >10 MB) o cancelar el selector | `documento_falso.jpg`, `imagen_pesada.png`, cancelación | Si se cancela no cambia nada; si es inválido rechaza bajo el área de carga (priorizando **"Formato de imagen inválido"** sobre tamaño si ambos fallan) y conserva la foto previa (FR-019, FR-020). | [ ] | [ ] |
| 2 | Quitar fotografía oficial con la "X" | Hacer clic en la **"X"** sobre la vista previa (tanto en foto precargada como recién cargada) | Vista previa con foto activa | Se limpia la vista previa, aparece de inmediato **"Este campo es obligatorio"** y **"Guardar cambios"** se deshabilita hasta adjuntar una foto válida (FR-019). | [ ] | [ ] |
| 3 | Verificar normalización de longitud y fallo de carga del mapa | Simular longitud `190` en datos de entrada y simular fallo en carga de teselas/librería del mapa | `longitude = 190`; bloqueo de mapa | La longitud `190` se normaliza a `-170`; si falla el mapa, muestra **"No se pudo cargar el mapa interactivo en este momento."** con botón **"Reintentar"** sin bloquear el resto del formulario (FR-016, FR-024). | [ ] | [ ] |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001 (Puerto compartido por nombre)** : Al guardar con un nombre de puerto existente (comparación sin distinguir mayúsculas): si se modifican las coordenadas conservando el nombre del puerto, se actualizan las coordenadas de ese puerto para todas las embarcaciones que lo usan; si se escribe un nombre nuevo, se crea un puerto nuevo sin modificar el anterior (FR-017, Decisión D2, Edge Cases).
* **EC-002 (Micro-movimiento sin cambio de 4 decimales y conservación de foto)** : Si el marcador se mueve pero los valores redondeados a 4 decimales no cambian, no cuenta como edición; si no se modifica la fotografía oficial, se conserva la ya guardada sin reenviarla (FR-016, FR-018, Decisión D6).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |