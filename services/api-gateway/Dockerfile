FROM openjdk:17-jre-slim

# Create non-root user
RUN groupadd -r apigateway && useradd -r -g apigateway apigateway

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the pre-built JAR file from local target directory
COPY services/api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown -R apigateway:apigateway /app

# Switch to non-root user
USER apigateway

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]