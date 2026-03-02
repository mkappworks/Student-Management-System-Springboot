# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build all services (skip tests)
mvn clean package -DskipTests
# or
make build

# Run all unit tests (across all services)
mvn test

# Run a single service's tests
cd student-service && mvn test

# Run integration tests (requires Docker for Testcontainers)
mvn verify

# Full Docker stack
make up           # start all services
make up-infra     # start only DBs + service-registry (for local dev)
make down         # stop all
make down-volumes # stop and remove volumes (full reset)

# Rebuild a single service in Docker
make rebuild SVC=student-service

# Tail logs
make logs
make logs-svc SVC=student-service

# Health check all services
make health
```

## Architecture Overview

This is a **Spring Boot 3.2 / Java 21** microservices system. All inter-service traffic goes through the **API Gateway** (`:8080`), which validates JWTs before forwarding requests.

### Request Flow
```
Client → API Gateway (:8080) → JWT filter → downstream service → PostgreSQL (own DB)
                                              ↑
                                    Service Registry (Eureka :8761)
```

### JWT Auth Model
- **auth-service** (`:8090`) issues JWTs on login/register.
- **API Gateway** validates the JWT using `JwtAuthenticationFilter` (a reactive `GlobalFilter`) and injects `X-User-Id` and `X-User-Role` headers into downstream requests.
- Each downstream service also has its own `SecurityConfig` + `JwtAuthFilter` that reads these headers or re-validates the token. The JWT secret (`JWT_SECRET`) must be identical across all services.
- Open endpoints (no auth): `/api/v1/auth/**`, `/eureka/**`, `/actuator/**`.

### Service Internals Pattern
Each domain service follows the same layered structure:
- `controller/` — REST endpoints with `@PreAuthorize` role checks
- `service/` — business logic, `@Transactional`
- `repository/` — Spring Data JPA
- `model/` — JPA entities
- `dto/` — request/response objects
- `security/` — `JwtAuthFilter`, `JwtService`
- `config/` — `SecurityConfig`, `ApplicationConfig`
- `exception/` — `ResourceNotFoundException`, `DuplicateResourceException`, `GlobalExceptionHandler`

### Database
- Each service has its **own PostgreSQL database** (separate container in Docker Compose).
- Schema managed via **Flyway** (`src/main/resources/db/migration/V1__*.sql`).
- JPA `ddl-auto: validate` — schema changes must go through Flyway migrations.

### Service Ports
| Service           | Port |
|-------------------|------|
| service-registry  | 8761 |
| api-gateway       | 8080 |
| auth-service      | 8090 |
| student-service   | 8081 |
| teacher-service   | 8082 |
| module-service    | 8086 |
| grade-service     | 8083 |
| enrollment-service| 8084 |
| notification-service| 8085 |
| Prometheus        | 9090 |
| Grafana           | 3000 |

## Key Conventions

- **API responses** are wrapped in `ApiResponse<T>` with `success`, `message`, and `data` fields.
- **Roles**: `ADMIN`, `TEACHER`, `STUDENT` — enforced via `@PreAuthorize("hasRole(...)")`.
- **Pagination**: list endpoints use Spring Data `Pageable` with `@PageableDefault(size=20)`.
- **Inter-service calls**: use OpenFeign clients (where present); services pass `studentId`/`moduleId` as foreign-key references rather than embedding full objects.

## Testing

- **Unit tests**: Mockito-based, under `src/test/java/.../service/` and `.../controller/`.
- **Integration tests**: use `@Testcontainers` + `PostgreSQLContainer` with `@ActiveProfiles("test")`. Eureka and Spring Cloud Config are disabled via `DynamicPropertySource` for isolation. Tests are ordered with `@Order` and share state (e.g., JWT token captured in step 2 used in subsequent steps).
- Test config lives in `src/test/resources/application-test.yml`.

## Local Development Setup

```bash
cp .env.example .env  # JWT_SECRET, DB passwords, Grafana password

# Start only infrastructure
make up-infra

# Then run services individually
cd auth-service && mvn spring-boot:run
cd student-service && mvn spring-boot:run
# ... etc, start api-gateway last
```

Environment variables with defaults (from `application.yml`):
- `DB_HOST` (localhost), `DB_PORT` (5432), `DB_NAME`, `DB_USERNAME`/`DB_PASSWORD` (postgres)
- `EUREKA_HOST` (localhost), `EUREKA_PORT` (8761)
- `JWT_SECRET` — must match across all services
