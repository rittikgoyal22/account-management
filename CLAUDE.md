# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a Spring Boot 3.5.7 / Java 21 / Gradle project. Use the Gradle wrapper:

- Build: `./gradlew build` (Linux/macOS) or `gradlew.bat build` (Windows)
- Run app: `./gradlew bootRun` — listens on port **8081**
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "com.etd.account_management.AccountManagementApplicationTests"`
- Run a single test method: `./gradlew test --tests "FQCN.methodName"`
- Clean: `./gradlew clean`

The package is `com.etd.account_management` (underscore — the original hyphenated form was invalid; see [HELP.md](HELP.md)).

## Database

- Default datasource is **H2 file-mode** at `~/data/account_management` with `AUTO_SERVER=TRUE` so multiple processes (app + console) can connect to the same file. See [src/main/resources/application.properties](src/main/resources/application.properties).
- H2 console is enabled at `/h2-console` (username `sa`, blank password).
- A MySQL configuration is commented out in the same file; switching back requires uncommenting both the `mysql-connector-j` dependency in [build.gradle](build.gradle) and the MySQL `spring.datasource.*` lines.
- [src/main/java/com/etd/account_management/account_management.sql](src/main/java/com/etd/account_management/account_management.sql) is a **MySQL** reference schema (with `REGEXP` check constraints and `AUTO_INCREMENT(100000, 1)`); it is not executed by the app and is not H2-compatible. JPA/Hibernate auto-creates tables for H2 by default.

## Architecture

Standard Spring Boot layered REST API with three domain entities. All endpoints are under `/api/*` and annotated `@CrossOrigin` (open CORS).

```
controller → service.interfaces / service.classes → dao (JpaRepository) → entity
                       ↘ mapper ↗
                       ↘ dto   ↗
```

### Domain model

Three tightly coupled JPA entities in [src/main/java/com/etd/account_management/entity/](src/main/java/com/etd/account_management/entity/):

- `Employee` — has a `@ManyToOne currentGrade` and a `@OneToMany gradeHistories`.
- `Grade` — reference table (`grades`), referenced by both Employee and GradeHistory.
- `GradeHistory` — append-only log of `(employee, grade, assignedOn)` rows; **must be written every time** an employee's grade is assigned or changed.

### Grade-change business rules

Enforced in [EmployeeServiceImpl.updateEmployee](src/main/java/com/etd/account_management/service/classes/EmployeeServiceImpl.java) — any change here must preserve all of:

1. Grade can only be **upgraded** (`new < current` by ID is treated as a downgrade and rejected). Note: the comparison assumes lower numeric `Grade.id` = higher seniority.
2. New joiners cannot change grade within **2 years** of their earliest `GradeHistory.assignedOn` (treated as joining date).
3. After that, grade can change at most **once per year** vs. the latest `GradeHistory.assignedOn`.
4. On `createEmployee`, if `role == "TravelDeskExe"` the grade is force-set to `1L` regardless of what was sent ([AppConstant.ROLE_TRAVEL_DESK_EXE](src/main/java/com/etd/account_management/constant/AppConstant.java)).
5. Every grade assignment (create or change) writes a new `GradeHistory` row via `GradeHistoryMapper.createGradeHistoryByEmployeeAndGrade`.

Violations throw `GradeUpdateRuleViolationException`; missing grade/employee throw `BadRequestException` / `NotFoundException`. All three are mapped to `ErrorDTO` JSON responses by [GlobalExceptionHandler](src/main/java/com/etd/account_management/exception/GlobalExceptionHandler.java).

### Validation

- Email must end with `@cognizant.com` ([AppConstant.EMAIL_DOMAIN](src/main/java/com/etd/account_management/constant/AppConstant.java)); centralized in [CommonUtil.validateEmailAddress](src/main/java/com/etd/account_management/util/CommonUtil.java) and called from both create and update paths.
- The MySQL reference schema also enforces 10-digit phone and a role whitelist (`Employee`, `HR`, `TravelDeskExe`), but these are **not enforced in Java** — add validation in code if you depend on them under H2.

### Error messages

User-facing error strings live in [src/main/resources/messages.properties](src/main/resources/messages.properties) and are looked up via `MessageSource` using keys defined as `String` constants in [AppConstant](src/main/java/com/etd/account_management/constant/AppConstant.java). When adding a new error: add the key constant in `AppConstant`, the English text in `messages.properties`, and pass the constant (not a literal) to `messageSource.getMessage(...)`.

### Mappers

Plain `@Component` classes in [src/main/java/com/etd/account_management/mapper/](src/main/java/com/etd/account_management/mapper/) — no MapStruct. Entities are never returned from controllers; always map to a `*ResponseDTO` first. `EmployeeMapper.mapEmployeeToEmployeeResponseDTO` derives `gradeAssignedOn` by sorting `gradeHistories` descending and taking the last element — be aware this logic depends on `GradeHistory.assignedOn` being non-null.

## Authentication & authorization (JWT)

Spring Security with stateless JWT auth. The flow:

1. `POST /register` (open) — creates an `AppUser` (table `app_users`) with BCrypt-hashed password, returns a JWT. Accepts `emailAddress`, `password`, optional `role` (defaults to `Employee`).
2. `POST /login` (open) — authenticates via `AuthenticationManager`/`DaoAuthenticationProvider`, returns a fresh JWT.
3. All other requests must send `Authorization: Bearer <token>`. [JwtAuthFilter](src/main/java/com/etd/account_management/config/JwtAuthFilter.java) runs before `UsernamePasswordAuthenticationFilter`, parses the JWT, loads the user via [MyUserDetailService](src/main/java/com/etd/account_management/service/classes/MyUserDetailService.java), and populates the `SecurityContext`.

Authorization rules ([SecurityConfig.filterChain](src/main/java/com/etd/account_management/config/SecurityConfig.java)):
- `/register`, `/login` — permitAll
- `/h2-console/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/v3/api-docs.yaml` — permitAll
- `GET /api/**` — any authenticated user
- `POST/PUT/DELETE /api/**` — `hasAuthority("HR")` only
- everything else — authenticated

X-Frame-Options is set to `sameOrigin` so the H2 console's iframes render. CSRF is disabled globally, so H2 console POSTs work.

### JWT signing key — caveat

[JWTUtil](src/main/java/com/etd/account_management/util/JWTUtil.java) generates a fresh HmacSHA256 key in its no-arg constructor on every app startup. **All previously issued tokens are invalidated on restart.** Fine for dev; for any shared/persistent use, externalize the secret (e.g. `jwt.secret` in `application.properties` or env var) and decode it in the constructor instead of generating a new one.

### Identity model

`AppUser` (table `app_users`) is intentionally separate from `Employee`. Creating an `Employee` via `POST /api/employees` does **not** create a login — auth users and HR-managed employee records are two distinct concerns. If you need to link them, add a foreign key on `app_users.email` → `employees.email_address` (both unique) and join on demand.

### Roles

Authorities are the raw role strings (no `ROLE_` prefix) — `hasAuthority("HR")` matches the literal `role` column. Existing role constants in [AppConstant](src/main/java/com/etd/account_management/constant/AppConstant.java): `HR`, `Employee`, `TravelDeskExe`. The Employee entity also has a `role` column — it is **not** used for security (only `AppUser.role` is); they're independent fields that happen to share a vocabulary.

### `ddl-auto=update`

Set in [application.properties](src/main/resources/application.properties) so that newly added entities (like `AppUser`) get their tables created against the existing H2 file without dropping prior data. Without it, the file-mode default is `none` and you'll see `Table "APP_USERS" not found` on first request.

## Conventions seen in the codebase

- Constructor injection only (no `@Autowired` field injection).
- Each service has an interface in `service/interfaces/` and an impl in `service/classes/`.
- Lombok (`@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor`) on entities and DTOs.
- SLF4J logger per class; `info` on entry to controller/service methods, `warn` before throwing.
- Transactional boundaries are on the service impl (`@Transactional` on create/update/delete).
- Springdoc OpenAPI is on the classpath — when the app is running, UI is at `/swagger-ui.html` and the JSON spec at `/v3/api-docs`.
