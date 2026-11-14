# SeaShareM1 (backend)

Módulo 1 de SEA-SHARE: gestión de flota y embarcaciones. Java 17, Spring Boot 4.1.1, Maven.

## Requisitos

- JDK 17 o superior (el proyecto compila con `release 17`)
- Maven 3.9+
- MySQL 8 en local con una base de datos `seashare` (para ejecutar la aplicación)

## Variables de entorno

Copia los valores de `.env.example` a las variables de entorno de tu IDE o de tu shell. Spring no lee el archivo `.env` por sí solo. `JWT_SECRET` no tiene valor por defecto: hay que definirla.

## Comandos principales

| Qué | Comando |
|---|---|
| Instalar dependencias y compilar | `mvn clean compile` |
| Ejecutar la aplicación (puerto 8081) | `mvn spring-boot:run` |
| Pruebas unitarias y de arquitectura | `mvn test` |
| Todo: pruebas, integración, formato y cobertura | `mvn verify` |
| Aplicar el formato del código | `mvn spotless:apply` |
| Verificar el formato sin cambiar nada | `mvn spotless:check` |
| Informe de cobertura | abrir `target/site/jacoco/index.html` después de `mvn verify` |

## Calidad

- **Spotless** (google-java-format): `mvn spotless:check` se ejecuta dentro de `mvn verify`.
- **JaCoCo**: exige 80 % de cobertura solo para `domain` y `application`. Excluye `package-info` y las clases `*MapperImpl` generadas por MapStruct. Mientras esas capas no tengan lógica, la verificación no falla.
- **ArchUnit**: `DependencyRulesTest` verifica que `domain` y `application` no dependan de Spring, JPA ni de `infrastructure`.
- **Failsafe**: ejecuta las pruebas `*IT` en la fase `verify`. `BuildSmokeIT` es solo una prueba de humo y se reemplaza por pruebas con Testcontainers.

## Estructura

Paquete base `com.seashare.seasharem1`, con las áreas `domain`, `application`, `infrastructure` y `configuration`, según el plan general técnico. Las migraciones Flyway van en `src/main/resources/db/migration`.

## Estado

Esqueleto de T001, T003, T005, T006 y T016. Las dependencias de JWT y Cloudinary se agregan en T013 y T019.
