# Multi-stage build for Discovery Server
FROM maven:3.9.6-openjdk-17-slim as builder

WORKDIR /app

# Copy parent POM and shared dependencies first for better caching
COPY ../../pom.xml ../pom.xml
COPY ../../shared ../shared

# Copy service-specific files
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM openjdk:17-jre-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]