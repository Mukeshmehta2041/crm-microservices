#!/bin/bash

# Docker Security Scanning Script for CRM Microservices Platform
# This script performs security scanning on all Docker images

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCAN_RESULTS_DIR="./security-scan-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="${SCAN_RESULTS_DIR}/security_scan_report_${TIMESTAMP}.json"

# Services to scan
SERVICES=(
    "discovery-server"
    "auth-service"
    "tenant-service"
    "users-service"
    "api-gateway"
    "contacts-service"
    "deals-service"
    "leads-service"
    "accounts-service"
    "activities-service"
    "pipelines-service"
    "custom-objects-service"
    "analytics-service"
    "workflow-service"
)

# Create results directory
mkdir -p "${SCAN_RESULTS_DIR}"

echo -e "${BLUE}=== CRM Platform Docker Security Scan ===${NC}"
echo -e "${BLUE}Timestamp: $(date)${NC}"
echo -e "${BLUE}Results will be saved to: ${SCAN_RESULTS_DIR}${NC}"
echo ""

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running${NC}"
        exit 1
    fi
}

# Function to check if Trivy is installed
check_trivy() {
    if ! command -v trivy &> /dev/null; then
        echo -e "${YELLOW}Trivy not found. Installing...${NC}"
        # Install Trivy based on OS
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew install aquasecurity/trivy/trivy
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            sudo apt-get update
            sudo apt-get install wget apt-transport-https gnupg lsb-release
            wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
            echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
            sudo apt-get update
            sudo apt-get install trivy
        else
            echo -e "${RED}Unsupported OS. Please install Trivy manually.${NC}"
            exit 1
        fi
    fi
}

# Function to build image if it doesn't exist
build_image_if_needed() {
    local service=$1
    local image_name="crm-${service}:latest"
    
    if ! docker image inspect "${image_name}" > /dev/null 2>&1; then
        echo -e "${YELLOW}Building ${service} image...${NC}"
        docker build -t "${image_name}" -f "services/${service}/Dockerfile" .
    fi
}

# Function to scan a single service
scan_service() {
    local service=$1
    local image_name="crm-${service}:latest"
    local service_report="${SCAN_RESULTS_DIR}/${service}_scan_${TIMESTAMP}.json"
    
    echo -e "${BLUE}Scanning ${service}...${NC}"
    
    # Build image if needed
    build_image_if_needed "${service}"
    
    # Run Trivy scan
    trivy image \
        --format json \
        --output "${service_report}" \
        --severity HIGH,CRITICAL \
        --ignore-unfixed \
        "${image_name}"
    
    # Extract summary
    local high_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "HIGH")] | length' "${service_report}" 2>/dev/null || echo "0")
    local critical_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "${service_report}" 2>/dev/null || echo "0")
    
    if [[ $critical_count -gt 0 ]]; then
        echo -e "${RED}  ❌ ${service}: ${critical_count} CRITICAL, ${high_count} HIGH vulnerabilities${NC}"
        return 1
    elif [[ $high_count -gt 0 ]]; then
        echo -e "${YELLOW}  ⚠️  ${service}: ${high_count} HIGH vulnerabilities${NC}"
        return 2
    else
        echo -e "${GREEN}  ✅ ${service}: No HIGH or CRITICAL vulnerabilities${NC}"
        return 0
    fi
}

