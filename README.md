# VERITAS

Standardized procurement and audit management system.

Built as part of the **Advanced Software Engineering** course at **TU Wien** (2026 Summer Semester).

## Tech Stack

| Layer      | Technology                                          |
|------------|-----------------------------------------------------|
| Frontend   | Angular 19, Tailwind CSS                            |
| Backend    | Java 21, Spring Boot 3.5, Spring Security, JPA      |
| Database   | PostgreSQL 16                                       |
| Build      | Gradle, npm                                         |
| CI/CD      | GitLab CI (test → build → deploy to Kubernetes)     |
| API        | OpenAPI 3 with auto-generated TypeScript client      |

## Requirements

- Docker & Docker Compose
- Make
- NPM
- Java 21 (if running backend locally)

## Quick Start

```bash
make init          # one-time: starts DB in Docker + installs frontend deps
make run-backend   # terminal 1: starts the Spring Boot backend
make run-frontend  # terminal 2: starts the Angular frontend (waits for backend)
```

No `.env` file is needed — sensible defaults are built in for local development.

## Optional: Environment Overrides

To enable **email notifications** or **live currency exchange rates**, create a `.env` file:

```bash
cp .env.example .env
```

Then fill in any of these optional values:
- `MAIL_USERNAME` / `MAIL_PASSWORD` — SMTP credentials (e.g. Gmail App Password)
- `EXCHANGE_RATE_API_KEY` — free key from [exchangerate-api.com](https://www.exchangerate-api.com/)
- `JWT_SECRET_KEY` — custom signing key (a default is provided for local dev)

> **Note:** The app runs fully without these — only email and currency features will be non-functional.

## Reset the Database

```bash
make down
make db-only
```

## Useful Makefile Commands

| Command              | Description                           |
|----------------------|---------------------------------------|
| `make init`          | One-time setup (DB + frontend deps)   |
| `make up`            | Start DB + backend in Docker          |
| `make down`          | Stop all Docker containers            |
| `make run-backend`   | Start Spring Boot backend locally     |
| `make run-frontend`  | Start Angular frontend locally        |
| `make test-backend`  | Run backend tests                     |
| `make clean`         | Remove build files and node_modules   |
| `make help`          | Show all available commands           |

## Useful Information

- Once the backend is started, the API docs are available at: http://localhost:8080/api/v1/swagger-ui.html
- After running `make test-backend`, a test coverage report is generated at `./backend/build/reports/jacoco/test/html/`