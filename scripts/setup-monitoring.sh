#!/bin/bash

# CRM Platform Monitoring Setup Script
# This script sets up comprehensive monitoring and observability for the CRM platform

set -e

echo "🚀 Setting up CRM Platform Monitoring and Observability..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker and Docker Compose are installed
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_status "Prerequisites check passed ✅"
}

# Create necessary directories
create_directories() {
    print_status "Creating monitoring directories..."
    
    mkdir -p infra/docker/config/prometheus
    mkdir -p infra/docker/config/grafana/provisioning/dashboards
    mkdir -p infra/docker/config/grafana/provisioning/datasources
    mkdir -p infra/docker/config/alertmanager
    mkdir -p infra/docker/config/logstash/pipeline
    mkdir -p logs/prometheus
    mkdir -p logs/grafana
    mkdir -p logs/elasticsearch
    mkdir -p logs/logstash
    mkdir -p logs/kibana
    
    print_status "Directories created ✅"
}

# Set up Prometheus configuration
setup_prometheus() {
    print_status "Setting up Prometheus configuration..."
    
    # Prometheus configuration is already created
    if [ -f "infra/docker/config/prometheus/prometheus.yml" ]; then
        print_status "Prometheus configuration found ✅"
    else
        print_error "Prometheus configuration not found!"
        exit 1
    fi
    
    # Alert rules configuration
    if [ -f "infra/docker/config/prometheus/alerts.yml" ]; then
        print_status "Prometheus alert rules found ✅"
    else
        print_error "Prometheus alert rules not found!"
        exit 1
    fi
}

# Set up Grafana dashboards
setup_grafana() {
    print_status "Setting up Grafana dashboards..."
    
    # Check if dashboard files exist
    dashboards=(
        "infra/docker/grafana/provisioning/dashboards/crm-overview.json"
        "infra/docker/grafana/provisioning/dashboards/crm-business-metrics.json"
        "infra/docker/grafana/provisioning/dashboards/crm-security-metrics.json"
    )
    
    for dashboard in "${dashboards[@]}"; do
        if [ -f "$dashboard" ]; then
            print_status "Dashboard $(basename $dashboard) found ✅"
        else
            print_error "Dashboard $dashboard not found!"
            exit 1
        fi
    done
}

# Set up Alertmanager
setup_alertmanager() {
    print_status "Setting up Alertmanager..."
    
    if [ -f "infra/docker/config/alertmanager/alertmanager.yml" ]; then
        print_status "Alertmanager configuration found ✅"
    else
        print_error "Alertmanager configuration not found!"
        exit 1
    fi
}

# Set up ELK Stack
setup_elk() {
    print_status "Setting up ELK Stack for centralized logging..."
    
    if [ -f "infra/docker/config/logstash/logstash.yml" ]; then
        print_status "Logstash configuration found ✅"
    else
        print_error "Logstash configuration not found!"
        exit 1
    fi
    
    if [ -f "infra/docker/config/logstash/pipeline/logstash.conf" ]; then
        print_status "Logstash pipeline configuration found ✅"
    else
        print_error "Logstash pipeline configuration not found!"
        exit 1
    fi
}

# Start monitoring services
start_monitoring_services() {
    print_status "Starting monitoring services..."
    
    # Start only monitoring-related services first
    docker-compose -f infra/docker/docker-compose.yml up -d \
        prometheus \
        grafana \
        alertmanager \
        elasticsearch \
        logstash \
        kibana \
        zipkin \
        postgres-exporter \
        redis-exporter \
        kafka-exporter
    
    print_status "Monitoring services started ✅"
}

# Wait for services to be ready
wait_for_services() {
    print_status "Waiting for services to be ready..."
    
    # Wait for Prometheus
    print_status "Waiting for Prometheus..."
    until curl -f http://localhost:9090/-/ready &> /dev/null; do
        sleep 2
    done
    print_status "Prometheus is ready ✅"
    
    # Wait for Grafana
    print_status "Waiting for Grafana..."
    until curl -f http://localhost:3000/api/health &> /dev/null; do
        sleep 2
    done
    print_status "Grafana is ready ✅"
    
    # Wait for Elasticsearch
    print_status "Waiting for Elasticsearch..."
    until curl -f http://localhost:9200/_cluster/health &> /dev/null; do
        sleep 5
    done
    print_status "Elasticsearch is ready ✅"
    
    # Wait for Kibana
    print_status "Waiting for Kibana..."
    until curl -f http://localhost:5601/api/status &> /dev/null; do
        sleep 5
    done
    print_status "Kibana is ready ✅"
}

# Display access information
display_access_info() {
    print_status "🎉 Monitoring setup completed successfully!"
    echo ""
    echo "📊 Access your monitoring tools:"
    echo "  • Prometheus:    http://localhost:9090"
    echo "  • Grafana:       http://localhost:3000 (admin/admin)"
    echo "  • Alertmanager:  http://localhost:9093"
    echo "  • Kibana:        http://localhost:5601"
    echo "  • Zipkin:        http://localhost:9411"
    echo ""
    echo "📈 Available Grafana Dashboards:"
    echo "  • CRM Platform Overview"
    echo "  • CRM Business Metrics"
    echo "  • CRM Security Metrics"
    echo ""
    echo "🔍 Log Analysis:"
    echo "  • Structured logs are being collected in Elasticsearch"
    echo "  • Use Kibana to search and analyze logs"
    echo "  • Correlation IDs are automatically added to all logs"
    echo ""
    echo "⚠️  Alert Configuration:"
    echo "  • Update infra/docker/config/alertmanager/alertmanager.yml"
    echo "  • Configure your email/Slack webhooks for notifications"
    echo ""
    print_status "Happy monitoring! 🚀"
}

# Main execution
main() {
    echo "🔧 CRM Platform Monitoring Setup"
    echo "================================="
    
    check_prerequisites
    create_directories
    setup_prometheus
    setup_grafana
    setup_alertmanager
    setup_elk
    start_monitoring_services
    wait_for_services
    display_access_info
}

# Run main function
main "$@"