# Multi-stage Dockerfile for Custom Objects Service
FROM openjdk:17-jdk-slim as builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY shared/ shared/
COPY services/custom-objects-service/pom.xml services/custom-objects-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -pl services/custom-objects-service

# Copy source code
COPY services/custom-objects-service/src/ services/custom-objects-service/src/

# Build the application
RUN ./mvnw clean package -pl services/custom-objects-service -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r customobjects && useradd -r -g customobjects customobjects

# Copy the built JAR
COPY --from=builder /app/services/custom-objects-service/target/*.jar app.jar

# Change ownership
RUN chown customobjects:customobjects app.jar

# Switch to non-root user
USER customobjects

# Expose port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]