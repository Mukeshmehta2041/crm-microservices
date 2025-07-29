#!/bin/bash

# Documentation Generation Script for CRM Platform
# This script generates comprehensive documentation for the project

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Configuration
DOCS_DIR="target/docs"
JAVADOC_DIR="$DOCS_DIR/javadoc"
COVERAGE_DIR="$DOCS_DIR/coverage"
QUALITY_DIR="$DOCS_DIR/quality"

# Create documentation directories
create_docs_structure() {
    print_header "Creating Documentation Structure"
    
    mkdir -p "$DOCS_DIR"
    mkdir -p "$JAVADOC_DIR"
    mkdir -p "$COVERAGE_DIR"
    mkdir -p "$QUALITY_DIR"
    
    print_success "Documentation directories created"
}

# Generate JavaDoc documentation
generate_javadoc() {
    print_header "Generating JavaDoc Documentation"
    
    print_info "Generating JavaDoc for all modules..."
    ./mvnw javadoc:aggregate -q
    
    # Copy aggregated JavaDoc to docs directory
    if [ -d "target/site/apidocs" ]; then
        cp -r target/site/apidocs/* "$JAVADOC_DIR/"
        print_success "JavaDoc documentation generated"
    else
        print_warning "JavaDoc generation may have failed"
    fi
}

# Generate test coverage reports
generate_coverage_reports() {
    print_header "Generating Test Coverage Reports"
    
    print_info "Running tests and generating coverage..."
    ./mvnw clean test jacoco:report -q
    
    # Copy coverage reports
    find . -path "*/target/site/jacoco" -type d | while read -r dir; do
        module_name=$(echo "$dir" | sed 's|./||' | sed 's|/target/site/jacoco||' | tr '/' '-')
        if [ -d "$dir" ]; then
            mkdir -p "$COVERAGE_DIR/$module_name"
            cp -r "$dir"/* "$COVERAGE_DIR/$module_name/"
        fi
    done
    
    print_success "Coverage reports generated"
}

# Generate quality reports
generate_quality_reports() {
    print_header "Generating Quality Reports"
    
    # Run quality checks
    print_info "Running Checkstyle..."
    ./mvnw checkstyle:checkstyle -q || true
    
    print_info "Running PMD..."
    ./mvnw pmd:pmd -q || true
    
    print_info "Running SpotBugs..."
    ./mvnw spotbugs:spotbugs -q || true
    
    # Copy quality reports
    find . -name "checkstyle-result.xml" | while read -r file; do
        module_name=$(dirname "$file" | sed 's|./||' | sed 's|/target.*||' | tr '/' '-')
        cp "$file" "$QUALITY_DIR/checkstyle-$module_name.xml" 2>/dev/null || true
    done
    
    find . -name "pmd.xml" | while read -r file; do
        module_name=$(dirname "$file" | sed 's|./||' | sed 's|/target.*||' | tr '/' '-')
        cp "$file" "$QUALITY_DIR/pmd-$module_name.xml" 2>/dev/null || true
    done
    
    find . -name "spotbugsXml.xml" | while read -r file; do
        module_name=$(dirname "$file" | sed 's|./||' | sed 's|/target.*||' | tr '/' '-')
        cp "$file" "$QUALITY_DIR/spotbugs-$module_name.xml" 2>/dev/null || true
    done
    
    print_success "Quality reports generated"
}

# Generate project metrics
generate_project_metrics() {
    print_header "Generating Project Metrics"
    
    local metrics_file="$DOCS_DIR/project-metrics.md"
    
    cat > "$metrics_file" << 'EOF'
# CRM Platform Project Metrics

## Code Statistics

EOF
    
    # Count lines of code
    if command -v cloc &> /dev/null; then
        echo "### Lines of Code" >> "$metrics_file"
        echo '```' >> "$metrics_file"
        cloc --exclude-dir=target,node_modules,.git --exclude-ext=xml,json,yml,yaml . >> "$metrics_file"
        echo '```' >> "$metrics_file"
        echo "" >> "$metrics_file"
    else
        print_warning "cloc not found. Install it for detailed code statistics"
    fi
    
    # Count files
    echo "### File Counts" >> "$metrics_file"
    echo "- Java files: $(find . -name '*.java' -not -path '*/target/*' | wc -l)" >> "$metrics_file"
    echo "- Test files: $(find . -name '*Test.java' -o -name '*IT.java' | wc -l)" >> "$metrics_file"
    echo "- Configuration files: $(find . -name '*.yml' -o -name '*.yaml' -o -name '*.properties' -not -path '*/target/*' | wc -l)" >> "$metrics_file"
    echo "- Docker files: $(find . -name 'Dockerfile*' -o -name 'docker-compose*.yml' | wc -l)" >> "$metrics_file"
    echo "" >> "$metrics_file"
    
    # Module information
    echo "### Modules" >> "$metrics_file"
    find . -name 'pom.xml' -not -path '*/target/*' | while read -r pom; do
        module_dir=$(dirname "$pom")
        module_name=$(basename "$module_dir")
        if [ "$module_name" != "." ]; then
            echo "- $module_name" >> "$metrics_file"
        fi
    done
    echo "" >> "$metrics_file"
    
    # Dependencies
    echo "### Dependencies" >> "$metrics_file"
    echo "Generated on: $(date)" >> "$metrics_file"
    
    print_success "Project metrics generated"
}

# Generate API documentation index
generate_api_docs_index() {
    print_header "Generating API Documentation Index"
    
    local api_index="$DOCS_DIR/api-index.html"
    
    cat > "$api_index" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CRM Platform API Documentation</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 2rem;
            border-radius: 10px;
            margin-bottom: 2rem;
        }
        .card {
            background: white;
            border: 1px solid #e1e5e9;
            border-radius: 8px;
            padding: 1.5rem;
            margin-bottom: 1rem;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .card h3 {
            margin-top: 0;
            color: #2c3e50;
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 1rem;
        }
        a {
            color: #3498db;
            text-decoration: none;
        }
        a:hover {
            text-decoration: underline;
        }
        .badge {
            background: #e74c3c;
            color: white;
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            font-size: 0.8rem;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>CRM Platform Documentation</h1>
        <p>Comprehensive documentation for the CRM microservices platform</p>
    </div>

    <div class="grid">
        <div class="card">
            <h3>📚 JavaDoc API Reference</h3>
            <p>Complete API documentation for all Java classes and methods.</p>
            <a href="javadoc/index.html">View JavaDoc →</a>
        </div>

        <div class="card">
            <h3>📊 Test Coverage Reports</h3>
            <p>Code coverage analysis and test execution reports.</p>
            <a href="coverage/">View Coverage Reports →</a>
        </div>

        <div class="card">
            <h3>🔍 Code Quality Reports</h3>
            <p>Static analysis results from Checkstyle, PMD, and SpotBugs.</p>
            <a href="quality/">View Quality Reports →</a>
        </div>

        <div class="card">
            <h3>📈 Project Metrics</h3>
            <p>Code statistics, module information, and project overview.</p>
            <a href="project-metrics.md">View Metrics →</a>
        </div>

        <div class="card">
            <h3>🏗️ Architecture Documentation</h3>
            <p>System architecture, design patterns, and technical decisions.</p>
            <a href="../docs/architecture-documentation.md">View Architecture →</a>
        </div>

        <div class="card">
            <h3>🔧 Development Guide</h3>
            <p>Setup instructions, coding standards, and development workflow.</p>
            <a href="../docs/development-environment-setup.md">View Dev Guide →</a>
        </div>

        <div class="card">
            <h3>🚀 Deployment Guide</h3>
            <p>Docker setup, environment configuration, and deployment procedures.</p>
            <a href="../docs/deployment-guide.md">View Deployment →</a>
        </div>

        <div class="card">
            <h3>🔒 Security Documentation</h3>
            <p>Security implementation, authentication, and best practices.</p>
            <a href="../docs/security-documentation.md">View Security →</a>
        </div>
    </div>

    <footer style="margin-top: 2rem; text-align: center; color: #666;">
        <p>Generated on: <script>document.write(new Date().toLocaleString())</script></p>
        <p>CRM Platform v1.0.0-SNAPSHOT</p>
    </footer>
</body>
</html>
EOF
    
    print_success "API documentation index generated"
}

# Generate README for docs directory
generate_docs_readme() {
    local readme_file="$DOCS_DIR/README.md"
    
    cat > "$readme_file" << 'EOF'
# CRM Platform Documentation

This directory contains generated documentation for the CRM Platform project.

## Contents

- **javadoc/**: JavaDoc API documentation for all modules
- **coverage/**: Test coverage reports from JaCoCo
- **quality/**: Code quality reports from static analysis tools
- **project-metrics.md**: Project statistics and metrics
- **api-index.html**: Interactive documentation index

## Viewing Documentation

### Local Viewing
Open `api-index.html` in your web browser to access all documentation.

### Command Line
```bash
# Generate fresh documentation
./scripts/generate-docs.sh

# Serve documentation locally (if Python is available)
cd target/docs
python -m http.server 8080
# Then open http://localhost:8080
```

## Automated Generation

Documentation is automatically generated:
- During CI/CD builds
- When running `mvn site`
- Using the `generate-docs.sh` script

## Coverage Thresholds

- Minimum line coverage: 80%
- Minimum branch coverage: 70%
- Minimum method coverage: 85%

## Quality Gates

- Checkstyle: No violations
- PMD: No high priority issues
- SpotBugs: No bugs or security issues

## Updating Documentation

1. Update JavaDoc comments in source code
2. Update markdown files in `docs/` directory
3. Run `./scripts/generate-docs.sh` to regenerate
4. Commit changes to version control

## Tools Used

- **JavaDoc**: API documentation generation
- **JaCoCo**: Code coverage analysis
- **Checkstyle**: Code style checking
- **PMD**: Static code analysis
- **SpotBugs**: Bug pattern detection
- **cloc**: Code line counting (optional)
EOF
    
    print_success "Documentation README generated"
}

# Main execution
main() {
    print_header "CRM Platform Documentation Generation"
    
    create_docs_structure
    generate_javadoc
    generate_coverage_reports
    generate_quality_reports
    generate_project_metrics
    generate_api_docs_index
    generate_docs_readme
    
    print_header "Documentation Generation Complete"
    print_success "Documentation generated in: $DOCS_DIR"
    print_info "Open $DOCS_DIR/api-index.html in your browser to view"
    
    # Optional: Open documentation in browser
    if command -v open &> /dev/null; then
        read -p "Open documentation in browser? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            open "$DOCS_DIR/api-index.html"
        fi
    fi
}

# Show help
show_help() {
    echo "CRM Platform Documentation Generator"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  --javadoc-only      Generate only JavaDoc documentation"
    echo "  --coverage-only     Generate only coverage reports"
    echo "  --quality-only      Generate only quality reports"
    echo "  --metrics-only      Generate only project metrics"
    echo ""
    echo "Examples:"
    echo "  $0                  Generate all documentation"
    echo "  $0 --javadoc-only   Generate only JavaDoc"
}

# Parse command line arguments
case "${1:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    --javadoc-only)
        create_docs_structure
        generate_javadoc
        print_success "JavaDoc generation complete"
        ;;
    --coverage-only)
        create_docs_structure
        generate_coverage_reports
        print_success "Coverage report generation complete"
        ;;
    --quality-only)
        create_docs_structure
        generate_quality_reports
        print_success "Quality report generation complete"
        ;;
    --metrics-only)
        create_docs_structure
        generate_project_metrics
        print_success "Metrics generation complete"
        ;;
    "")
        main
        ;;
    *)
        print_error "Unknown option: $1"
        show_help
        exit 1
        ;;
esac