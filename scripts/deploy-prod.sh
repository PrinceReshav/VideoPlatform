#!/bin/bash
# Video Platform - Production Deployment Script
# Usage: ./deploy-prod.sh [REGISTRY] [IMAGE_TAG]

set -euo pipefail

REGISTRY=${1:-ghcr.io/your-org}
IMAGE_TAG=${2:-latest}
IMAGE_NAME=${IMAGE_NAME:-video-platform}
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NAMESPACE=video-platform-prod
KUBECONFIG=${KUBECONFIG:-~/.kube/config}

echo "=========================================="
echo "Video Platform Production Deployment"
echo "Registry: $REGISTRY"
echo "Tag: $IMAGE_TAG"
echo "Namespace: $NAMESPACE"
echo "=========================================="

# Check prerequisites
check_prereqs() {
    echo "Checking prerequisites..."
    
    if ! command -v kubectl &> /dev/null; then
        echo "ERROR: kubectl not found"
        exit 1
    fi
    
    if ! command -v kustomize &> /dev/null; then
        echo "ERROR: kustomize not found"
        exit 1
    fi
    
    if ! kubectl cluster-info &> /dev/null; then
        echo "ERROR: Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    echo "Prerequisites OK"
}

# Build and push images
build_images() {
    echo "Building Docker images..."
    
    cd "$ROOT_DIR"
    
    # Build all services
    services=(
        "api-gateway"
        "auth-service"
        "user-creator-service"
        "video-service"
        "upload-service"
        "transcoding-service"
        "search-service"
        "engagement-service"
        "subscription-service"
        "playlist-service"
        "notification-service"
        "analytics-service"
        "moderation-service"
        "live-tv-service"
        "frontend"
    )
    
    for service in "${services[@]}"; do
        echo "Building $service..."
        if [[ "$service" == "frontend" ]]; then
            docker build -t "$REGISTRY/video-platform-frontend:$IMAGE_TAG" ./frontend
        else
            docker build -t "$REGISTRY/video-platform-$service:$IMAGE_TAG" ./backend/$service
        fi
    done
    
    # Push images
    for service in "${services[@]}"; do
        echo "Pushing $service..."
        docker push $REGISTRY/video-platform-$service:$IMAGE_TAG
    done
    
    # Frontend
    docker push $REGISTRY/video-platform-frontend:$IMAGE_TAG
}

# Deploy to Kubernetes
deploy_k8s() {
    echo "Deploying to Kubernetes..."
    
    # Create namespace if not exists
    kubectl apply -f infrastructure/kubernetes/base/namespace.yaml
    
    # Apply secrets (ensure they exist)
    if ! kubectl get secret video-platform-secrets -n video-platform-prod &> /dev/null; then
        echo "WARNING: Secret 'video-platform-secrets' not found in namespace video-platform-prod"
        echo "Please create the secret with production values before deploying"
        echo "kubectl create secret generic video-platform-secrets -n video-platform-prod --from-literal=POSTGRES_PASSWORD=... --from-literal=JWT_SECRET=... etc."
        read -p "Press Enter to continue after creating secrets..."
    fi
    
    # Apply ConfigMap
    kubectl apply -f infrastructure/kubernetes/base/configmap-secret.yaml
    
    # Apply kustomization
    cd infrastructure/kubernetes/overlays/prod
    kustomize edit set image \
        ${REGISTRY}/${IMAGE_NAME}-api-gateway:latest=${REGISTRY}/video-platform-api-gateway:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-auth-service:latest=${REGISTRY}/video-platform-auth-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-user-creator-service:latest=${REGISTRY}/video-platform-user-creator-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-video-service:latest=${REGISTRY}/video-platform-video-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-upload-service:latest=${REGISTRY}/video-platform-upload-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-transcoding-service:latest=${REGISTRY}/video-platform-transcoding-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-search-service:latest=${REGISTRY}/video-platform-search-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-engagement-service:latest=${REGISTRY}/video-platform-engagement-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-subscription-service:latest=${REGISTRY}/video-platform-subscription-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-playlist-service:latest=${REGISTRY}/video-platform-playlist-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-notification-service:latest=${REGISTRY}/video-platform-notification-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-analytics-service:latest=${REGISTRY}/video-platform-analytics-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-moderation-service:latest=${REGISTRY}/video-platform-moderation-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-live-tv-service:latest=${REGISTRY}/video-platform-live-tv-service:$IMAGE_TAG \
        ${REGISTRY}/${IMAGE_NAME}-frontend:latest=${REGISTRY}/video-platform-frontend:$IMAGE_TAG
    
    kustomize build . | kubectl apply -f -
    
    echo "Waiting for deployments to be ready..."
    kubectl rollout status deployment/api-gateway -n video-platform-prod --timeout=300s
    kubectl rollout status deployment/auth-service -n video-platform-prod --timeout=300s
    kubectl rollout status deployment/video-service -n video-platform-prod --timeout=300s
    kubectl rollout status deployment/upload-service -n video-platform-prod --timeout=300s
    kubectl rollout status deployment/transcoding-service -n video-platform-prod --timeout=300s
    kubectl rollout status deployment/live-tv-service -n video-platform-prod --timeout=300s
    
    echo "Deployment complete!"
}

# Health check
health_check() {
    echo "Running health checks..."
    
    services=(
        "api-gateway:8080"
        "auth-service:8081"
        "user-creator-service:8082"
        "video-service:8083"
        "upload-service:8084"
        "transcoding-service:8080"
        "search-service:8085"
        "engagement-service:8086"
        "subscription-service:8087"
        "playlist-service:8088"
        "notification-service:8089"
        "analytics-service:8090"
        "moderation-service:8091"
        "live-tv-service:8092"
    )
    
    for svc in "${services[@]}"; do
        name=$(echo $svc | cut -d: -f1)
        port=$(echo $svc | cut -d: -f2)
        echo "Checking $name..."
        if kubectl exec -n video-platform-prod deployment/$name -- curl -f http://localhost:$port/actuator/health/liveness &> /dev/null; then
            echo "✓ $name is healthy"
        else
            echo "✗ $name health check failed"
        fi
    done
}

# Main
main() {
    case "${1:-deploy}" in
        build)
            build_images
            ;;
        deploy)
            check_prereqs
            deploy_k8s
            health_check
            ;;
        rollback)
            echo "Rolling back..."
            kubectl rollout undo deployment -n video-platform-prod
            ;;
        status)
            kubectl get pods -n video-platform-prod -o wide
            ;;
        logs)
            kubectl logs -n video-platform-prod -l app=${2:-api-gateway} --tail=100 -f
            ;;
        *)
            echo "Usage: $0 {build|deploy|rollback|status|logs} [REGISTRY] [TAG]"
            echo "  build   - Build and push Docker images"
            echo "  deploy  - Deploy to Kubernetes (default)"
            echo "  rollback - Rollback last deployment"
            echo "  status  - Show deployment status"
            echo "  logs    - Show logs for a service"
            exit 1
            ;;
    esac
}

main "$@"