# syntax=docker/dockerfile:1.7

############################################
# Build Stage
############################################
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy Maven files first for better layer caching
COPY pom.xml .
# COPY mvnw .
# COPY .mvn .mvn

# Download dependencies using BuildKit cache
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B --no-transfer-progress dependency:go-offline

# Copy application source
COPY src ./src

# Build application
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B --no-transfer-progress clean package -DskipTests

############################################
# Runtime Stage
############################################
FROM eclipse-temurin:21-jre-jammy

# OCI Image Metadata
LABEL org.opencontainers.image.title="order-service" \
    org.opencontainers.image.description="Spring Boot Application - Order Service" \
    org.opencontainers.image.vendor="NULL"

WORKDIR /app

# Create non-root user with fixed UID/GID
RUN groupadd --gid 1001 spring && \
    useradd --uid 1001 --gid spring \
    --shell /usr/sbin/nologin \
    --create-home spring

# Copy executable JAR
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring:spring

EXPOSE 8084

# Uncomment if Spring Boot Actuator is enabled
# HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
#     CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT [ "java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar" ]