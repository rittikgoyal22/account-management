# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Convention:** This file must be updated as part of every code change. Never leave CLAUDE.md stale after a codebase modification.

## Build & Run

This is a Spring Boot 3.5.7 / Java 21 / Gradle project.

> **Note:** The `gradle/wrapper/gradle-wrapper.jar` is not committed. Use the system `gradle` command directly instead of the wrapper scripts.

- Build: `gradle build`
- Run app: `gradle bootRun` — listens on port **8081**
- Run all tests: `gradle test`
- Run a single test class: `gradle test --tests "com.etd.account_management.AccountManagementApplicationTests"`
- Run a single test method: `gradle test --tests "FQCN.methodName"`
- Clean: `gradle clean`

The package is `com.etd.account_management` (underscore — the original hyphenated form was invalid; see [HELP.md](HELP.md)).

---

## Database

- Default datasource is **H2 file-mode** at `~/data/account_management` with `AUTO_SERVER=TRUE` so multiple processes (app + console) can connect to the same file.
- H2 console is at `/h2-console` (username `sa`, blank password). X-Frame-Options is `sameOrigin` so iframes render correctly.
- A MySQL configuration is commented out in `application.properties`; switching back requires uncommenting both the `mysql-connector-j` dependency in `build.gradle` and the MySQL `spring.datasource.*` lines.
- `account_management.sql` is a **MySQL reference schema only** — not executed by the app, not H2-compatible. JPA/Hibernate auto-creates all tables on startup via `ddl-auto=update`.
- `ddl-auto=update` adds new columns and tables but **never drops** them. If you delete an entity class, drop its table manually via H2 console or do a fresh start.

### Fresh start (wipe DB)

Delete both files then restart — `DataInitializer` seeds everything automatically:
```
~/data/account_management.mv.db
~/data/account_management.trace.db
```

---

## Architecture

Standard Spring Boot layered REST API. All `/api/*` endpoints annotated `@CrossOrigin` (open CORS).

```
controller → service.interfaces / service.classes → dao (JpaRepository) → entity
                       ↘ mapper ↗
                       ↘ dto   ↗
```

### Package layout

```
com.etd.account_management
├── config/          JwtAuthFilter, SecurityConfig, DataInitializer
├── constant/        AppConstant  (all message-key and string constants)
├── controller/      AuthController, EmployeeController, GradeController, GradeHistoryController
├── dao/             EmployeeRepo, GradeRepo, GradeHistoryRepo,
│                    RefreshTokenRepo, TokenBlacklistRepo
├── dto/             *RequestDTO, *ResponseDTO, AuthRequestDTO, AuthResponseDTO,
│                    RefreshRequestDTO, ErrorDTO
├── entity/          Employee, Grade, GradeHistory, RefreshToken, TokenBlacklist
├── exception/       BadRequestException, NotFoundException, GradeUpdateRuleViolationException
│                    GlobalExceptionHandler  (maps all three to ErrorDTO)
├── mapper/          EmployeeMapper, GradeMapper, GradeHistoryMapper
├── service/
│   ├── interfaces/  EmployeeService, GradeService, GradeHistoryService,
│   │                RefreshTokenService, TokenBlacklistService
│   └── classes/     EmployeeServiceImpl, GradeServiceImpl, GradeHistoryServiceImpl,
│                    MyUserDetailService, RefreshTokenServiceImpl, TokenBlacklistServiceImpl
└── util/            JWTUtil, CommonUtil
```

---

## Domain Model

Five JPA entities in `entity/`:

- **`Employee`** — core entity. Fields: `employeeId` (sequence, 6-digit), `firstName`, `lastName`, `phoneNumber`, `emailAddress`, `role`, `password` (BCrypt hash), `accessGranted`, `currentGrade` (@ManyToOne), `gradeHistories` (@OneToMany).
- **`Grade`** — reference/lookup table (`grades`). Seeded with Grade-1, Grade-2, Grade-3 by `DataInitializer`.
- **`GradeHistory`** — append-only audit log of `(employee, grade, assignedOn)`. A new row must be written every time a grade is assigned or changed.
- **`RefreshToken`** — stores one active refresh token per employee. Fields: `id`, `token` (UUID string, unique), `employee` (@OneToOne, unique), `expiryDate`. Old token is deleted on every new login or refresh call.
- **`TokenBlacklist`** — stores access tokens that have been explicitly invalidated via logout. Fields: `id`, `token` (length 512, unique), `expiryDate`. Entries are cleaned up on startup once their `expiryDate` has passed (tokens only live 1 hour so the table stays small).

---

## Startup Seeding — DataInitializer

`config/DataInitializer` implements `ApplicationRunner` and runs on every startup:

1. **Seeds grades** — inserts Grade-1 / Grade-2 / Grade-3 if `grades` table is empty.
2. **Seeds default employees** — creates the following if they don't already exist:

| Role | Email | Default password |
|---|---|---|
| HR | `admin.hr@cognizant.com` | `Admin@123` |
| TravelDeskExe | `desk.exec@cognizant.com` | `Exec@123` |

