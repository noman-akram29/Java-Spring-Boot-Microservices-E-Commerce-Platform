# syntax=docker/dockerfile:1.7

############################################
# Build Stage
############################################
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B --no-transfer-progress dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B --no-transfer-progress clean package -DskipTests

# Download OpenTelemetry Java agent
ARG OTEL_AGENT_VERSION=2.28.1

RUN mkdir -p /workspace/otel \
    && curl -fL \
    -o /workspace/otel/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar

############################################
# Runtime Stage
############################################
FROM eclipse-temurin:21-jre-jammy

LABEL org.opencontainers.image.title="product-service" \
    org.opencontainers.image.description="Spring Boot Application - Product Service" \
    org.opencontainers.image.vendor="NULL"

WORKDIR /app

RUN groupadd --gid 1001 spring && \
    useradd --uid 1001 --gid spring \
    --shell /usr/sbin/nologin \
    --create-home spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

# OpenTelemetry Java agent
COPY --from=build --chown=spring:spring /workspace/otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

COPY --chmod=755 entrypoint.sh /app/entrypoint.sh

USER spring:spring

EXPOSE 8082

ENTRYPOINT ["/app/entrypoint.sh"]