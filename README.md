# VERITAS

Standardized procurement and audit management system.

## Requirements

- Docker & Docker Compose
- Make
- NPM
- Java 21 (if running backend locally)

## Quick Start

To start the database in docker run:
```Bash
make db-only
```

To start the backend locally run:
```Bash
make run-backend
```

To start the frontend locally run:
```Bash
make run-frontend
```

To reset the database run:
```Bash
make down
make db-only
```

## Useful Makefile Commands

Clean the frontend and backend:
```Bash
make clean
```

## Useful Information

- Once the Backend is started, the API is available at: http://localhost:8080/api/v1/swagger-ui.html
- After running `make test-backend` a test report is generated at `./backend/build/reports/jacoco/test/html/`