Passwords are BCrypt-hashed (strength 12) before saving.

3. **Cleans up expired blacklisted tokens** — runs `tokenBlacklistRepo.deleteExpiredTokens(LocalDateTime.now())` to purge stale entries from `token_blacklist`.

---

## Business Rules

### Employee ID
- Auto-generated from `@SequenceGenerator` (`employee_seq`, `initialValue = 100000`, `allocationSize = 1`) — guarantees IDs from 100000 to 999999 (always 6 digits).
- `CommonUtil.validateEmployeeId(Long id)` enforces the range in `getById`, `updateEmployee`, and `deleteEmployee`. Violation → `BadRequestException`.

### Email
- Must end with `@cognizant.com`. Validated by `CommonUtil.validateEmail(String)` / `validateEmailAddress(EmployeeRequestDTO)`, called from both create and update paths.

### Grade-change rules
Enforced in `EmployeeServiceImpl.updateEmployee` — all four rules must hold:

1. Grade can only go **upward** — lower `Grade.id` = higher seniority. Trying to set a higher ID is a downgrade → `GradeUpdateRuleViolationException`.
2. New joiner freeze — grade cannot change within **2 years** of the earliest `GradeHistory.assignedOn` (treated as joining date).
3. After the 2-year freeze — grade can change at most **once per year** vs. the latest `GradeHistory.assignedOn`.
4. On `createEmployee`, if `role == "TravelDeskExe"` the grade is force-set to Grade-1 (`id = 1L`) regardless of input.

Violations → `GradeUpdateRuleViolationException`. Missing grade/employee → `BadRequestException` / `NotFoundException`. All mapped to `ErrorDTO` by `GlobalExceptionHandler`.

### Password
- HR provides plain-text `password` in `POST /api/employees` request body.
- `EmployeeServiceImpl.createEmployee` BCrypt-hashes it (strength 12) before saving.
- `EmployeeRequestDTO` has `@ToString(exclude = "password")` — passwords are never logged.
- The update endpoint (`PUT /api/employees/{id}`) does **not** change the password.

---

## Authentication & Authorization (JWT + Refresh Token + Blacklist)

Spring Security with stateless JWT. CSRF disabled globally.

### Token strategy

| Token | Validity | Storage |
|---|---|---|
| Access token | **1 hour** | Client memory / Authorization header |
| Refresh token | **7 days** | `refresh_tokens` DB table (one per employee) |
| Blacklisted token | Until expiry | `token_blacklist` DB table (cleaned up on startup) |

### Full auth lifecycle

```
1. POST /login  →  access token (1h) + refresh token (7d) returned
2. Every API call  →  Authorization: Bearer <accessToken>
3. Access token expires  →  403  →  call POST /auth/refresh
4. POST /auth/refresh  →  new access token + new refresh token (token rotation)
5. Refresh token expires (7 days)  →  400  →  must POST /login again
6. POST /auth/logout  →  refresh token deleted + access token blacklisted immediately
```

### JwtAuthFilter — request validation order

For every incoming request:
1. Extract `Bearer <token>` from `Authorization` header
2. Parse and extract `username` from token (catches exception silently if invalid/expired)
3. **Check `TokenBlacklistService.isBlacklisted(token)`** — if `true`, skip authentication entirely → 403
4. Load `UserDetails` from `EmployeeRepo` via `MyUserDetailService`
5. Call `JWTUtil.validateToken()` — checks username match + not expired
6. Set `UsernamePasswordAuthenticationToken` in `SecurityContext`

### MyUserDetailService

Loads from `EmployeeRepo.findByEmailAddress(email)`. Maps `Employee` to Spring `UserDetails`:
- `username` → `employee.emailAddress`
- `password` → `employee.password` (BCrypt hash)
- `authorities` → `[employee.role]` (single authority, no `ROLE_` prefix)
- `enabled` → `employee.accessGranted` — set to `false` to disable login without deleting the record.

### Authorization rules (SecurityConfig)

| Path | Rule |
|---|---|
| `POST /login` | `permitAll` |
| `POST /auth/refresh` | `permitAll` (access token may be expired) |
| `POST /auth/logout` | `permitAll` (access token may be expired) |
| `/h2-console/**` | `permitAll` |
| `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` | `permitAll` |
| `GET /api/**` | any authenticated user |
| `POST /api/**`, `PUT /api/**`, `DELETE /api/**` | `hasAuthority("HR")` only |
| everything else | authenticated |

### Roles

Raw strings, no `ROLE_` prefix. Constants in `AppConstant`: `ROLE_HR = "HR"`, `ROLE_EMPLOYEE = "Employee"`, `ROLE_TRAVEL_DESK_EXE = "TravelDeskExe"`.

### JWTUtil — methods

| Method | Description |
|---|---|
| `generateToken(username)` | Creates a signed JWT valid for 1 hour |
| `extractUsername(token)` | Parses subject claim; throws if token is invalid or expired |
| `validateToken(token, userDetails)` | Returns true if username matches and token is not expired |
| `extractExpiration(token)` | Returns `LocalDateTime` expiry; **safe on expired tokens** (catches `ExpiredJwtException` and reads the claim from the exception) — used by `TokenBlacklistService` |

