# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Convention:** This file must be updated as part of every code change. Never leave CLAUDE.md stale after a codebase modification.

## Build & Run

Spring Boot 3.5.7 / Java 21 / Gradle project.

> **Note:** `gradle/wrapper/gradle-wrapper.jar` is not committed. Use system `gradle` directly.

- Build: `gradle build`
- Run: `gradle bootRun` — listens on port **8081**
- Run all tests: `gradle test`
- Run single test class: `gradle test --tests "com.etd.account_management.AccountManagementApplicationTests"`
- Run single test method: `gradle test --tests "FQCN.methodName"`
- Clean: `gradle clean`

Package: `com.etd.account_management` (underscore — the hyphenated form is invalid in Java).

---

## Microservice Role

account-management is the **employee CRUD service** in the ETD system. It manages employees, grades and grade history. It does **not** handle login or token issuance — that is auth-service's responsibility.

| Service | Port | Responsibility |
|---|---|---|
| auth-service | 8080 | Login, refresh, logout, blacklist check — issues JWT tokens |
| **account-management** | **8081** | Employee / grade / grade-history CRUD + owner of the shared `account_management` MySQL database |
| travel-planner | 8082 | Travel request lifecycle |

---

## Database

- **MySQL 8** database `account_management`, reached over TCP on port **3306**. account-management is the schema owner; auth-service connects to the **same** database directly. Configured via `spring-boot-starter-data-jpa` + `runtimeOnly 'com.mysql:mysql-connector-j'` (driver `com.mysql.cj.jdbc.Driver`, dialect `org.hibernate.dialect.MySQLDialect`).
- Connection: JDBC URL `jdbc:mysql://localhost:3306/account_management`, username `root`.
- JPA/Hibernate auto-creates/updates tables via `spring.jpa.hibernate.ddl-auto=update` — no SQL script is executed by the app.
- `ddl-auto=update` adds columns and tables but **never drops** them. Drop stale tables manually in MySQL if needed.

> **account-management must start first** — it creates the shared `employees` / `grades` schema and seeds the default users. auth-service then connects to the same `account_management` database (with `ddl-auto=update`) and creates the `refresh_tokens` and `token_blacklist` tables it owns.

### Fresh start (wipe DB)

Stop all services, then DROP and re-CREATE the `account_management` MySQL database (or TRUNCATE its tables), then **restart account-management first**, then the others:
```sql
DROP DATABASE account_management;
CREATE DATABASE account_management;
```

> **Note:** `refresh_tokens` and `token_blacklist` tables also live in this database — they are owned by auth-service and are not touched by account-management's schema management.

---

## Architecture

Standard Spring Boot layered REST API. All `/api/*` controllers annotated `@CrossOrigin` (open CORS).

```
controller → service.interfaces / service.classes → dao (JpaRepository) → entity
                       ↘ mapper ↗
                       ↘ dto   ↗
                       ↘ client (Feign to auth-service) ↗
```

### Package layout

```
com.etd.account_management
├── client/          AuthServiceClient  (Feign client to auth-service for blacklist check)
├── config/          JwtAuthFilter, SecurityConfig, DataInitializer
├── constant/        AppConstant  (message-key and string constants)
├── controller/      EmployeeController, GradeController, GradeHistoryController
├── dao/             EmployeeRepo, GradeRepo, GradeHistoryRepo
├── dto/             EmployeeRequestDTO, EmployeeResponseDTO, GradeResponseDTO,
│                    GradeHistoryResponseDTO, ErrorDTO
├── entity/          Employee, Grade, GradeHistory
├── exception/       BadRequestException, NotFoundException,
│                    GradeUpdateRuleViolationException, GlobalExceptionHandler
├── mapper/          EmployeeMapper, GradeMapper, GradeHistoryMapper
├── service/
│   ├── interfaces/  EmployeeService, GradeService, GradeHistoryService
│   └── classes/     EmployeeServiceImpl, GradeServiceImpl, GradeHistoryServiceImpl,
│                    MyUserDetailService
└── util/            JWTUtil, CommonUtil
```

---

## Domain Model

Three JPA entities in `entity/`:

- **`Employee`** — core entity. Table: `employees`. Fields: `employeeId` (sequence, 6-digit), `firstName`, `lastName`, `phoneNumber`, `emailAddress`, `role`, `password` (BCrypt hash), `accessGranted`, `currentGrade` (@ManyToOne → Grade), `gradeHistories` (@OneToMany → GradeHistory).
- **`Grade`** — reference/lookup table (`grades`). Seeded with Grade-1, Grade-2, Grade-3 by `DataInitializer`. Lower `id` = higher seniority (Grade-1 is most senior).
- **`GradeHistory`** — append-only audit log. Table: `grades_history`. Fields: `id`, `assignedOn`, `employee` (@ManyToOne), `grade` (@ManyToOne). A new row is written on every grade assignment or change.

