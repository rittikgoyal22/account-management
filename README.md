# Account Management Service

Part of the **Employee Travel Desk (ETD)** system — a Cognizant FSE Business Aligned Project.

This microservice is the **HR and Authentication hub** of the ETD platform. It manages all employee records, controls access to the system, and issues JWT tokens used by every other service.

---

## What this service does

| Responsibility | Details |
|---|---|
| **Employee management** | HR can add, view, update grade, and delete employees |
| **Authentication** | Issues JWT access tokens (1 hour) and refresh tokens (7 days) on login |
| **Session management** | Token refresh and logout with immediate access token invalidation |
| **Grade tracking** | Every grade change is recorded as an audit history entry |
| **Role-based access** | HR has full write access; all authenticated users can read |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.7 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Database | H2 (file-mode, dev) / MySQL (prod, commented out) |
| Build tool | Gradle |
| Utilities | Lombok, Springdoc OpenAPI |

---

## Prerequisites

- Java 21
- Gradle (system install — the wrapper jar is not committed)

---

## Running the application

```bash
# Build
gradle build

# Run — starts on port 8081
gradle bootRun

# Run tests
gradle test

# Clean build output
gradle clean
```

On first startup, **DataInitializer** automatically:
- Seeds grades: Grade-1, Grade-2, Grade-3
- Creates default HR and TravelDeskExec users
- Cleans up any expired blacklisted tokens from previous sessions

---

## Default credentials

| Role | Email | Password |
|---|---|---|
| HR | `admin.hr@cognizant.com` | `Admin@123` |
| TravelDeskExec | `desk.exec@cognizant.com` | `Exec@123` |

---

## Database

### H2 (default — no setup needed)

The app uses H2 in file mode. Database is stored at `~/data/account_management`.

- Console URL: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:file:~/data/account_management`
- Username: `sa` | Password: *(blank)*

### Fresh start (wipe all data)

Stop the app, delete these two files, then restart:
```
~/data/account_management.mv.db
~/data/account_management.trace.db
```

### Switch to MySQL

1. Uncomment `runtimeOnly 'com.mysql:mysql-connector-j'` in `build.gradle`
2. Uncomment the MySQL `spring.datasource.*` lines in `application.properties`
3. Comment out the H2 datasource lines

---

## API Reference

Base URL: `http://localhost:8081`

### Authentication

#### Login
```
POST /login
```
**Body:**
```json
{
  "emailAddress": "admin.hr@cognizant.com",
  "password": "Admin@123"
}
```
**Response `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "a3f9b2c1-d4e5-6f78-90ab-cdef12345678",
  "emailAddress": "admin.hr@cognizant.com",
  "role": "HR"
}
```

#### Refresh Token
```
POST /auth/refresh
```
Call this when the access token expires (you receive a `403`). The old refresh token is deleted and a new pair is issued (token rotation).

**Body:**
```json
{
  "refreshToken": "a3f9b2c1-d4e5-6f78-90ab-cdef12345678"
}
```
**Response `200`:** Same structure as login — save both new tokens immediately.

#### Logout
```
POST /auth/logout
```
Deletes the refresh token from DB **and** immediately blacklists the access token so it cannot be reused within its remaining 1-hour window.

**Header:** `Authorization: Bearer <accessToken>`
**Body:**
```json
{
  "refreshToken": "a3f9b2c1-d4e5-6f78-90ab-cdef12345678"
}
```
**Response `204 No Content`**

> Always include the `Authorization` header on logout. Without it the refresh token is deleted but the access token remains valid until its natural expiry.

---

### Employees

All endpoints require `Authorization: Bearer <token>`.

#### Get all employees
```
GET /api/employees
```
**Response `200`:** Array of employee objects.

#### Get employee by ID
```
GET /api/employees/{id}
```
ID must be a valid 6-digit number (100000–999999).

**Response `200`:**
```json
{
  "id": 100000,
  "firstName": "Admin",
  "lastName": "HR",
  "phoneNumber": "9000000001",
  "emailAddress": "admin.hr@cognizant.com",
  "role": "HR",
  "gradeName": "Grade-1",
  "gradeAssignedOn": "2025-06-05T10:00:00"
}
```

