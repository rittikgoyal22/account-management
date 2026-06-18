# Account Management Service

Part of the **Employee Travel Desk (ETD)** system — a Cognizant FSE Business Aligned Project.

This microservice is the **HR and Employee management service** of the ETD platform. It manages employee records, grades and grade history. It also hosts the **shared H2 database** used by auth-service and other ETD services. Authentication (login / token refresh / logout) is handled by **auth-service** on port 8080.

---

## What this service does

| Responsibility | Details |
|---|---|
| **Employee management** | HR can add, view, update grade and delete employees |
| **Grade tracking** | Every grade assignment or change is recorded as an immutable audit history entry |
| **H2 TCP server host** | Starts H2 TCP server on port 9092 — auth-service connects to it for the shared database |
| **Token validation** | Validates JWT tokens on every request; calls auth-service to check if token was blacklisted |
| **Role-based access** | HR has full write access; all authenticated users can read |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.7 |
| Security | Spring Security + JWT validation (JJWT 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Database | H2 file-mode (dev) / MySQL (prod, commented out) |
| Build tool | Gradle |
| HTTP client | Spring Cloud OpenFeign (calls auth-service for blacklist check) |
| Utilities | Lombok, Springdoc OpenAPI |

---

## Prerequisites

- Java 21
- Gradle (system install — wrapper jar is not committed)
- No other service needed — **account-management starts first** and all others depend on it

---

## Service Startup Order

```
1. account-management (port 8081)  ← start first — starts H2 TCP server on port 9092
2. auth-service (port 8080)        ← connects to H2 TCP server
3. travel-planner (port 8082)      ← connects to H2 TCP server
```

---

## Running the application

```bash
# Build
gradle build

# Run — starts on port 8081 and H2 TCP server on port 9092
gradle bootRun

# Run tests
gradle test

# Clean build output
gradle clean
```

On first startup **DataInitializer** automatically:
1. Seeds grades: Grade-1 (most senior), Grade-2, Grade-3 (most junior)
2. Creates three default users (see Default Credentials below)

---

## Default Credentials

These accounts are created on first startup. Use them to log in via **auth-service** (`POST http://localhost:8080/login`).

| Role | Email | Password | Starting Grade |
|---|---|---|---|
| HR | `admin.hr@cognizant.com` | `Admin@123` | Grade-1 |
| TravelDeskExe | `desk.exec@cognizant.com` | `Exec@123` | Grade-1 |
| Employee | `john.employee@cognizant.com` | `Employee@123` | Grade-3 |

---

## Authentication

**Login, token refresh and logout are handled by auth-service (port 8080) — not this service.**

```
POST http://localhost:8080/login          → get access token + refresh token
POST http://localhost:8080/auth/refresh   → rotate tokens
POST http://localhost:8080/auth/logout    → invalidate session
```

All requests to this service must include the access token issued by auth-service:
```
Authorization: Bearer <accessToken>
```

On every incoming request, this service calls auth-service via Feign to check if the token was blacklisted:
```
GET http://localhost:8080/auth/blacklist/check?token=<accessToken>
```
If blacklisted → `403`. If auth-service is unreachable → fail-open (request proceeds with signature + expiry check only).

---

## Database

### H2 (default — no setup needed)

account-management opens the database file directly (embedded mode) and also starts an **H2 TCP server on port 9092**. auth-service connects to this TCP server.

| Setting | Value |
|---|---|
| Console URL | `http://localhost:8081/h2-console` |
| JDBC URL | `jdbc:h2:file:~/data/account_management` |
| Username | `sa` |
| Password | *(blank)* |

> `refresh_tokens` and `token_blacklist` tables are also in this DB file — owned and managed by auth-service.

### Fresh start (wipe all data)

Stop **all services**, delete these two files, then restart **account-management first**, then auth-service and travel-planner:
```
~/data/account_management.mv.db
~/data/account_management.trace.db
```

### Switch to MySQL

1. Uncomment `runtimeOnly 'com.mysql:mysql-connector-j'` in `build.gradle`
2. Uncomment the MySQL `spring.datasource.*` lines in `application.properties`
3. Comment out the H2 datasource lines
4. Run `account_management.sql` to create the schema

---

## API Reference

Base URL: `http://localhost:8081`

All endpoints require `Authorization: Bearer <token>` unless stated otherwise. Obtain a token from `POST http://localhost:8080/login`.

### Employees

#### Get all employees
```
GET /api/employees
```
**Response `200`:** Array of employee objects.

#### Get employee by ID
```
GET /api/employees/{id}
```
`id` must be a 6-digit number (100000–999999).

**Response `200`:**
```json
{
  "employeeId": 100000,
  "firstName": "Admin",
  "lastName": "HR",
  "phoneNumber": "9000000001",
  "emailAddress": "admin.hr@cognizant.com",
  "role": "HR",
  "gradeName": "Grade-1",
  "gradeAssignedOn": "2026-06-06T10:00:00",
  "accessGranted": true
}
```

#### Create employee *(HR only)*
```
POST /api/employees
```
**Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": "9876543210",
  "emailAddress": "jane.smith@cognizant.com",
  "role": "Employee",
  "password": "Jane@123",
  "currentGradeId": 3,
  "accessGranted": true
}
```

> `role` values: `Employee`, `HR`, `TravelDeskExe`
> For `TravelDeskExe` — grade is always force-set to Grade-1 regardless of `currentGradeId`.
> Password is BCrypt-hashed before storing — never saved as plain text.

**Response `200`:** Created employee object.

#### Update employee *(HR only)*
```
PUT /api/employees/{id}
```
Used to upgrade an employee's grade or update details. Password field is ignored — passwords cannot be changed via this endpoint.

**Response `200`:** Updated employee object.

#### Delete employee *(HR only)*
```
DELETE /api/employees/{id}
```
Deletes the employee and all their grade history records.

**Response `204 No Content`**

---

### Grades

#### Get all grades
```
GET /api/grades
```
**Response `200`:**
```json
[
  { "id": 1, "name": "Grade-1" },
  { "id": 2, "name": "Grade-2" },
  { "id": 3, "name": "Grade-3" }
]
```

> Grade-1 (id=1) is the **most senior**. Grade-3 (id=3) is the most junior.

---

### Grade History

#### Get all grade history
```
GET /api/gradeHistory
```

#### Get grade history for an employee
```
GET /api/gradeHistory/{employeeId}
```
**Response `200`:** Array of `{ id, employeeId, gradeName, assignedOn }` records.

---

## Role Permissions

| Endpoint | HR | Employee | TravelDeskExe |
|---|:---:|:---:|:---:|
| `GET /api/employees` | ✅ | ✅ | ✅ |
| `GET /api/employees/{id}` | ✅ | ✅ | ✅ |
| `GET /api/grades` | ✅ | ✅ | ✅ |
| `GET /api/gradeHistory` | ✅ | ✅ | ✅ |
| `GET /api/gradeHistory/{id}` | ✅ | ✅ | ✅ |
| `POST /api/employees` | ✅ | ❌ | ❌ |
| `PUT /api/employees/{id}` | ✅ | ❌ | ❌ |
| `DELETE /api/employees/{id}` | ✅ | ❌ | ❌ |

> Auth endpoints (`/login`, `/auth/refresh`, `/auth/logout`) are on **auth-service port 8080** — available to all roles.

---

## Business Rules

### Employee ID
- Auto-generated, always 6 digits (100000–999999)
- Out-of-range ID → `400 Employee ID must be a valid 6 digit number`

### Email
- Must end with `@cognizant.com` — enforced on both create and update

### Grade changes
1. **Upward only** — lower `id` = higher seniority. Attempting to set a higher ID (downgrade) → `400`
2. **2-year new joiner freeze** — no grade change within 2 years of the joining date (earliest grade history entry)
3. **Once per year** — after the freeze, grade can only change once per 12-month period
4. **TravelDeskExec default** — always assigned Grade-1 on creation, regardless of input

All grade-rule violations → `400 BAD_REQUEST`.

### Password
- Stored as BCrypt hash (strength 12) — never plain text
- Provided by HR when creating an employee
- Cannot be changed through the update endpoint

---

## Error Response Format

```json
{
  "message": "Human-readable description",
  "fieldName": "Field that caused the error (nullable)",
  "status": "HTTP status name"
}
```

| HTTP Status | When |
|---|---|
| `400 BAD_REQUEST` | Validation failure, grade rule violation |
| `404 NOT_FOUND` | Employee or grade not found |
| `403 FORBIDDEN` | Missing / expired / blacklisted token, insufficient role |

---

## Swagger UI

- **UI:** `http://localhost:8081/swagger-ui.html`
- **JSON spec:** `http://localhost:8081/v3/api-docs`

