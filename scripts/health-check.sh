#!/bin/bash
# Video Platform - Health Check Script
# Verifies all infrastructure services are running correctly

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[⚠]${NC} $1"; }
log_error() { echo -e "${RED}[✗]${NC} $1"; }

FAILED=0

check_service() {
    local name=$1
    local check_cmd=$2
    local description=$3
    
    log_info "Checking $name: $description"
    if eval "$check_cmd" >/dev/null 2>&1; then
        log_success "$name is healthy"
        return 0
    else
        log_error "$name is NOT healthy"
        FAILED=1
        return 1
    fi
}

check_http() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}
    
    log_info "Checking $name: $url"
    status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")
    if [ "$status" = "$expected_status" ] || [ "$status" = "200" ] || [ "$status" = "201" ] || [ "$status" = "401" ] || [ "$status" = "403" ]; then
        log_success "$name responded with HTTP $status"
        return 0
    else
        log_error "$name responded with HTTP $status (expected $expected_status)"
        FAILED=1
        return 1
    fi
}

echo "========================================"
echo "Video Platform - Health Check"
echo "========================================"
echo ""

# # Redis
check_service "Redis" \
    "docker exec video-platform-redis redis-cli ping | grep -q PONG" \
    "Redis responding to PING"

# Kafka
check_service "Kafka" \
    "docker exec video-platform-kafka kafka-broker-api-versions --bootstrap-server localhost:9092" \
    "Kafka broker API accessible"

# Elasticsearch
check_http "Elasticsearch" "http://localhost:9200/_cluster/health" 200

# MinIO
check_http "MinIO API" "http://localhost:9000/minio/health/live" 200
check_http "MinIO Console" "http://localhost:9001" 200

# Kafka UI
check_http "Kafka UI" "http://localhost:8095" 200

# API Gateway
check_http "API Gateway" "http://localhost:8080/actuator/health" 200

echo ""
echo "========================================"
if [ $FAILED -eq 0 ]; then
    log_success "All health checks PASSED"
    echo "========================================"
    exit 0
else
    log_error "Some health checks FAILED"
    echo "========================================"
    exit 1
fi