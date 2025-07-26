#!/bin/bash

echo "=== CRM Microservices Platform Setup Verification ==="
echo ""

echo "1. Checking Maven project structure..."
if [ -f "pom.xml" ]; then
    echo "✓ Parent POM exists"
else
    echo "✗ Parent POM missing"
    exit 1
fi

echo "2. Checking shared libraries..."
for lib in "common-utils" "security-common" "testing-common"; do
    if [ -f "shared/$lib/pom.xml" ]; then
        echo "✓ $lib module exists"
    else
        echo "✗ $lib module missing"
        exit 1
    fi
done

echo "3. Checking infrastructure services..."
if [ -f "services/discovery-server/pom.xml" ]; then
    echo "✓ Discovery server module exists"
else
    echo "✗ Discovery server module missing"
    exit 1
fi

echo "4. Checking Docker configuration..."
if [ -f "docker-compose.yml" ]; then
    echo "✓ Docker Compose configuration exists"
else
    echo "✗ Docker Compose configuration missing"
    exit 1
fi

if [ -f "docker-compose.dev.yml" ]; then
    echo "✓ Development Docker Compose override exists"
else
    echo "✗ Development Docker Compose override missing"
    exit 1
fi

echo "5. Checking CI/CD configuration..."
if [ -f ".github/workflows/ci-cd.yml" ]; then
    echo "✓ CI/CD pipeline configuration exists"
else
    echo "✗ CI/CD pipeline configuration missing"
    exit 1
fi

echo "6. Testing Maven build..."
if mvn validate -q; then
    echo "✓ Maven project validates successfully"
else
    echo "✗ Maven project validation failed"
    exit 1
fi

if mvn compile -q; then
    echo "✓ Maven project compiles successfully"
else
    echo "✗ Maven project compilation failed"
    exit 1
fi

if mvn test -q; then
    echo "✓ Maven tests pass successfully"
else
    echo "✗ Maven tests failed"
    exit 1
fi

echo "7. Checking Docker Compose configuration..."
if docker-compose config --quiet; then
    echo "✓ Docker Compose configuration is valid"
else
    echo "✗ Docker Compose configuration is invalid"
    exit 1
fi

echo ""
echo "=== Setup Verification Complete ==="
echo "✓ All components are properly configured and ready for development"
echo ""
echo "Next steps:"
echo "1. Start infrastructure services: make start-infra"
echo "2. Build and start application services: make start"
echo "3. Access services:"
echo "   - Discovery Server: http://localhost:8761"
echo "   - API Gateway: http://localhost:8080"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana: http://localhost:3000"
echo ""