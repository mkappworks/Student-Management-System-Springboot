# Student Management System — Microservices

A production-ready **Spring Boot 3.5 / Java 21** microservices application for managing students, teachers, modules, enrollments, and grades. Includes a **React Router v7** frontend. Built with Spring Cloud, PostgreSQL, JWT security, Docker Compose, Prometheus, and Grafana.

---

## Architecture

```
  ┌──────────────────────┐
  │  Web Frontend :3001  │  (React Router v7 / Vite / Tailwind)
  └──────────┬───────────┘
             │ HTTP
  ┌──────────▼───────────┐       ┌────────────────┐
  │     API Gateway      │       │  Eureka Server │
  │        :8080         │◄─────►│     :8761      │
  └──────────┬───────────┘       └────────────────┘
             │  JWT validated + header injected
  ┌──────────▼─────────────────────────────────────┐
  │                 Microservices                   │
  │  Auth      :8090  │  Student   :8081            │
  │  Teacher   :8082  │  Module    :8086            │
  │  Grade     :8083  │  Enrollment :8084           │
  │  Notification :8085                             │
  └─────────────────────────────────────────────────┘
             │
  ┌──────────▼──────────────────┐
  │         Monitoring           │
  │  Prometheus :9090            │
  │  Grafana    :3000            │
  └──────────────────────────────┘
```

### Services

| Service              | Port | Description                                  |
|----------------------|------|----------------------------------------------|
| web                  | 3001 | React Router v7 frontend (Docker) / 5173 dev |
| service-registry     | 8761 | Eureka service discovery                     |
| api-gateway          | 8080 | JWT-validating API gateway (Spring Cloud GW) |
| auth-service         | 8090 | User registration, login, JWT issuance       |
| student-service      | 8081 | Student CRUD, search, status management      |
| teacher-service      | 8082 | Teacher CRUD, department management          |
| module-service       | 8086 | Module (course) management with capacity     |
| grade-service        | 8083 | Grade recording with letter-grade calculation|
| enrollment-service   | 8084 | Student-module enrollment management         |
| notification-service | 8085 | In-app / email notifications                 |

---

## Security Architecture

Security is split across two layers, each with a distinct `SecurityConfig`:

### API Gateway — Traffic Gating
- **Framework:** WebFlux (`ServerHttpSecurity`) — the gateway is a reactive application
- **Role:** Single entry point for all inbound traffic. Decides whether a request is allowed to reach any downstream service at all
- Open paths: `/api/v1/auth/**`, `/actuator/**`, `/eureka/**` — all others require authentication
- JWT validation is performed by `JwtAuthenticationFilter` (a `GlobalFilter`), which validates the token and injects `X-User-Id` and `X-User-Role` headers into the forwarded request
- Has no knowledge of users or passwords — it only validates tokens

### Auth Service — Authentication Mechanism
- **Framework:** Servlet (`HttpSecurity`) — auth-service is a standard blocking Spring MVC app
- **Role:** Owns the authentication mechanism itself — validates credentials and issues JWTs
- Wires `UserDetailsService` (loads users from the DB), `DaoAuthenticationProvider` (BCrypt password checking), and exposes `AuthenticationManager` so `AuthService` can call `authenticate()` on login
- Session policy is `STATELESS` — this service issues JWTs, not sessions

```
Client
  │
  ▼
API Gateway SecurityConfig  ←  "Is this request authenticated? Block or forward."
  │
  ▼
Auth Service SecurityConfig  ←  "Load the user, check the password, issue a JWT."
```

| | API Gateway | Auth Service |
|---|---|---|
| Framework | WebFlux (reactive) | Servlet (blocking) |
| Purpose | Traffic gating | Credential validation & JWT issuance |
| Knows about users? | No | Yes (via `UserRepository`) |
| JWT role | Validates tokens | Creates tokens |

---

## Technology Stack

