# Block 1: System Memory & Active Roadmap

> CRITICAL INSTRUCTION FOR THE AGENT: This file is the single source of truth for the project.
> 1. READING: Read this section before starting any new iteration or generating code.
> 2. STATUS UPDATE: Update the Schedule checkboxes [x] ONLY when a User Story (US) is 100% completed and tested according to the Definition of Done.
> 3. BLOCKER LOG: Do not record syntax errors or common test failures during development. Update the Troubleshooting section ONLY if an error persists after 3 failed correction attempts, or if an architectural change is required.

## 1. Project Overview & Scope
* **Project:** RESTful API for Library Catalog Management
* **Main Objective:** Act as the transactional core and Single Source of Truth for physical inventory, orchestrating relational entities and ensuring consistency in batch loan state transitions.
* **In-Scope (To Do):** CRUD for Book, Author, Category, and Reader. Atomic loan/return engine (`@Transactional`). Mandatory Soft Delete (`ativo = false`). Headless API. Mandatory Pagination. Simple security via Basic Auth.
* **Out-of-Scope (DO NOT DO):** Any User Interface (Frontend). Complex Security Layers (No JWT, OAuth, or advanced RBAC). Asynchronous Processing (No queues, `@Scheduled`, or emails). Multi-Tenant Architecture. Physical record deletion (`DELETE FROM` is forbidden).

## 2. Definition of Done (Quality Criteria & TDD)
A User Story (US) or task can only be marked as completed `[x]` if it meets ALL the criteria below:
* **TDD / Tests:** Unit and/or integration tests were written, executed, and are passing (including failure/rejection paths).
* **Clean Contracts & API Verification:** Exclusive use of Java Records as DTOs. API input/output JSON payloads must strictly match the designed DTOs. No implicit field serialization or JPA entity exposure in HTTP responses.
* **Validation & Integrity:** Jakarta Bean Validation constraints (`@NotBlank`, `@Size`, `@ISBN`) strictly applied.
* **No N+1 Queries:** No N+1 Query Problems introduced (use of `JOIN FETCH` validated). RFC 7807 (Problem Details) returned for all errors.
* **Architectural Adherence:** The generated code strictly follows the layer rules and restrictions defined in Blocks 2 and 3 of this document.

## 3. Current Focus (Active Task)
* **Global Status:** [READY]
* **Active Epic / US:** [Epic 1 - Central Catalog Management]
* **Immediate Goal:** [Start US 1.1 - Category Registration]

## 4. Decision Log & Troubleshooting
* [2026-05-17] [DECISION]: Using .env file for environment variables instead of hardcoded values in docker-compose.yml (security best practice)
* [2026-05-17] [DECISION]: SonarQube Cloud integration with 70% minimum code coverage threshold, JaCoCo for coverage reporting
* [YYYY-MM-DD] [BLOCKER RESOLVED]: [Empty]

## 5. Roadmap & Development Schedule (MVP Scope)

### Initial Infrastructure & Base Setup
- [x] Isolated environment configuration (Docker Compose and PostgreSQL).
- [x] SonarQube Cloud integration with code coverage reporting (JaCoCo).
- [ ] Repository initialization, base packages, and Flyway setup with initial Migration.
- [ ] Implementation of standard `RestControllerAdvice` (RFC 7807) and static security configuration (Basic Auth with BCrypt for Admin routes).

### Epic 1: Central Catalog Management
- [ ] US 1.1: Category Registration (POST with uniqueness rules and ativo=true).
- [ ] US 1.2: Category Query (Paginated GET, filtering inactive and dynamic search).
- [ ] US 1.3: Category Maintenance (PUT edit and DELETE via Soft Delete).
- [ ] US 1.4: Author Registration (POST with uniqueness rules).
- [ ] US 1.5: Author Query (Paginated GET, omitting biography in list).
- [ ] US 1.6: Author Maintenance (PUT edit and DELETE via Soft Delete, preserving history).
- [ ] US 1.7: Book Registration (Physical copy, strict validation of active dependencies and `@ISBN`).
- [ ] US 1.8: Book Details (Search by ID or ISBN, mandatory optimization with `JOIN FETCH`).
- [ ] US 1.9: Book Data and Relationship Edit (PUT with `@ManyToMany` sync via `@Transactional`).
- [ ] US 1.10: Book Inactivation (Soft Delete tied to lock: impossible to inactivate a book with EMPRESTADO status).
- [ ] US 1.11: Book Query (Paginated GET focused on lightweight navigation).

