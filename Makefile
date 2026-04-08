BACKEND_DIR = ./backend
FRONTEND_DIR = ./frontend

.PHONY: help up down restart db-only run-local build clean test logs

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

up: ## Start everything (DB + Backend) in Docker containers
	docker compose up -d

down: ## Stop and remove all containers
	docker compose down

restart: down up ## Restart the entire Docker stack

db-only: ## Start only the PostgreSQL container (for local development)
	docker compose up -d db

logs: ## View real-time logs from Docker
	docker compose logs -f

build: db-only ## Build the JAR file via the backend Makefile
	$(MAKE) -C $(BACKEND_DIR) build

run-local: db-only ## Run the backend locally
	@echo "Starting backend with DB_HOST=localhost..."
	export DB_HOST=localhost && $(MAKE) -C $(BACKEND_DIR) run

test: db-only ## Run all backend tests
	$(MAKE) -C $(BACKEND_DIR) test

clean: ## Clean everything
	$(MAKE) -C $(BACKEND_DIR) clean