### Backend
- **Java 21** with virtual threads
- **Spring Boot 3.5.11** — Web, Data JPA, Security, Validation, Actuator
- **Spring Cloud 2025.0.0** — Gateway, Eureka, OpenFeign
- **PostgreSQL 16** — separate database per service
- **Flyway** — database migrations
- **JWT (JJWT 0.12.3)** — stateless authentication
- **Docker Compose** — multi-container orchestration
- **Prometheus + Grafana** — metrics and dashboards
- **Testcontainers** — integration tests with real PostgreSQL
- **Lombok** — boilerplate reduction

### Frontend
- **React 19** + **React Router v7** — file-based routing with SSR support
- **TypeScript 5** — type safety
- **Vite 7** — build tooling
- **Tailwind CSS v4** — utility-first styling
- **shadcn/ui** + **Base UI** — component library
- **Bun** — package manager and runtime

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker Desktop / Docker Engine 24+
- Bun (for frontend development)
- Make (optional, for convenience commands)

---

## Quick Start

### 1 — Clone and configure

```bash
git clone <repo-url>
cd student-management-system
cp .env.example .env          # edit JWT_SECRET and passwords in production
```

### 2 — Option A: Full Docker deployment

```bash
# Build all JARs
mvn clean package -DskipTests

# Launch everything
docker compose up -d

# Watch startup
docker compose logs -f
```

All 19 containers (7 databases + 9 services + web frontend + Prometheus + Grafana) will start. First run takes ~3–5 minutes.

### 2 — Option B: Local development (databases in Docker, services local)

```bash
# Start databases and Eureka only
docker compose up -d postgres-student postgres-teacher postgres-grade \
  postgres-enrollment postgres-notification postgres-module postgres-auth \
  service-registry

# Run services locally (one terminal each)
cd services/auth-service       && mvn spring-boot:run
cd services/student-service    && mvn spring-boot:run
cd services/teacher-service    && mvn spring-boot:run
cd services/module-service     && mvn spring-boot:run
cd services/grade-service      && mvn spring-boot:run
cd services/enrollment-service && mvn spring-boot:run
cd services/notification-service && mvn spring-boot:run
cd services/api-gateway        && mvn spring-boot:run  # start last

# Run frontend dev server (separate terminal)
make web-dev           # http://localhost:5173
```

---

## API Usage

### Authentication

**Register a user:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "System",
    "lastName": "Admin",
    "email": "admin@sms.com",
    "password": "Admin@1234",
    "role": "ADMIN"
  }'
```

**Login and get JWT:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@sms.com","password":"Admin@1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "Token: $TOKEN"
```

### Student Operations

```bash
# Create student
curl -X POST http://localhost:8080/api/v1/students \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Johnson",
    "email": "alice@student.com",
    "programme": "Computer Science",
    "yearOfStudy": 1,
    "gender": "FEMALE"
  }'

# List students (paginated)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/students?page=0&size=10"

# Search students
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/students/search?query=Alice"

# Update status
curl -X PATCH "http://localhost:8080/api/v1/students/1/status?status=INACTIVE" \
  -H "Authorization: Bearer $TOKEN"
```

### Module Operations

```bash
# Create module
curl -X POST http://localhost:8080/api/v1/modules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CS101",
    "name": "Introduction to Computer Science",
    "description": "Core CS fundamentals",
    "credits": 3,
    "maxStudents": 30,
    "semester": "Fall",
    "academicYear": "2024/2025",
    "location": "Room A101"
  }'

# List modules
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/modules
```

### Enrollment Operations

```bash
# Enroll a student in a module
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "moduleId": "<uuid>",
    "academicYear": "2024/2025",
    "semester": 1
  }'

# Get student enrollments
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/enrollments/student/1

# Drop enrollment
curl -X PATCH "http://localhost:8080/api/v1/enrollments/1/drop?reason=Medical" \
  -H "Authorization: Bearer $TOKEN"
```

### Grade Operations

```bash
# Record a grade
curl -X POST http://localhost:8080/api/v1/grades \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "moduleId": 1,
    "teacherId": 1,
    "score": 85.5,
    "maxScore": 100,
    "assessmentType": "MIDTERM",
    "semester": 1,
    "academicYear": "2024/2025"
  }'

# Get student grades
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/grades/student/1
```

---

## Roles & Permissions