### JWT signing key — caveat

`JWTUtil` generates a fresh random HmacSHA256 key on every app startup. **All previously issued tokens are invalidated on restart.** To persist tokens across restarts, externalize the secret to `application.properties` (e.g. `jwt.secret=<base64-value>`) and inject via `@Value`.

### Refresh token — important implementation note

`RefreshTokenRepo.deleteByEmployee` uses `@Modifying(clearAutomatically = true)` with a direct JPQL DELETE. **Do not change this to a derived delete method.** The derived method goes through Hibernate's entity lifecycle and batches the DELETE behind the next INSERT, causing a unique constraint violation on `employee_id`. The `@Modifying @Query` executes immediately, before the INSERT.

### Token blacklist — logout behaviour

`POST /auth/logout` requires **both**:
- `Authorization: Bearer <accessToken>` header — the access token is read and saved to `token_blacklist`
- `{ "refreshToken": "..." }` body — the refresh token is deleted from `refresh_tokens`

If the `Authorization` header is omitted, the refresh token is still deleted (no new `/auth/refresh` calls possible) but the access token remains valid until its natural 1-hour expiry. **Always send both when calling logout.**

---

## API Endpoints

### Auth

| Method | URL | Auth | Body | Description |
|---|---|---|---|---|
| POST | `/login` | Open | `{ emailAddress, password }` | Authenticate; returns access token + refresh token |
| POST | `/auth/refresh` | Open | `{ refreshToken }` | Rotate tokens; old refresh token deleted, new pair returned |
| POST | `/auth/logout` | Open | `{ refreshToken }` | Blacklist access token + delete refresh token |

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
| GET | `/api/gradeHistory` | Authenticated | List all grade history records |
| GET | `/api/gradeHistory/{employeeId}` | Authenticated | Grade history for a specific employee |

---

## Error Messages

User-facing strings live in `messages.properties`. Keys are constants in `AppConstant`. Always look up via `messageSource.getMessage(CONSTANT, null, Locale.ENGLISH)` — never use string literals.

| Constant | Key | Message |
|---|---|---|
| `ERROR_GRADE_CHANGE_NEW_JOINER` | `error.grade.change.new.joiner` | Grade of new joiners can only be changed after they complete 2 years |
| `ERROR_GRADE_CHANGE_ONCE_YEAR` | `error.grade.change.once.year` | An employee grade can only be changed once in a year |
| `ERROR_GRADE_CHANGE_UPWARDS_ONLY` | `error.grade.change.upwards.only` | Employee grade can only be upgraded, not downgraded |
| `ERROR_EMPLOYEE_NOT_FOUND` | `error.employee.not.found` | Employee not found |
| `ERROR_EMPLOYEE_INVALID_EMAIL` | `error.employee.invalid.email` | Invalid email address |
| `ERROR_EMPLOYEE_INVALID_ID` | `error.employee.invalid.id` | Employee ID must be a valid 6 digit number |
| `ERROR_GRADE_NOT_FOUND` | `error.grade.not.found` | Grade not found |
| `ERROR_INVALID_CREDENTIALS` | `error.invalid.credentials` | Invalid email or password |
| `ERROR_REFRESH_TOKEN_INVALID` | `error.refresh.token.invalid` | Invalid refresh token |
| `ERROR_REFRESH_TOKEN_EXPIRED` | `error.refresh.token.expired` | Refresh token has expired. Please login again |

---

## Mappers

Plain `@Component` classes in `mapper/` — no MapStruct. Entities are never returned from controllers; always map to a `*ResponseDTO` first.

- `EmployeeMapper.mapEmployeeToEmployeeResponseDTO` — derives `gradeAssignedOn` by sorting `gradeHistories` descending and taking the last element. Depends on `GradeHistory.assignedOn` being non-null.
- `EmployeeMapper.mapEmployeeRequestDTOToEmployee` — does **not** set `password`. The service handles BCrypt hashing and sets it on the entity after mapping.

---

## Conventions

- **Constructor injection only** — no `@Autowired` field injection anywhere.
- **Service interfaces** in `service/interfaces/`, impls in `service/classes/`.
- **Lombok** on all entities and DTOs: `@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor`. Use `@ToString(exclude = "password")` on any DTO carrying credentials.
- **SLF4J logger** per class — `logger.info(...)` on entry to controller/service methods, `logger.warn(...)` immediately before throwing any exception.
- **`@Transactional`** on service impl methods that write to the DB (create / update / delete).
- **`@Modifying(clearAutomatically = true) @Query`** for any bulk DELETE that runs alongside an INSERT in the same transaction — never use derived delete methods in those cases (Hibernate batching causes unique constraint violations).
- **CLAUDE.md must be updated** whenever any code change is made — entity added, endpoint changed, business rule modified, bug fixed.
- **Springdoc OpenAPI** — Swagger UI at `/swagger-ui.html`, JSON spec at `/v3/api-docs`.
