# Multi-stage Dockerfile for Contacts Service
FROM openjdk:17-jdk-slim as builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY services/contacts-service/pom.xml ./services/contacts-service/

# Copy shared dependencies
COPY shared/ ./shared/

# Download dependencies
RUN ./mvnw dependency:go-offline -pl services/contacts-service

# Copy source code
COPY services/contacts-service/src ./services/contacts-service/src

# Build the application
RUN ./mvnw clean package -pl services/contacts-service -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r contacts && useradd -r -g contacts contacts

# Copy the built jar
COPY --from=builder /app/services/contacts-service/target/*.jar app.jar

# Change ownership
RUN chown contacts:contacts app.jar

# Switch to non-root user
USER contacts

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]