| Role    | Permissions                                                |
|---------|------------------------------------------------------------|
| ADMIN   | Full access to all endpoints                               |
| TEACHER | Read/write grades, read students and modules               |
| STUDENT | Read own data, view modules and own enrollments            |

---

## Monitoring

| Service    | URL                           | Credentials        |
|------------|-------------------------------|--------------------|
| Web UI     | http://localhost:3001         | None               |
| API        | http://localhost:8080         | JWT Bearer token   |
| Eureka     | http://localhost:8761         | None               |
| Prometheus | http://localhost:9090         | None               |
| Grafana    | http://localhost:3000         | admin / admin123   |

Grafana pre-provisions a **SMS - Microservices Overview** dashboard with:
- HTTP request rate per service
- Error rate (4xx / 5xx)
- JVM heap memory usage
- HikariCP active database connections
- P99 response time

---

## Running Tests

```bash
# Unit tests (all services)
mvn test

# Single service
cd student-service && mvn test

# With coverage report
mvn test && open target/site/jacoco/index.html
```

---

## Environment Variables

All services accept these environment variables (with defaults for local dev):

| Variable    | Default                            | Description              |
|-------------|-------------------------------------|--------------------------|
| DB_HOST     | localhost                           | PostgreSQL hostname      |
| DB_PORT     | 5432                                | PostgreSQL port          |
| DB_NAME     | \<service\>_db                      | Database name            |
| DB_USERNAME | postgres                            | Database user            |
| DB_PASSWORD | postgres                            | Database password        |
| EUREKA_HOST | localhost                           | Eureka server hostname   |
| EUREKA_PORT | 8761                                | Eureka server port       |
| JWT_SECRET  | 404E635266...                       | HS256 secret key (hex)   |

---

## Letter Grade Scale

| Score    | Grade |
|----------|-------|
| ≥ 90%    | A+    |
| ≥ 80%    | A     |
| ≥ 70%    | B     |
| ≥ 60%    | C     |
| ≥ 50%    | D     |
| < 50%    | F     |

---

## Project Structure

```
student-management-system/
├── docker-compose.yml          # Full stack orchestration
├── Makefile                    # Developer convenience commands
├── .env.example                # Environment variable template
├── services/                   # All Spring Boot microservices
│   ├── pom.xml                 # Parent POM
│   ├── service-registry/       # Eureka Server
│   ├── api-gateway/            # Spring Cloud Gateway + JWT filter
│   ├── auth-service/           # Authentication & user management
│   ├── student-service/        # Student domain
│   ├── teacher-service/        # Teacher domain
│   ├── module-service/         # Module (course) domain
│   ├── grade-service/          # Grade domain
│   ├── enrollment-service/     # Enrollment domain
│   └── notification-service/   # Notification domain
├── web/                        # React Router v7 frontend
│   ├── app/                    # Routes, components, styles
│   ├── public/                 # Static assets
│   ├── Dockerfile
│   ├── package.json
│   └── vite.config.ts
└── monitoring/
    ├── prometheus/
    │   └── prometheus.yml
    └── grafana/
        └── provisioning/
```

---

## Production Checklist

- [ ] Rotate `JWT_SECRET` to a strong random value
- [ ] Change all database passwords
- [ ] Change Grafana admin password
- [ ] Enable HTTPS (TLS termination at load balancer or gateway)
- [ ] Set `spring.jpa.show-sql=false` (already default)
- [ ] Configure log aggregation (ELK / Loki)
- [ ] Add circuit breakers (Resilience4j)
- [ ] Set up distributed tracing (Zipkin / Jaeger)
- [ ] Configure rate limiting in API Gateway
- [ ] Set up database connection pool tuning per load profile

---

## Troubleshooting

**Services not registering with Eureka**
```bash
docker compose logs service-registry
# Wait 30–60s after service-registry is healthy before checking
```

**Database connection failures**
```bash
docker compose exec postgres-student pg_isready -U postgres -d student_db
```

**JWT errors**
```bash
# Ensure JWT_SECRET is identical across all services
grep JWT_SECRET .env
```

**Full reset**
```bash
docker compose down -v && docker compose up -d
```
