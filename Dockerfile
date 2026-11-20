# ==========================================
# STAGE 1: BUILD
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .

# Descargar dependencias (aprovecha la caché de Docker)
RUN mvn dependency:go-offline

# Copiar código fuente
COPY src ./src

# Compilar y generar el JAR
RUN mvn clean package -DskipTests


# ==========================================
# STAGE 2: RUNTIME
# ==========================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar únicamente el JAR generado
COPY --from=build /app/target/*.jar app.jar


# Ejecutar Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]