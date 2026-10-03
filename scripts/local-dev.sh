#!/bin/bash
set -e
COMPOSE_FILE="docker-compose.dev.yml"
PROJECT_NAME="video-platform"

echo "Video Platform local development infrastructure"

check() {
  command -v docker >/dev/null || { echo "Docker is required."; exit 1; }
  docker info >/dev/null || { echo "Docker daemon is not running."; exit 1; }
}

case "${1:-up}" in
  up)
    check
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d redis kafka elasticsearch minio
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up minio-init
    echo "Infrastructure is ready. Spring Boot services use H2 in the dev profile."
    echo "Gateway:       http://localhost:8080 (run backend/api-gateway locally)"
    echo "Redis:         localhost:6379"
    echo "Kafka:         localhost:9092"
    echo "Kafka UI:      http://localhost:8095"
    echo "Elasticsearch: http://localhost:9200"
    echo "MinIO:         http://localhost:9000"
    echo "MinIO Console: http://localhost:9001"
    ;;
  down)
    check
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down
    ;;
  logs)
    check
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" logs -f
    ;;
  status)
    check
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" ps
    ;;
  clean)
    check
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down -v
    ;;
  restart)
    "$0" down
    "$0" up
    ;;
  *)
    echo "Usage: $0 [up|down|logs|status|clean|restart]"
    exit 1
    ;;
esac
