# ============================================================================
# Etapa 1: Compilación con Eclipse Temurin JDK 21 y Maven Wrapper
# ============================================================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copiar configuración Maven y wrapper
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Descargar dependencias aprovechando la caché de capas de Docker
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente y empaquetar artefacto JAR para producción
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ============================================================================
# Etapa 2: Runtime ligero con Eclipse Temurin JRE 21
# ============================================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Crear usuario sin privilegios root por seguridad
RUN useradd -m -u 1001 appuser
USER appuser

# Copiar artefacto ejecutable generado en la etapa anterior
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

# Variables de entorno por defecto (Render sobreescribe PORT en tiempo de ejecución)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# Reservar memoria para metaspace, buffers y threads; el heap no es toda la JVM.
# Estos límites requieren verificación real de carga, no garantizan ausencia de OOM.
# exec entrega SIGTERM a Java para completar solicitudes al actualizar Render.
ENTRYPOINT ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=50.0 -XX:InitialRAMPercentage=20.0 -Xss512k -XX:+ExitOnOutOfMemoryError -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
