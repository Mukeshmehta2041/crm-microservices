# Multi-stage Dockerfile for Auth Service
# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-alpine as builder

# Install Maven
RUN apk add --no-cache maven

WORKDIR /app

# Copy parent POM and shared modules first for better caching
COPY pom.xml .
COPY shared/ shared/

# Copy service-specific files
COPY services/auth-service/pom.xml services/auth-service/
COPY services/auth-service/src services/auth-service/src

# Build the application
RUN mvn clean package -pl services/auth-service -am -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -g 1001 crm && adduser -D -s /bin/sh -u 1001 -G crm crm

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/services/auth-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R crm:crm /app
USER crm

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]