# Use lightweight Java runtime base image
FROM eclipse-temurin:17-jre

# Create non-root user
RUN groupadd -g 1001 crm && useradd -m -s /bin/bash -u 1001 -g crm crm

# Install curl for health checks (optional)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy your pre-built jar into the image
COPY services/notification-service/target/*.jar app.jar

# Set correct ownership
RUN chown -R crm:crm /app
USER crm

# Expose the app port
EXPOSE 8093

# Health check (optional but recommended)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8093/actuator/health || exit 1

# JVM settings for better container performance
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Start the app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]