---

## Project Structure

```
src/main/java/com/etd/account_management/
├── client/           AuthServiceClient  (Feign — blacklist check on every request)
├── config/           JwtAuthFilter, SecurityConfig, DataInitializer, H2ServerConfig
├── constant/         AppConstant (message keys + string constants)
├── controller/       EmployeeController, GradeController, GradeHistoryController
├── dao/              EmployeeRepo, GradeRepo, GradeHistoryRepo
├── dto/              EmployeeRequestDTO, EmployeeResponseDTO, GradeResponseDTO,
│                     GradeHistoryResponseDTO, ErrorDTO
├── entity/           Employee, Grade, GradeHistory
├── exception/        BadRequestException, NotFoundException,
│                     GradeUpdateRuleViolationException, GlobalExceptionHandler
├── mapper/           EmployeeMapper, GradeMapper, GradeHistoryMapper
├── service/
│   ├── interfaces/   EmployeeService, GradeService, GradeHistoryService
│   └── classes/      EmployeeServiceImpl, GradeServiceImpl, GradeHistoryServiceImpl,
│                     MyUserDetailService
└── util/             JWTUtil (token validation), CommonUtil (input validation)
```

---

## Related Services

| Service | Port | Responsibility |
|---|---|---|
| **auth-service** | **8080** | Login, token refresh, logout, blacklist check |
| **account-management** *(this service)* | **8081** | Employee / grade / grade-history CRUD + H2 TCP server |
| travel-planner | 8082 | Travel request lifecycle, budget calculation |
| reservation-management | — | Flight / hotel / cab reservation upload and tracking |
| reimbursement-management | — | Expense claim submission and processing |

Each service validates JWT tokens locally (signature + expiry). For blacklist enforcement after logout, account-management and travel-planner call auth-service's `GET /auth/blacklist/check` endpoint via Feign on every incoming request.
