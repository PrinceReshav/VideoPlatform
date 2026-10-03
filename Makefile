# Video Platform - Makefile
# Common development commands

.PHONY: help up down restart logs status clean health test build generate-api seed

# Default target
help:
	@echo "Video Platform - Development Commands"
	@echo ""
	@echo "Infrastructure:"
	@echo "  make up           Start all infrastructure services"
	@echo "  make down         Stop all services"
	@echo "  make restart      Restart all services"
	@echo "  make logs         Show logs (add SERVICE=name to filter)"
	@echo "  make status       Check service health status"
	@echo "  make health       Run comprehensive health checks"
	@echo "  make clean        Remove all containers, volumes, and data"
	@echo ""
	@echo "Development:"
	@echo "  make build        Build all service Docker images"
	@echo "  make test         Run all tests"
	@echo "  make generate-api Generate TypeScript API client from OpenAPI specs"
	@echo "  make seed         Seed database with initial data"
	@echo ""
	@echo "Examples:"
	@echo "  make up"
	@echo "  make logs SERVICE=postgres"
	@echo "  make status"

# Infrastructure commands
up:
	./scripts/local-dev.sh up

down:
	./scripts/local-dev.sh down

restart:
	./scripts/local-dev.sh restart

logs:
	./scripts/local-dev.sh logs $(SERVICE)

status:
	./scripts/local-dev.sh status

health:
	./scripts/health-check.sh

clean:
	./scripts/local-dev.sh clean

# Build all service images
build:
	@echo "Building all service Docker images..."
	docker compose -f docker-compose.yml build --parallel

# Run tests (to be implemented per service)
test:
	@echo "Running tests for all services..."
	@for service in backend/*/; do \
		if [ -f "$$service/pom.xml" ]; then \
			echo "Testing $$service..."; \
			(cd "$$service" && ./mvnw test); \
		fi; \
	done
	@if [ -d "frontend" ]; then \
		echo "Testing frontend..."; \
		(cd frontend && npm test); \
	fi
	@if [ -d "mobile" ]; then \
		echo "Testing mobile..."; \
		(cd mobile && npm test); \
	fi

# Generate TypeScript API client from OpenAPI specs
generate-api:
	@echo "Generating API clients..."
	@./scripts/generate-api.sh

# Seed database with initial data
seed:
	@echo "Seeding database..."
	@./scripts/seed-data.sh

# Development workflow: up + health check
dev: up health
	@echo ""
	@echo "Development environment ready!"
	@echo "API Gateway: http://localhost:8080"
	@echo "Kafka UI:    http://localhost:8095"
	@echo "MinIO:       http://localhost:9001"

# Production-like validation
validate: build test health
	@echo "All validations passed!"