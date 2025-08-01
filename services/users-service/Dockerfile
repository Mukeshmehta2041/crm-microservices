# Multi-stage Dockerfile for Users Service
# Stage 1: Build stage
FROM amazoncorretto:17-alpine as builder

# Install Maven
RUN apk add --no-cache maven

WORKDIR /app

# Copy parent POM and shared modules first for better caching
COPY pom.xml .
COPY shared/ shared/

# Copy service-specific files
COPY services/users-service/pom.xml services/users-service/
COPY services/users-service/src services/users-service/src

# Build the application
RUN mvn clean package -pl services/users-service -am -DskipTests

# Stage 2: Runtime stage
FROM amazoncorretto:17-alpine

# Create non-root user for security
RUN addgroup -S crm && adduser -S crm -G crm

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/services/users-service/target/*.jar app.jar

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