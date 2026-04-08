.PHONY: help build run test clean docker-build

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Build the backend JAR
	./gradlew clean bootJar

run: ## Run the backend locally
	./gradlew bootRun

test: ## Run backend tests
	./gradlew test

clean: ## Clean backend build files
	./gradlew clean

docker-build: ## Build only the backend Docker image
	docker build -t veritas-backend:latest .