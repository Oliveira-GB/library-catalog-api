# Library Catalog Management API

RESTful API acting as the transactional core and Single Source of Truth for physical library inventory management. Orchestrates relational entities (Books, Authors, Categories, Readers) and ensures consistency in batch loan state transitions through atomic operations.

## Tech Stack

- **Java 21**
- **Spring Boot 3.x**
- **PostgreSQL**
- **Flyway**
- **Docker & Docker Compose**

## Architectural Decisions

- **RFC 7807 Problem Details**: All API errors follow the RFC 7807 standard. Global exception handling via `@RestControllerAdvice` ensures uniform, sanitized error responses without leaking internal stack traces or database constraint names.
- **Soft Delete Pattern**: Physical deletion (`DELETE FROM`) is strictly forbidden for core catalog and identity entities. Records are logically excluded using `ativo = false`, preserving full historical audit trails and referential integrity.
- **Pessimistic Locking in Loan Engine**: The transactional loan engine uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the Reader record during batch operations. This guarantees serialization and prevents race conditions in concurrent loan requests without introducing optimistic locking complexity.
- **Fail-Fast Validations**: The loan validation engine operates in strict fail-fast mode. A `LoanValidationOrchestrator` executes isolated strategy validators sequentially *before* any entity mutation, guaranteeing trivial rollback and clean entity state on rejection.

## Quickstart Guide

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+ (or use `./mvnw`)
- Docker and Docker Compose
- Git

### 1. Clone and Navigate

```bash
git clone <repository-url>
cd library-catalog-api
```

### 2. Initialize the Environment

Start the PostgreSQL infrastructure using Docker Compose:

```bash
docker-compose up -d
```

### 3. Configure Environment Variables

The application requires the following environment variables (fail-fast if not set):

**Option A: Export manually**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/catalog
export DB_USER=catalog
export DB_PASSWORD=catalog
export SERVER_PORT=8080  # Optional, defaults to 8080
```

**Option B: Load from .env file**
```bash
set -a && source .env && set +a
export DB_URL=jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}
export DB_USER=${POSTGRES_USER}
export DB_PASSWORD=${POSTGRES_PASSWORD}
```

### 4. Run the Application

**Development mode (with SQL logging):**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Production mode:**
```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## Links and Access

- **Swagger UI (Interactive Documentation)**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON Contract**: http://localhost:8080/v3/api-docs

### Default Test Credentials

Use the following credentials to authenticate via the Swagger UI "Authorize" button or directly in the `Authorization` header using Basic Auth:

- **Username**: `admin`
- **Password**: `admin123`

> **Note**: Public catalog lookup endpoints (e.g., ISBN query under `/api/v1/catalogo/livros/**`) do not require authentication.

### Security Note

The interactive Swagger UI is intentionally exposed without authentication to serve as a public discovery and testing interface. This is a deliberate architectural trade-off that prioritizes API discoverability and developer experience over schema opacity. **All protected business endpoints (mutations, administrative routes, and reports) continue to enforce database-backed Basic Auth strictly.**

You may notice a lock icon on *all* endpoints within the Swagger UI, including public ones. This is a visual artifact of the globally configured security scheme and has no functional impact — the Spring Security filter chain independently enforces the actual access rules defined in the application.

## Test Execution

Run the full automated test suite locally using Maven:

```bash
./mvnw clean test
```

**Test Features:**
- Automatic PostgreSQL container lifecycle management via Testcontainers
- Database isolation via `@Transactional` rollback
- Same PostgreSQL 16 image as production
- No external database required for testing

### Code Coverage

```bash
./mvnw clean verify
# Report available at: target/site/jacoco/index.html
```

## SonarQube Integration

This project uses [SonarQube Cloud](https://sonarcloud.io) for continuous code quality analysis.

### Setup Instructions

1. Go to [sonarcloud.io](https://sonarcloud.io) and login with your GitHub account
2. Import this repository
3. Generate a Sonar Token in your project settings
4. Add the token as `SONAR_TOKEN` in your GitHub repository secrets:
   - Go to Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Name: `SONAR_TOKEN`
   - Value: Your SonarCloud token

### Quality Metrics

| Metric | Threshold |
|--------|-----------|
| Code Coverage | ≥ 70% |
| Duplications | ≤ 3% |
| Bugs | 0 (Critical/Blocker) |
| Vulnerabilities | 0 |
| Code Smells | ≤ 50 |

### Manual Analysis

To run SonarQube analysis locally:

```bash
./mvnw clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN
```

## Project Structure

```
├── src/
│   ├── main/java/github/oliveira/gb/librarycatalogapi/
│   │   ├── api/              # Controllers, DTOs (Java Records), Mappers
│   │   ├── domain/           # Entities, Services, Repositories, Validations
│   │   └── infrastructure/   # Config, Security, Exception Handling, OpenAPI
│   └── test/
├── .github/workflows/        # CI/CD pipelines
├── docker-compose.yml        # Infrastructure (PostgreSQL)
├── Dockerfile               # Multi-stage application container
└── pom.xml                  # Dependencies and plugins
```

## Development Guidelines

See [AGENTS.md](AGENTS.md) for detailed architecture decisions, coding standards, and Definition of Done.

### Key Principles

- **TDD**: All features must have tests before implementation
- **Clean Architecture**: Layered architecture with package-by-feature
- **RFC 7807**: Standardized error responses
- **Soft Delete**: No physical deletions, use `ativo = false`
- **Pagination**: All list endpoints must be paginated
- **Java Records**: Use for all DTOs

## License

This project is open source and available under the [MIT License](LICENSE).
