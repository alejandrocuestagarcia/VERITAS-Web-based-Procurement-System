# VERITAS

Standardized procurement and audit management system.

## Requirements

- Docker & Docker Compose
- Make
- Java 21 (if running backend locally)

## Quick Start

To spin up the entire environment locally run (except docker database):
```Bash
make run-local
```

To spin up the entire environment in Docker run:
```Bash
make up
```

## Useful Information

- Once the Backend is started, the API is available at: http://localhost:8080/api/v1/swagger-ui.html

