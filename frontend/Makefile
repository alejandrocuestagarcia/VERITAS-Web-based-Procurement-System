.PHONY: help install run build test clean docker-build

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

install: ## Install dependencies using npm
	npm install

run: install ## Start the development server using npx
	npx ng serve

build: install ## Build the production artifacts
	npx ng build

test: install ## Run unit tests
	npx ng test

clean: ## Remove build artifacts and node_modules
	rm -rf dist/
	rm -rf node_modules/