### Epic 2: Reader Management (Identity)
- [ ] US 2.1: Reader Registration (POST with unique CPF, synchronous external CEP API integration for embedded address).
- [ ] US 2.2: Paginated Reader Query (GET with cumulative filters: name, cpf, email).
- [ ] US 2.3: Reader Profile Details (GET by ID, including complete address).
- [ ] US 2.4: Reader Maintenance and Inactivation (PUT isolating immutable CPF and updating CEP via external API, and DELETE via Soft Delete).

### Epic 3: Transactional Loan and Return Engine
- [ ] US 3.1: Batch Loan Registration (POST with physical/financial delinquency locks and possession limit lock).
- [ ] US 3.2: Batch Term Renewal (PATCH with strict renewal limits and time rules).
- [ ] US 3.3: Return and Fine Settlement (GET flow for late fee calculation and POST with unified status change for `StatusLote` and `StatusLivro`).
- [ ] US 3.4: Flow and Delinquency Query (GET dashboard filtered by state, consolidating real and projected debts).
- [ ] US 3.5: Transaction History and Auditing (Chronological GET by reader).
- [ ] US 3.6: Pending Debt Settlement (Atomic POST migrating `valorMultaPendente` to fully paid, conditioned on return).

### Epic 4: Search Mechanism, Discovery, and Reports
- [ ] US 4.1: Parameterized Discovery Catalog (Public indexed GET without access restriction).
- [ ] US 4.2: Inventory Report (Export via Content Negotiation [CSV/PDF] locked to 100 results).
- [ ] US 4.3: Movement and Delinquency Report (Time interval filters based on US 3.6 and US 3.3 resolutions).
- [ ] US 4.4: ISBN Query API (Public integration route exposing derived boolean for physical copy availability).

# Block 2: Developer Guidelines

## 1. Setup & Build Commands
* Run tests: `./mvnw clean test`
* Build application: `./mvnw clean package -DskipTests`
* Run environment: `docker-compose up -d`

## 2. Tech Stack
* Language: Java 21
* Framework: Spring Boot 3.x
* Database: PostgreSQL
* Migrations: Flyway
* ORM / Data Access: Spring Data JPA / Hibernate
* Testing: JUnit 5, Mockito, SpringBootTest
* Validation: Jakarta Bean Validation
* Utilities: OpenAPI (Swagger)

## 3. Architecture & Directories
The project strictly follows a layered architecture combined with package-by-feature.

Directory Structure:
`src/main/java/com/library/catalog/`
* `api/`: Separated by feature (e.g., `api/livro/`, `api/leitor/`). Contains Controllers, DTOs (Records), Mappers.
* `domain/`: Separated by feature (e.g., `domain/livro/`, `domain/leitor/`). Contains Entities, Repositories, Services, Business Validations.
* `infrastructure/`: Cross-cutting concerns. Contains Global Exception Handling (`RestControllerAdvice`), Security, OpenAPI, external HTTP clients (e.g., ViaCEP).

### Layer Responsibilities
**API Layer**
* Responsible exclusively for receiving HTTP requests, input validation, DTO mapping, delegating to services, and returning HTTP responses.
* Controllers must remain thin. Absolutely no business logic is allowed in controllers.

**Domain Layer**
* Contains all business rules, state transitions, and workflow orchestration.
* Services contain business logic and orchestrate repositories.
* Repositories are responsible only for data access. Avoid business logic inside repositories.

**Infrastructure Layer**
* Responsible for cross-cutting concerns, external system integrations, and framework setup.

## 4. Database & Persistence Rules
* Schema Generation: Automatic schema generation (`ddl-auto=update` or `create`) is strictly forbidden.
* Migrations: All schema changes must be implemented via explicit SQL migration scripts in Flyway.
* Soft Delete: Physical deletion (`DELETE FROM`) is strictly prohibited. Use logical exclusion (`ativo = false`) and configure default database filters using `@SQLRestriction("ativo = true")`.
* Performance & Relationships: Prevent N+1 query problems. Use intentional fetching strategies (`JOIN FETCH` or `@EntityGraph`) for `ManyToMany` relationships. Prefer lazy loading by default.
* Timestamps & Auditing: Use Spring Data JPA Auditing. All entities must be annotated with `@EntityListeners(AuditingEntityListener.class)` and use `@CreatedDate` and `@LastModifiedDate` for `createdAt` and `updatedAt`. Do not set dates manually in services.