> **Note:** `refresh_tokens` and `token_blacklist` tables exist in the shared DB but are owned exclusively by auth-service. account-management has no entities or repos for them.

---

## Startup Seeding — DataInitializer

`config/DataInitializer` implements `ApplicationRunner` and runs on every startup. Seeds are idempotent.

**Step 1 — Grades** (if `grades` table is empty):

| id | name | Seniority |
|---|---|---|
| 1 | Grade-1 | Most senior |
| 2 | Grade-2 | Mid |
| 3 | Grade-3 | Most junior |

**Step 2 — Default employees** (by email, if not already present):

| Role | Email | Password | Grade |
|---|---|---|---|
| HR | `admin.hr@cognizant.com` | `Admin@123` | Grade-1 |
| TravelDeskExe | `desk.exec@cognizant.com` | `Exec@123` | Grade-1 |
| Employee | `john.employee@cognizant.com` | `Employee@123` | Grade-3 |

All passwords are BCrypt-hashed (strength 12) before saving.

> **Why DataInitializer lives here and not in auth-service:** Employees require a `current_grade_id` FK, so grades must be seeded first. auth-service's Employee entity is read-only with no grade relationship — it cannot insert employees correctly. account-management is the system of record for employee data.

---

## Business Rules

### Employee ID
- Auto-generated from `@SequenceGenerator` (`employee_seq`, `initialValue = 100000`, `allocationSize = 1`) — always 6 digits.
- `CommonUtil.validateEmployeeId(Long id)` enforces the 100000–999999 range in `getById`, `updateEmployee`, `deleteEmployee`. Violation → `BadRequestException`.

### Email
- Must end with `@cognizant.com`. Validated by `CommonUtil.validateEmail` / `CommonUtil.validateEmailAddress`, called on both create and update paths.

### Grade-change rules
Enforced in `EmployeeServiceImpl.updateEmployee` — all rules must hold:

1. Grade can only go **upward** — lower `Grade.id` = higher seniority. Setting a higher ID = downgrade → `GradeUpdateRuleViolationException`.
2. New joiner freeze — grade cannot change within **2 years** of the earliest `GradeHistory.assignedOn` (joining date).
3. After freeze — grade can change at most **once per year** vs. the latest `GradeHistory.assignedOn`.
4. On `createEmployee`, if `role == "TravelDeskExe"` the grade is force-set to Grade-1 (`id = 1L`) regardless of input.

Violations → `GradeUpdateRuleViolationException`. Missing resources → `BadRequestException` / `NotFoundException`. All mapped to `ErrorDTO` by `GlobalExceptionHandler`.

### Password
- HR provides plain-text password in the `POST /api/employees` body.
- `EmployeeServiceImpl.createEmployee` BCrypt-hashes it (strength 12) before saving.
- `EmployeeRequestDTO` has `@ToString(exclude = "password")` — passwords are never logged.
- `PUT /api/employees/{id}` does **not** change the password.

---

## Authentication & Authorization

> **Login, refresh and logout are handled by auth-service (port 8080).** This service only **validates** incoming JWT tokens — it does not issue them.

### How token validation works

`JwtAuthFilter` runs on every incoming request:
1. Extracts `Bearer <token>` from `Authorization` header
2. Parses `username` (subject claim) — catches exception silently if invalid/expired
3. **Calls `AuthServiceClient.isBlacklisted(token)`** via Feign → `GET http://localhost:8080/auth/blacklist/check?token=...`; if `true` → 403
4. Loads `UserDetails` via `MyUserDetailService` (queries `EmployeeRepo` by email)
5. Calls `JWTUtil.validateToken(token, userDetails)` — checks username match + not expired
6. Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`

Fail-open: if auth-service is unreachable at step 3, the blacklist check is skipped (warning logged) and the request proceeds with local signature + expiry validation only.

### AuthServiceClient (Feign)

`client/AuthServiceClient` calls `GET ${auth.service.base_url}auth/blacklist/check?token=...` (`http://localhost:8080/auth/blacklist/check`). Returns `Boolean`. Requires `@EnableFeignClients` on `AccountManagementApplication` and Spring Cloud dependency in `build.gradle`.

### MyUserDetailService

Loads `Employee` by email → builds Spring `UserDetails`:
- `username` → `emailAddress`
- `password` → BCrypt hash (used only by Spring Security internals)
- `authorities` → `[role]` (single authority, no `ROLE_` prefix)
- `enabled` → `accessGranted`

### Authorization rules (SecurityConfig)

