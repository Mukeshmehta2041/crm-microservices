FROM openjdk:17-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r crm && useradd -r -g crm crm

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the pre-built JAR file from local target directory
COPY services/deals-service/target/deals-service-1.0.0-SNAPSHOT.jar app.jar

# Change ownership
RUN chown -R crm:crm /app

# Switch to non-root user
USER crm

# Expose port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8085/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]