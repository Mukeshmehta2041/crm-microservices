# Use lightweight Java runtime base image
FROM eclipse-temurin:17-jre

# Create non-root user for security
RUN groupadd -g 1001 crm && useradd -m -s /bin/bash -u 1001 -g crm crm

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the pre-built JAR file
COPY services/discovery-server/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R crm:crm /app
USER crm

# Expose port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8761/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]