#### Create employee *(HR only)*
```
POST /api/employees
```
**Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "9876543210",
  "emailAddress": "john.doe@cognizant.com",
  "role": "Employee",
  "password": "John@123",
  "currentGradeId": 3,
  "accessGranted": true
}
```
> `role` must be one of: `Employee`, `HR`, `TravelDeskExe`
> For `TravelDeskExe`, grade is always force-set to Grade-1 regardless of `currentGradeId`.

#### Update employee *(HR only)*
```
PUT /api/employees/{id}
```
Used primarily to upgrade an employee's grade. Send the same body structure as create (password field is ignored — passwords cannot be changed via this endpoint).

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

---

## Role Permissions

| Endpoint | HR | Employee | TravelDeskExe |
|---|:---:|:---:|:---:|
| `POST /login` | ✅ | ✅ | ✅ |
| `POST /auth/refresh` | ✅ | ✅ | ✅ |
| `POST /auth/logout` | ✅ | ✅ | ✅ |
| `GET /api/**` | ✅ | ✅ | ✅ |
| `POST /api/employees` | ✅ | ❌ | ❌ |
| `PUT /api/employees/{id}` | ✅ | ❌ | ❌ |
| `DELETE /api/employees/{id}` | ✅ | ❌ | ❌ |

---

## Business Rules

### Employee ID
- Auto-generated, always 6 digits (100000–999999)
- Passing an out-of-range ID to any endpoint returns `400 Employee ID must be a valid 6 digit number`

### Email
- Must end with `@cognizant.com`
- Enforced on both create and update

### Grade changes
1. **Upward only** — Grade-1 is highest seniority (lowest ID). Downgrading returns `400`
2. **2-year new joiner freeze** — no grade change within 2 years of the joining date (earliest grade history record)
3. **Once per year** — after the freeze, grade can only change once in any 12-month period
4. **TravelDeskExec default** — always assigned Grade-1 on creation regardless of input

### Password
- Stored as BCrypt hash (strength 12) — never plain text
- Provided by HR when creating an employee via `POST /api/employees`
- Cannot be changed through the update endpoint

---

## Token Lifecycle

```
POST /login
  ├── accessToken  — valid 1 hour  → send in Authorization: Bearer header
  └── refreshToken — valid 7 days  → store safely, only send to /auth/refresh

accessToken expires → 403 on any API call
  → POST /auth/refresh  → new accessToken + new refreshToken (old one deleted)

refreshToken expires after 7 days of inactivity
  → 400 "Refresh token has expired. Please login again"
  → Must POST /login again

POST /auth/logout
  → refreshToken deleted from DB
  → accessToken added to blacklist (cannot be used even within remaining 1-hour window)
  → 204 No Content
```

---

## Error Response Format

All errors return a consistent JSON structure:
```json
{
  "message": "Human-readable error description",
  "fieldName": "Field that caused the error (or null)",
  "status": "HTTP status name"
}
```

| HTTP Status | When |
|---|---|
| `400 BAD_REQUEST` | Validation failure, invalid credentials, bad token |
| `404 NOT_FOUND` | Employee or grade not found |
| `403 FORBIDDEN` | Missing/expired/blacklisted token, insufficient role |

---

## Swagger UI

When the app is running:
- **UI:** `http://localhost:8081/swagger-ui.html`
- **JSON spec:** `http://localhost:8081/v3/api-docs`

---

## Project Structure

```
src/main/java/com/etd/account_management/
├── config/           Security config, JWT filter, DataInitializer
├── constant/         All string constants and message keys
├── controller/       REST controllers (Auth, Employee, Grade, GradeHistory)
├── dao/              JPA repositories
├── dto/              Request and response DTOs
├── entity/           JPA entities (Employee, Grade, GradeHistory, RefreshToken, TokenBlacklist)
├── exception/        Custom exceptions + GlobalExceptionHandler
├── mapper/           Entity ↔ DTO mappers
├── service/
│   ├── interfaces/   Service contracts
│   └── classes/      Service implementations
└── util/             JWTUtil, CommonUtil (validation helpers)
```

---

## Related Services

This service is one of four microservices in the ETD platform:

| Service | Port | Responsibility |
|---|---|---|
| **account-management** *(this service)* | 8081 | Employee management + Authentication |
| travel-planner | — | Travel request lifecycle, budget calculation |
| reservation-management | — | Flight/hotel/cab reservation upload and tracking |
| reimbursement-management | — | Expense claim submission and processing |

All other services validate the JWT issued by this service. They must share the same JWT signing secret to verify tokens without calling this service on every request.
