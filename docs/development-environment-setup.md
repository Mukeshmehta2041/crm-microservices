# Development Environment Setup Guide

## Overview

This guide provides step-by-step instructions for setting up a complete development environment for the CRM microservices platform. Follow these instructions to ensure a consistent development experience across all team members.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Development Tools](#development-tools)
3. [Project Setup](#project-setup)
4. [Database Setup](#database-setup)
5. [IDE Configuration](#ide-configuration)
6. [Docker Environment](#docker-environment)
7. [Code Quality Tools](#code-quality-tools)
8. [Testing Setup](#testing-setup)
9. [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements
- **Operating System**: macOS, Linux, or Windows 10/11
- **RAM**: Minimum 16GB (32GB recommended)
- **Storage**: At least 50GB free space
- **Network**: Stable internet connection for downloading dependencies

### Required Software Versions
- **Java**: OpenJDK 17 or later
- **Maven**: 3.6.3 or later
- **Docker**: 20.10 or later
- **Docker Compose**: 2.0 or later
- **Git**: 2.30 or later

## Development Tools

### 1. Java Development Kit (JDK)

#### Install OpenJDK 17
```bash
# macOS (using Homebrew)
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc

# Linux (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java -version
javac -version
```

#### Set JAVA_HOME
```bash
# macOS
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Add to your shell profile (.zshrc, .bashrc, etc.)
echo 'export JAVA_HOME=/path/to/java' >> ~/.zshrc
```

### 2. Apache Maven

#### Install Maven
```bash
# macOS (using Homebrew)
brew install maven

# Linux (Ubuntu/Debian)
sudo apt install maven

# Verify installation
mvn -version
```

#### Configure Maven Settings
Create or update `~/.m2/settings.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <localRepository>${user.home}/.m2/repository</localRepository>
    
    <profiles>
        <profile>
            <id>crm-platform</id>
            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>
        </profile>
    </profiles>
    
    <activeProfiles>
        <activeProfile>crm-platform</activeProfile>
    </activeProfiles>
</settings>
```

### 3. Docker and Docker Compose

#### Install Docker
```bash
# macOS
brew install --cask docker

# Linux (Ubuntu/Debian)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Verify installation
docker --version
docker-compose --version
```

### 4. Git Configuration

#### Configure Git
```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@company.com"
git config --global init.defaultBranch main
git config --global pull.rebase false
```

#### Set up SSH Keys (recommended)
```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "your.email@company.com"

# Add to SSH agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# Copy public key to clipboard
cat ~/.ssh/id_ed25519.pub
# Add this key to your GitHub/GitLab account
```

## Project Setup

### 1. Clone the Repository
```bash
git clone git@github.com:company/crm-microservices-platform.git
cd crm-microservices-platform
```

### 2. Build the Project
```bash
# Clean and compile all modules
mvn clean compile

# Run tests
mvn test

# Package all services
mvn package -DskipTests
```

### 3. Verify Setup
```bash
# Check if all services can be built
mvn clean install -DskipTests

# Verify Docker images can be built
docker-compose build
```

## Database Setup

### 1. PostgreSQL with Docker
```bash
# Start PostgreSQL container
docker run --name crm-postgres \
  -e POSTGRES_DB=crm_platform \
  -e POSTGRES_USER=crm_user \
  -e POSTGRES_PASSWORD=crm_password \
  -p 5432:5432 \
  -d postgres:13

# Verify connection
docker exec -it crm-postgres psql -U crm_user -d crm_platform
```

### 2. Redis with Docker
```bash
# Start Redis container
docker run --name crm-redis \
  -p 6379:6379 \
  -d redis:6-alpine

# Verify connection
docker exec -it crm-redis redis-cli ping
```

### 3. Database Migration
```bash
# Run Flyway migrations for all services
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/auth_db
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/tenant_db
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/users_db
```

## IDE Configuration

### IntelliJ IDEA (Recommended)

#### 1. Install IntelliJ IDEA
- Download IntelliJ IDEA Ultimate or Community Edition
- Install required plugins:
  - Spring Boot
  - Docker
  - SonarLint
  - CheckStyle-IDEA
  - SpotBugs

#### 2. Import Project
1. Open IntelliJ IDEA
2. Select "Open or Import"
3. Navigate to the project directory
4. Select the root `pom.xml` file
5. Choose "Open as Project"

#### 3. Configure Code Style
1. Go to `Preferences > Editor > Code Style > Java`
2. Import the code style scheme:
   - Click the gear icon next to "Scheme"
   - Select "Import Scheme > IntelliJ IDEA code style XML"
   - Import `ide-config/intellij-code-style.xml`

#### 4. Configure Inspections
1. Go to `Preferences > Editor > Inspections`
2. Import inspection profile:
   - Click the gear icon
   - Select "Import Profile"
   - Import `ide-config/intellij-inspections.xml`

#### 5. Configure Run Configurations
Create run configurations for each service:
1. Go to `Run > Edit Configurations`
2. Add new "Spring Boot" configuration
3. Set main class (e.g., `com.crm.platform.auth.AuthServiceApplication`)
4. Set active profiles: `dev`
5. Set environment variables as needed

### Visual Studio Code

#### 1. Install Extensions
```bash
# Install VS Code extensions
code --install-extension vscjava.vscode-java-pack
code --install-extension pivotal.vscode-spring-boot
code --install-extension ms-vscode.vscode-docker
code --install-extension sonarsource.sonarlint-vscode
```

#### 2. Configure Settings
Create `.vscode/settings.json`:
```json
{
    "java.home": "/path/to/java-17",
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-17",
            "path": "/path/to/java-17"
        }
    ],
    "java.format.settings.url": "ide-config/eclipse-formatter.xml",
    "java.checkstyle.configuration": "checkstyle.xml",
    "sonarlint.connectedMode.project": {
        "connectionId": "crm-platform",
        "projectKey": "crm-microservices-platform"
    }
}
```

## Docker Environment

### 1. Development Environment
```bash
# Start all services in development mode
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### 2. Service-Specific Development
```bash
# Start only infrastructure services
docker-compose up -d postgres redis eureka-server

# Run specific service locally
cd services/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Database Management
```bash
# Access PostgreSQL
docker exec -it crm-postgres psql -U crm_user -d auth_db

# Access Redis
docker exec -it crm-redis redis-cli

# View database logs
docker logs crm-postgres
```

## Code Quality Tools

### 1. SonarQube Setup
```bash
# Start SonarQube with Docker
docker run -d --name sonarqube \
  -p 9000:9000 \
  sonarqube:community

# Access SonarQube at http://localhost:9000
# Default credentials: admin/admin
```

### 2. Run Code Quality Checks
```bash
# Run all quality checks
mvn clean verify

# Run specific checks
mvn checkstyle:check
mvn pmd:check
mvn spotbugs:check

# Run SonarQube analysis
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token
```

### 3. Format Code
```bash
# Format all Java files
mvn spotless:apply

# Check formatting
mvn spotless:check
```

## Testing Setup

### 1. Unit Tests
```bash
# Run unit tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### 2. Integration Tests
```bash
# Run integration tests
mvn verify

# Run specific integration test
mvn test -Dtest=UserServiceIntegrationTest
```

### 3. TestContainers Setup
Ensure Docker is running for integration tests:
```bash
# Verify Docker is accessible
docker ps

# Run integration tests with TestContainers
mvn verify -Dspring.profiles.active=test
```

## Environment Variables

### 1. Development Environment
Create `.env` file in the project root:
```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crm_platform
DB_USER=crm_user
DB_PASSWORD=crm_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Configuration
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400

# Logging Configuration
LOG_LEVEL=DEBUG
```

### 2. IDE Environment Variables
Configure environment variables in your IDE run configurations:
- `SPRING_PROFILES_ACTIVE=dev`
- `DB_HOST=localhost`
- `REDIS_HOST=localhost`

## Troubleshooting

### Common Issues

#### 1. Java Version Issues
```bash
# Check Java version
java -version

# Check JAVA_HOME
echo $JAVA_HOME

# Update alternatives (Linux)
sudo update-alternatives --config java
```

#### 2. Maven Issues
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Reload dependencies
mvn dependency:purge-local-repository
mvn clean install
```

#### 3. Docker Issues
```bash
# Restart Docker daemon
sudo systemctl restart docker

# Clean Docker system
docker system prune -a

# Check Docker logs
docker logs container-name
```

#### 4. Port Conflicts
```bash
# Check what's using a port
lsof -i :8080

# Kill process using port
kill -9 $(lsof -t -i:8080)
```

#### 5. Database Connection Issues
```bash
# Test PostgreSQL connection
psql -h localhost -p 5432 -U crm_user -d crm_platform

# Check if PostgreSQL is running
docker ps | grep postgres

# View PostgreSQL logs
docker logs crm-postgres
```

### Getting Help

1. **Documentation**: Check the `docs/` directory for detailed guides
2. **Issues**: Create an issue in the project repository
3. **Team Chat**: Ask questions in the development team channel
4. **Code Review**: Request help during code reviews

## Next Steps

After completing the setup:

1. **Read the Documentation**
   - [Java Coding Standards](java-coding-standards.md)
   - [Code Review Guidelines](code-review-guidelines.md)
   - [API Documentation](api-documentation.md)

2. **Run Your First Service**
   - Start the Discovery Server
   - Start the Auth Service
   - Test the health endpoints

3. **Make Your First Contribution**
   - Pick up a beginner-friendly issue
   - Follow the development workflow
   - Submit a pull request

4. **Join the Team**
   - Attend daily standups
   - Participate in code reviews
   - Contribute to documentation

## Conclusion

You should now have a fully functional development environment for the CRM platform. If you encounter any issues during setup, refer to the troubleshooting section or reach out to the development team for assistance.

Welcome to the team! 🚀