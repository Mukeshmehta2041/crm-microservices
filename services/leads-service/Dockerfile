# Multi-stage Dockerfile for Leads Service
FROM openjdk:17-jdk-slim as builder

WORKDIR /app

# Copy parent POM and shared dependencies
COPY pom.xml .
COPY shared/ shared/
COPY services/leads-service/pom.xml services/leads-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -pl services/leads-service

# Copy source code
COPY services/leads-service/src services/leads-service/src

# Build the application
RUN ./mvnw clean package -pl services/leads-service -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r crm && useradd -r -g crm crm

# Copy the built JAR
COPY --from=builder /app/services/leads-service/target/*.jar app.jar

# Change ownership
RUN chown crm:crm app.jar

# Switch to non-root user
USER crm

# Expose port
EXPOSE 8086

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8086/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]