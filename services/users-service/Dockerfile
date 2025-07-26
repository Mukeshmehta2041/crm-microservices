FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy parent pom and shared dependencies
COPY pom.xml /app/parent-pom.xml
COPY shared /app/shared

# Copy service pom.xml
COPY services/users-service/pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -f /app/parent-pom.xml
RUN mvn dependency:go-offline

# Copy source code and build
COPY services/users-service/src ./src
RUN mvn clean package -DskipTests

# Production image
FROM openjdk:17-jdk-slim

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy jar file
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown appuser:appuser app.jar

USER appuser

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]