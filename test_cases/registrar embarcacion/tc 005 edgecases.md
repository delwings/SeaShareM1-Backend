### Test Case Specification: TC-005-EdgeCasesYErrores

**Created** : 05 de noviembre de 2026  **Spec/Plan Link** : docs/features/registrar-embarcacion/spec.md  **Status** : Approved

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-005-EdgeCasesYErrores
**Product Name & Version** : SeaShare Módulo 1 v1.0
**Author** : Delwin
**Current Owner** : QA Team
**Priority** : P2 - High

#### Objectives & Context  *(mandatory)*

##### Test Objectives

Este caso de prueba verifica la resiliencia del sistema, el manejo de errores técnicos, los comportamientos ante caídas de red, la integridad de los datos en casos límite (espacios, ceros, decimales, límites numéricos) y la persistencia de estados ante la expiración de sesiones o recargas de página durante el registro de embarcaciones.

##### Document References

| Reference Name | Type of Reference | Document Location / Link | 
| ----- | ----- | ----- | 
| \[Feature Spec\] | \[Specification Document\] | docs/features/registrar-embarcacion/spec.md | 
| \[Implementation Plan\] | \[Plan Document\] | docs/features/registrar-embarcacion/plan.md | 

#### Prerequisites & Environment  *(mandatory)*

##### Test Case Limitations

* Requiere capacidad de simular interrupciones de red o fallos en el servidor/Cloudinary.

##### Dependencies & Assumptions

* TC-001 y TC-002 deben haber validado los flujos estándar previamente.

##### Preconditions & Default Setup

* **Initial State** : Propietario autenticado (rol `OWNER`) con o sin borrador activo.

* **Environment** : Staging / Sandbox (Backend en puerto 8081, Frontend en 5500).

* **Test Accounts** : propietario@seashare.com

#### Test Execution Flow  *(mandatory)*

##### Section 1: Errores Técnicos y Fallos de Red al Guardar

| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments | 
| ----- | ----- | ----- | ----- | ----- | ----- | ----- | 
| 1 | Simular fallo técnico en Paso 1 | Interrumpir red y hacer clic en "Siguiente" con datos válidos | Datos válidos de Paso 1 | Se muestra modal "No se pudo guardar el registro" con botones "Cerrar" y "Reintentar". Datos intactos | \[ \] | \[ \] | 
| 2 | Evaluar acción "Cerrar" en error | Hacer clic en "Cerrar" en el modal de error técnico | N/A | El modal se cierra y el usuario permanece en el formulario con sus datos intactos | \[ \] | \[ \] | 
| 3 | Evaluar acción "Reintentar" | Restablecer red y hacer clic en "Reintentar" | N/A | El sistema reintenta el guardado; al tener éxito, avanza al Paso 2 con normalidad | \[ \] | \[ \] | 
| 4 | Simular fallo técnico en Paso 2 | Interrumpir red y hacer clic en "Guardar Registro" | Datos válidos de Paso 2 | Muestra modal "No se pudo guardar el registro" sin perder datos previos de Paso 1 ni Paso 2 | \[ \] | \[ \] | 
| 5 | Evitar duplicados por doble clic | Hacer doble clic rápido en "Guardar Registro" | N/A | El botón se deshabilita con ícono de prohibido y no genera registros duplicados | \[ \] | \[ \] | 

##### Section 2: Límites, Formatos Especiales y Normalización de Datos

| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments | 
| ----- | ----- | ----- | ----- | ----- | ----- | ----- | 
| 1 | Espacios al inicio o final | Escribir nombres con espacios sobrantes (ej. "  Marina Magdalena  ") | Espacios extra | El sistema los elimina automáticamente antes de validar y guardar | \[ \] | \[ \] | 
| 2 | Uso de caracteres especiales en nombre | Escribir nombres con tildes, ñ, ü o numerales romanos | "Río Magdalena II", "Peñón" | El sistema los acepta correctamente al contener al menos una letra | \[ \] | \[ \] | 
| 3 | Redondeo de coordenadas del mapa | Seleccionar punto en mapa que entregue 6 decimales | Coordenadas con alta precisión | Los campos "Latitud" y "Longitud" se completan y guardan redondeados exactamente a 4 decimales | \[ \] | \[ \] | 
| 4 | Ceros a la izquierda en números | Ingresar capacidad o tarifa con ceros a la izquierda | "007", "0180000" | El sistema los acepta y guarda como el valor numérico limpio (7 y 180000) | \[ \] | \[ \] | 
| 5 | Tarifa excediendo el límite técnico | Escribir una tarifa superior a 9.999.999.999 COP | 10000000000 | Muestra el mensaje "La tarifa excede el valor permitido" y bloquea el guardado | \[ \] | \[ \] | 
| 6 | Reutilización de puerto existente | Escribir el nombre de un puerto ya registrado (sin importar mayúsculas/tildes) | "marina magdalena" | La embarcación se asocia al puerto existente reutilizando sus coordenadas originales | \[ \] | \[ \] | 

##### Section 3: Sesión, Recargas y Ciclo de Vida del Borrador

| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments | 
| ----- | ----- | ----- | ----- | ----- | ----- | ----- | 
| 1 | Recarga de página en Paso 1 | Recargar el navegador estando en el Paso 1 sin guardar | N/A | Se pierde la información escrita y los campos quedan vacíos sin crear borrador | \[ \] | \[ \] | 
| 2 | Recarga de página en Paso 2 | Recargar el navegador estando en el Paso 2 | N/A | El borrador se conserva con los datos del Paso 1 (se pierden los datos a medias del Paso 2) | \[ \] | \[ \] | 
| 3 | Expiración de sesión en acción de guardado | Expirar sesión justo al hacer clic en "Siguiente" o "Guardar" | N/A | El sistema redirige al login pero conserva de forma segura el borrador existente | \[ \] | \[ \] | 
| 4 | Fallo al eliminar borrador | Simular error de red al confirmar "Sí, eliminar y empezar" | N/A | Muestra modal "No se pudo eliminar el borrador" y el borrador se conserva intacto | \[ \] | \[ \] | 

#### Edge Cases & Boundary Conditions  *(mandatory)*

* **EC-001** : ¿Qué pasa si la matrícula tiene un formato inválido y además ya existe en la BD? El sistema evalúa primero el error de formato antes de validar la existencia.

* **EC-002** : ¿Qué pasa si el propietario abandona un borrador indefinidamente? No vence jamás y su matrícula legal permanece bloqueada para otros usuarios de forma intencional.