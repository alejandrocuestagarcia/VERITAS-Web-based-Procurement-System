.PHONY: help build run test test-all test-unit test-integration clean docker-build

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Build the backend JAR
	./gradlew clean bootJar

run: ## Run the backend locally
	./gradlew bootRun

test: ## Run backend tests
	./gradlew test --rerun-tasks

test-all: ## Run all backend tests
	./gradlew test --rerun-tasks

test-unit: ## Run only unit tests
	./gradlew unitTest --rerun-tasks

test-integration: ## Run only integration tests
	./gradlew integrationTest --rerun-tasks

clean: ## Clean backend build files
	./gradlew clean

docker-build: ## Build only the backend Docker image
	docker build -t veritas-backend:latest .