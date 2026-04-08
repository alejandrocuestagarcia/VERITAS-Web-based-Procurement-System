BACKEND_DIR = ./backend
FRONTEND_DIR = ./frontend

.PHONY: help up down restart db-only run-be run-fe install-fe build-all clean

# DOCKER

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

up: ## Start DB and Backend in Docker
	docker compose up -d

down: ## Stop all Docker containers
	docker compose down

db-only: ## Start ONLY the Database (Required for local runs)
	docker compose up -d db

logs: ## View Docker logs
	docker compose logs -f

# BACKEND

run-backend: db-only ## Start the Spring Boot backend locally
	export DB_HOST=localhost && $(MAKE) -C $(BACKEND_DIR) run

build-backend: ## Build the Backend JAR
	$(MAKE) -C $(BACKEND_DIR) build

# FRONTEND

install-frontend: ## Install Frontend dependencies (node_modules)
	$(MAKE) -C $(FRONTEND_DIR) install

run-frontend: install-frontend ## Start the Angular frontend locally
	$(MAKE) -C $(FRONTEND_DIR) run

build-frontend: ## Build the Frontend
	$(MAKE) -C $(FRONTEND_DIR) build

build-all: build-backend ## Build everything
	$(MAKE) -C $(FRONTEND_DIR) build

# SETUP

clean: ## Remove build files and node_modules from both folders
	$(MAKE) -C $(BACKEND_DIR) clean
	$(MAKE) -C $(FRONTEND_DIR) clean

init: db-only install-frontend ## One-time setup: Start DB and install frontend libs
	@echo "✅ Setup complete. Use 'make run-be' and 'make run-fe' in two terminals."