# Function to generate summary report
generate_summary() {
    local summary_file="${SCAN_RESULTS_DIR}/summary_${TIMESTAMP}.json"
    local html_report="${SCAN_RESULTS_DIR}/security_report_${TIMESTAMP}.html"
    
    echo -e "${BLUE}Generating summary report...${NC}"
    
    # Create summary JSON
    echo "{" > "${summary_file}"
    echo "  \"scan_timestamp\": \"$(date -Iseconds)\"," >> "${summary_file}"
    echo "  \"services\": [" >> "${summary_file}"
    
    local first=true
    for service in "${SERVICES[@]}"; do
        local service_report="${SCAN_RESULTS_DIR}/${service}_scan_${TIMESTAMP}.json"
        if [[ -f "${service_report}" ]]; then
            if [[ $first == true ]]; then
                first=false
            else
                echo "    ," >> "${summary_file}"
            fi
            
            local high_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "HIGH")] | length' "${service_report}" 2>/dev/null || echo "0")
            local critical_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "${service_report}" 2>/dev/null || echo "0")
            
            echo "    {" >> "${summary_file}"
            echo "      \"service\": \"${service}\"," >> "${summary_file}"
            echo "      \"critical_vulnerabilities\": ${critical_count}," >> "${summary_file}"
            echo "      \"high_vulnerabilities\": ${high_count}," >> "${summary_file}"
            echo "      \"report_file\": \"${service}_scan_${TIMESTAMP}.json\"" >> "${summary_file}"
            echo "    }" >> "${summary_file}"
        fi
    done
    
    echo "  ]" >> "${summary_file}"
    echo "}" >> "${summary_file}"
    
    # Generate HTML report
    cat > "${html_report}" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>CRM Platform Security Scan Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .service { margin: 10px 0; padding: 10px; border-left: 4px solid #ccc; }
        .critical { border-left-color: #d32f2f; background-color: #ffebee; }
        .high { border-left-color: #f57c00; background-color: #fff3e0; }
        .clean { border-left-color: #388e3c; background-color: #e8f5e8; }
        .summary { background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>CRM Platform Security Scan Report</h1>
        <p>Generated: $(date)</p>
    </div>
    
    <div class="summary">
        <h2>Summary</h2>
        <p>Total services scanned: ${#SERVICES[@]}</p>
    </div>
    
    <h2>Service Details</h2>
EOF
    
    for service in "${SERVICES[@]}"; do
        local service_report="${SCAN_RESULTS_DIR}/${service}_scan_${TIMESTAMP}.json"
        if [[ -f "${service_report}" ]]; then
            local high_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "HIGH")] | length' "${service_report}" 2>/dev/null || echo "0")
            local critical_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "${service_report}" 2>/dev/null || echo "0")
            
            local css_class="clean"
            local status="✅ Clean"
            if [[ $critical_count -gt 0 ]]; then
                css_class="critical"
                status="❌ Critical Issues"
            elif [[ $high_count -gt 0 ]]; then
                css_class="high"
                status="⚠️ High Issues"
            fi
            
            cat >> "${html_report}" << EOF
    <div class="service ${css_class}">
        <h3>${service}</h3>
        <p>Status: ${status}</p>
        <p>Critical: ${critical_count}, High: ${high_count}</p>
        <p><a href="${service}_scan_${TIMESTAMP}.json">View detailed report</a></p>
    </div>
EOF
        fi
    done
    
    echo "</body></html>" >> "${html_report}"
    
    echo -e "${GREEN}Summary report generated: ${summary_file}${NC}"
    echo -e "${GREEN}HTML report generated: ${html_report}${NC}"
}

# Function to check for base image updates
check_base_image_updates() {
    echo -e "${BLUE}Checking for base image updates...${NC}"
    
    local base_images=("openjdk:17-jdk-slim" "openjdk:17-jre-slim")
    
    for image in "${base_images[@]}"; do
        echo -e "${YELLOW}Pulling latest ${image}...${NC}"
        docker pull "${image}"
    done
}

# Main execution
main() {
    echo -e "${BLUE}Starting security scan...${NC}"
    
    # Pre-flight checks
    check_docker
    check_trivy
    
    # Update Trivy database
    echo -e "${BLUE}Updating Trivy vulnerability database...${NC}"
    trivy image --download-db-only
    
    # Check for base image updates
    check_base_image_updates
    
    # Scan all services
    local failed_services=()
    local warning_services=()
    
    for service in "${SERVICES[@]}"; do
        if scan_service "${service}"; then
            continue
        elif [[ $? -eq 2 ]]; then
            warning_services+=("${service}")
        else
            failed_services+=("${service}")
        fi
    done
    
    # Generate summary
    generate_summary
    
    # Final report
    echo ""
    echo -e "${BLUE}=== Scan Complete ===${NC}"
    echo -e "${GREEN}Clean services: $((${#SERVICES[@]} - ${#warning_services[@]} - ${#failed_services[@]}))${NC}"
    echo -e "${YELLOW}Services with warnings: ${#warning_services[@]}${NC}"
    echo -e "${RED}Services with critical issues: ${#failed_services[@]}${NC}"
    
    if [[ ${#failed_services[@]} -gt 0 ]]; then
        echo -e "${RED}Services with critical vulnerabilities:${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "${RED}  - ${service}${NC}"
        done
        exit 1
    elif [[ ${#warning_services[@]} -gt 0 ]]; then
        echo -e "${YELLOW}Services with high vulnerabilities:${NC}"
        for service in "${warning_services[@]}"; do
            echo -e "${YELLOW}  - ${service}${NC}"
        done
        exit 2
    else
        echo -e "${GREEN}All services passed security scan!${NC}"
        exit 0
    fi
}

# Run main function
main "$@"