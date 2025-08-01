# Single-stage Dockerfile for Users Service using pre-built JAR
FROM amazoncorretto:17-alpine

# Create non-root user for security
RUN addgroup -S crm && adduser -S crm -G crm

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the pre-built JAR file
COPY services/users-service/target/*.jar app.jar

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