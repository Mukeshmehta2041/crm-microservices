# Multi-stage Dockerfile for Accounts Service
FROM openjdk:17-jdk-slim as builder
WORKDIR /app

# Copy parent POM and shared dependencies
COPY ../../pom.xml ./pom.xml
COPY ../../shared ./shared

# Copy service-specific files
COPY pom.xml ./services/accounts-service/pom.xml
COPY src ./services/accounts-service/src

# Build the application
WORKDIR /app/services/accounts-service
RUN ../../../mvnw clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built JAR
COPY --from=builder /app/services/accounts-service/target/*.jar app.jar

# Expose port
EXPOSE 8087

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8087/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]