## 5. API & Code Conventions
* Naming Standards: Use `PascalCase` for classes and `camelCase` for variables and methods. API endpoints must be pluralized, use `kebab-case`, and be written in Portuguese (e.g., `/api/v1/livros`, `/api/v1/leitores`).
* API Versioning: All API routes must include a version prefix (e.g., `/api/v1/`).
* REST Responses: Map operations to standard HTTP status codes strictly (200 OK for queries/updates, 201 Created for creations, 204 No Content for deletions/inactivations, 400 Bad Request for validation errors, 404 Not Found, 409 Conflict, 422 Unprocessable Entity). Do not use generic JSON wrapper objects for responses; return the DTO directly.
* DTOs: Use native Java Records for data transfer to avoid cyclic serialization loops.
* Validation: Apply strict Jakarta Bean Validation annotations directly on the DTOs.
* Dependency Injection: Prefer constructor injection. Field injection (`@Autowired` on fields) is strictly forbidden.
* Pagination: Endpoints returning lists must implement Spring Data's `Pageable` natively.
* Transactions: Transaction boundaries (`@Transactional`) belong to the Service layer. Keep transactional methods atomic.
* Read-Only Transactions: All read-only service methods (GET operations) MUST be annotated with `@Transactional(readOnly = true)` to optimize database performance.

## 6. Exceptions & Logging
* API Errors: Error handling must be centralized globally using a `@RestControllerAdvice`.
* Standardization: API error responses must follow the RFC 7807 (Problem Details for HTTP APIs) standard.
* Information Leakage: Never expose internal stack traces or database constraint names in the HTTP response.

## 7. Testing Standards
* Pattern: Follow the Arrange / Act / Assert (AAA) structural pattern. Use Test-Driven Development (TDD) before implementing API routes.
* Unit Tests: Focus exclusively on the `domain` layer (Services and business rules). Use `Mockito` to isolate logic from the database and framework.
* Integration Tests: Focus on the `api` and `infrastructure` layers. Use `@SpringBootTest` and `MockMvc` to validate HTTP flows, JSON serialization, global exception handling, and database constraints (unique rules, formatting).
* Mocking: Mock external dependencies (like the CEP API) to keep tests deterministic.

# Block 3: Anti-Hallucination Boundaries

## 1. Scope Awareness & Restrictions
* Strict Focus: Only modify files and directories directly related to the active User Story (US). For example, if working on the `Leitor` domain, do not alter `Livro` or `Emprestimo` configurations unless strictly required by a relationship.
* Out-of-Scope Enforcement: Do not implement User Interfaces (Frontend) or template engines. Do not implement complex security mechanisms like JWT, OAuth, or RBAC. Do not implement asynchronous processing or `@Scheduled` tasks.
* No Spontaneous Refactoring: Do not rewrite, refactor, or alter existing code, architecture, or Flyway migrations outside the current scope.

## 2. Forbidden Practices (Strict Violations)
* Database Schema: Never use `spring.jpa.hibernate.ddl-auto=create` or `update`. All schema changes must be explicit SQL migration scripts in Flyway.
* Lombok Misuse on Entities: Never use `@Data`, `@EqualsAndHashCode`, or `@ToString` on JPA `@Entity` classes, as they cause infinite loops and severe performance issues in bidirectional relationships. Use `@Getter` and `@Setter` selectively.
* Physical Deletion: Never use `DELETE FROM` or `repository.delete()` for core entities. Soft delete (`ativo = false`) is mandatory for `Livro`, `Autor`, `Categoria`, and `Leitor` to preserve loan history.
* Entity Exposure: Never return JPA Entities from Controllers. Strictly use Java Records as DTOs for all API responses and requests.
* Controller Logic: Do not place business rules, state transitions, or transaction boundaries inside Controllers.
* Field Injection: Never use `@Autowired` on class fields. Strictly use constructor injection.
* N+1 Queries: Do not trigger lazy loading inside loops. Use `JOIN FETCH` or `@EntityGraph`.
* Exception Handling: Do not swallow exceptions or return default Spring error formats. Always use the global `@RestControllerAdvice` implementing RFC 7807.

## 3. Philosophy, Priorities & Anti-Overengineering
When making technical decisions, strictly follow this priority order:
1. Correctness (Does it strictly meet the acceptance criteria and domain rules?)
2. Maintainability (Is the code cohesive?)
3. Consistency (Does it match the existing project patterns?)
4. Simplicity (Is it the most straightforward solution?)

Anti-Overengineering Rules:
* Avoid premature abstraction. Do not create interfaces for Services or Repositories if there is only one implementation.
* Keep the Loan Engine Atomic: The `Emprestimo` rules (late fees, hoarding limits, status transitions) must remain simple, atomic, and strictly encapsulated within the Service layer under `@Transactional`.
* Do not invent libraries: Do not add dependencies for email sending, PDF generation (unless specifically tackling the US 4.2 export), or notification systems "just in case".
* Always ask for clarification before making risky assumptions about the loan business rules or database constraints.
