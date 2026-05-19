# Library Catalog API

RESTful API for Library Catalog Management - A transactional core for physical inventory management.

## Tech Stack

- Java 21
- Spring Boot 3.x
- PostgreSQL 16
- Maven
- Docker & Docker Compose

## How to Run

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

### 2. Start Infrastructure

Start PostgreSQL via Docker Compose:

```bash
docker compose up -d

# Verify status
docker compose ps
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
# The docker-compose.yml uses a .env file
# You can source the same variables
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

**Or with explicit profile:**
```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

**Production mode:**
```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### 5. Port Configuration

To use a different port:
```bash
export SERVER_PORT=8081
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running Tests

Tests use TestContainers with PostgreSQL 16 (singleton pattern for performance):

```bash
# Run all tests
./mvnw clean test

# Run specific test class
./mvnw test -Dtest=LibraryCatalogApiApplicationTests

# Run with verbose output
./mvnw test -X
```

**Test Features:**
- Automatic PostgreSQL container lifecycle management
- Database isolation via @Transactional rollback
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
│   ├── main/java/com/library/catalog/
│   │   ├── api/          # Controllers, DTOs, Mappers
│   │   ├── domain/       # Entities, Services, Repositories
│   │   └── infrastructure/ # Config, Security, Exception Handling
│   └── test/
├── .github/workflows/    # CI/CD pipelines
├── docker-compose.yml    # Infrastructure (PostgreSQL)
├── Dockerfile           # Application container
└── pom.xml             # Dependencies and plugins
```

## Development Guidelines

See [agent.md](AGENTS.md) for detailed architecture decisions, coding standards, and Definition of Done.

### Key Principles

- **TDD**: All features must have tests before implementation
- **Clean Architecture**: Layered architecture with package-by-feature
- **RFC 7807**: Standardized error responses
- **Soft Delete**: No physical deletions, use `ativo = false`
- **Pagination**: All list endpoints must be paginated
- **Java Records**: Use for all DTOs

## License

This project is open source and available under the [MIT License](LICENSE).