| Path | Rule |
|---|---|
| `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` | `permitAll` |
| `GET /api/**` | any authenticated user |
| `POST /api/**`, `PUT /api/**`, `DELETE /api/**` | `hasAuthority("HR")` only |
| everything else | authenticated |

`SecurityConfig` provides a `PasswordEncoder` bean (`BCryptPasswordEncoder` strength 12) used by `DataInitializer`. `AuthenticationProvider` and `AuthenticationManager` beans were removed when login moved to auth-service.

### Roles

Raw strings, no `ROLE_` prefix. Constants in `AppConstant`: `ROLE_HR = "HR"`, `ROLE_EMPLOYEE = "Employee"`, `ROLE_TRAVEL_DESK_EXE = "TravelDeskExe"`.

### JWTUtil — validation only

| Method | Description |
|---|---|
| `extractUsername(token)` | Parses `sub` claim — throws if token invalid or expired |
| `validateToken(token, userDetails)` | Returns true if username matches and token not expired |

`generateToken` and `extractExpiration` were removed — token issuance belongs to auth-service. `isTokenExpired` is private, used only by `validateToken`.

### Shared JWT secret

`application.properties` must contain `jwt.secret=<value>` matching auth-service. Key is derived from the secret string via UTF-8 bytes (HMAC-SHA256). Changing without updating auth-service invalidates all active tokens.

---

## API Endpoints

> Auth endpoints (`/login`, `/auth/refresh`, `/auth/logout`) are in **auth-service (port 8080)**.

### Employees

| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/api/employees` | Authenticated | List all employees |
| GET | `/api/employees/{id}` | Authenticated | Single employee by 6-digit ID |
| POST | `/api/employees` | HR only | Create employee (password BCrypt-hashed on save) |
| PUT | `/api/employees/{id}` | HR only | Update employee grade / details |
| DELETE | `/api/employees/{id}` | HR only | Delete employee (cascades grade history) |

### Grades

| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/api/grades` | Authenticated | List all grades |

### Grade History

| Method | URL | Auth | Description |
|---|---|---|---|
| GET | `/api/gradeHistory` | Authenticated | All grade history records |
| GET | `/api/gradeHistory/{employeeId}` | Authenticated | Grade history for a specific employee |

---

## Error Messages

Strings in `messages.properties`. Keys are constants in `AppConstant`. Always look up via `messageSource.getMessage(CONSTANT, null, Locale.ENGLISH)` — never use string literals.

| Constant | Key | Message |
|---|---|---|
| `ERROR_GRADE_CHANGE_NEW_JOINER` | `error.grade.change.new.joiner` | Grade of new joiners can only be changed after they complete 2 years |
| `ERROR_GRADE_CHANGE_ONCE_YEAR` | `error.grade.change.once.year` | An employee grade can only be changed once in a year |
| `ERROR_GRADE_CHANGE_UPWARDS_ONLY` | `error.grade.change.upwards.only` | Employee grade can only be upgraded, not downgraded |
| `ERROR_EMPLOYEE_NOT_FOUND` | `error.employee.not.found` | Employee not found |
| `ERROR_EMPLOYEE_INVALID_EMAIL` | `error.employee.invalid.email` | Invalid email address |
| `ERROR_EMPLOYEE_INVALID_ID` | `error.employee.invalid.id` | Employee ID must be a valid 6 digit number |
| `ERROR_GRADE_NOT_FOUND` | `error.grade.not.found` | Grade not found |

---

## Mappers

Plain `@Component` classes in `mapper/` — no MapStruct. Entities are never returned from controllers; always mapped to a `*ResponseDTO` first.

- `EmployeeMapper.mapEmployeeToEmployeeResponseDTO` — derives `gradeAssignedOn` by sorting `gradeHistories` descending and taking the latest entry. Depends on `GradeHistory.assignedOn` being non-null.
- `EmployeeMapper.mapEmployeeRequestDTOToEmployee` — does **not** set `password`; the service BCrypt-hashes it separately after mapping.

---

## Conventions

- **Constructor injection only** — no `@Autowired` field injection.
- **Service interfaces** in `service/interfaces/`, impls in `service/classes/`.
- **Lombok** on all entities and DTOs: `@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor`. Use `@ToString(exclude = "password")` on any DTO carrying credentials.
- **SLF4J logger** per class — `logger.info(...)` on entry, `logger.warn(...)` immediately before throwing.
- **`@Transactional`** on service impl methods that write to the DB.
- **`@Modifying(clearAutomatically = true) @Query`** for bulk DELETEs that run alongside INSERTs — never use derived delete methods in those cases (Hibernate batching causes unique constraint violations).
- **CLAUDE.md must be updated** on every code change.
- **Springdoc OpenAPI** — Swagger UI at `/swagger-ui.html`, spec at `/v3/api-docs`.
