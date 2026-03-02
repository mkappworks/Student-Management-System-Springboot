.PHONY: build test up down logs clean help

# Colors
GREEN  := \033[0;32m
YELLOW := \033[0;33m
RESET  := \033[0m

help: ## Show this help
	@echo "$(GREEN)Student Management System - Available Commands$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(RESET) %s\n", $$1, $$2}'

build: ## Build all services (skip tests)
	@echo "$(GREEN)Building all services...$(RESET)"
	mvn clean package -DskipTests

test: ## Run all unit tests
	@echo "$(GREEN)Running unit tests...$(RESET)"
	mvn test

test-integration: ## Run integration tests (requires Docker)
	@echo "$(GREEN)Running integration tests...$(RESET)"
	mvn verify

up: ## Start all services with Docker Compose
	@echo "$(GREEN)Starting all services...$(RESET)"
	docker compose up -d

up-infra: ## Start only infrastructure (DBs + service-registry)
	@echo "$(GREEN)Starting infrastructure...$(RESET)"
	docker compose up -d postgres-student postgres-teacher postgres-grade \
		postgres-enrollment postgres-notification postgres-module postgres-auth \
		service-registry

up-monitoring: ## Start monitoring stack
	docker compose up -d prometheus grafana

down: ## Stop all services
	@echo "$(YELLOW)Stopping all services...$(RESET)"
	docker compose down

down-volumes: ## Stop all services and remove volumes
	@echo "$(YELLOW)Stopping all services and removing volumes...$(RESET)"
	docker compose down -v

logs: ## Tail logs for all services
	docker compose logs -f

logs-svc: ## Tail logs for a specific service (usage: make logs-svc SVC=student-service)
	docker compose logs -f $(SVC)

ps: ## Show running containers
	docker compose ps

rebuild: ## Rebuild and restart a specific service (usage: make rebuild SVC=student-service)
	docker compose up -d --build --no-deps $(SVC)

clean: ## Clean Maven build artifacts
	mvn clean

health: ## Check health of all services
	@echo "$(GREEN)Checking service health...$(RESET)"
	@curl -s http://localhost:8761/actuator/health | python3 -m json.tool 2>/dev/null || echo "service-registry: DOWN"
	@curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || echo "api-gateway: DOWN"
	@curl -s http://localhost:8090/actuator/health | python3 -m json.tool 2>/dev/null || echo "auth-service: DOWN"
	@curl -s http://localhost:8081/actuator/health | python3 -m json.tool 2>/dev/null || echo "student-service: DOWN"
	@curl -s http://localhost:8082/actuator/health | python3 -m json.tool 2>/dev/null || echo "teacher-service: DOWN"
	@curl -s http://localhost:8086/actuator/health | python3 -m json.tool 2>/dev/null || echo "module-service: DOWN"
	@curl -s http://localhost:8083/actuator/health | python3 -m json.tool 2>/dev/null || echo "grade-service: DOWN"
	@curl -s http://localhost:8084/actuator/health | python3 -m json.tool 2>/dev/null || echo "enrollment-service: DOWN"
	@curl -s http://localhost:8085/actuator/health | python3 -m json.tool 2>/dev/null || echo "notification-service: DOWN"
