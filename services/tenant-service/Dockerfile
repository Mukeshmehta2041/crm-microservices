# Multi-stage Dockerfile for Tenant Service
# Stage 1: Build stage
FROM openjdk:17-jdk-slim as builder

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy parent POM and shared modules first for better caching
COPY pom.xml .
COPY shared/ shared/

# Copy service-specific files
COPY services/tenant-service/pom.xml services/tenant-service/
COPY services/tenant-service/src services/tenant-service/src

# Build the application
RUN mvn clean package -pl services/tenant-service -am -DskipTests

# Stage 2: Runtime stage
FROM openjdk:17-jre-slim

# Create non-root user for security
RUN groupadd -r crm && useradd -r -g crm crm

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/services/tenant-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R crm:crm /app
USER crm

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]