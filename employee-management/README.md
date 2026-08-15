# Employee & Department Management API — Week 4

A production-oriented Spring Boot REST API for managing Employees and Departments, built for the
**Week 4 Trainee Assignment: Java & Spring Boot — Advanced REST API Development**.

Extends the Week 3 base with: JPA relationships, layered DTOs, advanced JPQL/native queries, pagination &
sorting & search, centralized exception handling, SLF4J logging, transaction + rollback demo, external REST
API integration, Swagger/OpenAPI docs, and JUnit 5 + Mockito unit tests.

## Technologies Used

- Java 17, Spring Boot 3.3
- Spring Web, Spring Data JPA, Bean Validation
- MySQL (runtime) / H2 (tests)
- springdoc-openapi (Swagger UI)
- RestClient (external API integration)
- Lombok
- JUnit 5, Mockito, AssertJ, JaCoCo
- Docker / docker-compose

## Project Structure

```
src/main/java/com/trainee/employeemanagement
├── EmployeeManagementApplication.java
├── audit/            EmployeeAudit entity (transaction demo)
├── config/           OpenApiConfig, RestClientConfig, CorrelationIdFilter
├── controller/        EmployeeController, DepartmentController
├── dto/               Request/Response DTOs, ApiResponse, PageResponseDTO, ExternalProfileDTO
├── entity/            Employee, Department
├── exception/         Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
├── repository/        EmployeeRepository, DepartmentRepository, EmployeeAuditRepository
└── service/           EmployeeService/Impl, DepartmentService/Impl, EmployeeExternalIntegrationService
```

## 1. JPA Relationships

`Department` (one) ↔ `Employee` (many) via `@OneToMany(mappedBy = "department")` /
`@ManyToOne @JoinColumn(name = "department_id")`.

- **Lazy vs Eager:** both sides are `FetchType.LAZY`. An employee list or a department lookup doesn't always
  need the other side fully loaded, and eager fetching a department's full employee collection (or every
  employee's department) on every query would be unpredictable and wasteful at scale. Where the department
  name IS needed for a response, it's read explicitly inside the `@Transactional` service method while the
  Hibernate session is still open, so the lazy proxy resolves safely.
- **Cascade:** `Department.employees` cascades `PERSIST` and `MERGE` only — deliberately **not** `REMOVE`.
  Deleting a department with employees still assigned is rejected as a `400 Bad Request` business-rule
  violation (`DepartmentServiceImpl#delete`) rather than silently cascading deletes to employee records.

Full reasoning is documented as Javadoc directly on `Employee.java` / `Department.java`.

## 2. REST APIs (all under `/api/v1`)

### Departments
| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/departments` | Create department |
| GET | `/api/v1/departments` | List all departments |
| GET | `/api/v1/departments/{id}` | Get by id |
| PUT | `/api/v1/departments/{id}` | Update |
| DELETE | `/api/v1/departments/{id}` | Delete (fails if it still has employees) |

### Employees
| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/employees` | Create employee |
| POST | `/api/v1/employees/with-audit?performedBy=admin` | Create + audit record, single transaction |
| GET | `/api/v1/employees?page=0&size=10&sortBy=salary&direction=desc` | Paginated + sorted list |
| GET | `/api/v1/employees/{id}` | Get by id |
| PUT | `/api/v1/employees/{id}` | Update |
| DELETE | `/api/v1/employees/{id}` | Delete |
| GET | `/api/v1/employees/search?name=John&page=0&size=10` | Search by name (paginated) |
| GET | `/api/v1/employees/department/{departmentId}?page=0&size=10` | Employees in a department (paginated) |
| GET | `/api/v1/employees/salary-above/{amount}` | Employees earning above a salary |
| GET | `/api/v1/employees/highest-paid` | Highest-paid employee |
| GET | `/api/v1/employees/joined-after/{yyyy-MM-dd}` | Employees who joined after a date |
| GET | `/api/v1/employees/average-salary-by-department` | Average salary per department |
| GET | `/api/v1/employees/external-profile/{externalId}` | External REST API integration demo |

Every response is wrapped in a consistent `ApiResponse<T>` envelope (`success`, `message`, `data`, `timestamp`).

## 3. DTOs

`EmployeeRequestDTO` / `EmployeeResponseDTO` and `DepartmentRequestDTO` / `DepartmentResponseDTO` keep JPA
entities out of the controller layer entirely — controllers and services only ever see DTOs.
`PageResponseDTO<T>` wraps Spring Data's `Page<T>` so pagination metadata is a stable, documented shape
instead of leaking Spring internals.

## 4. Advanced Database Queries (`EmployeeRepository`)

- **Derived:** `findBySalaryGreaterThan`, `findByNameContainingIgnoreCase`, `findByDepartmentId`,
  `findByJoiningDateAfter`
- **JPQL:** `findAllOrderedBySalaryDesc` (backs highest-paid), `findEmployeesJoinedAfter`,
  `averageSalaryByDepartmentJpql`
- **Native SQL:** `averageSalaryByDepartmentNative`, `findBySalaryAboveNative`

## 5. Validation & Exception Handling

Bean Validation (`@NotBlank`, `@Email`, `@Positive`, `@PastOrPresent`) on request DTOs.
Custom exceptions: `ResourceNotFoundException` (404), `DuplicateResourceException` (409),
`BadRequestException` (400), `ExternalApiException` (502) — all mapped by a single
`@RestControllerAdvice` (`GlobalExceptionHandler`) to a consistent error body:

```json
{
  "success": false,
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: 42",
  "path": "/api/v1/employees/42",
  "timestamp": "2026-08-13T10:15:30"
}
```

## 6. Logging

SLF4J via `LoggerFactory.getLogger(...)` in every service/controller/exception handler. INFO for business
events (create/update/delete), DEBUG for read/query details, WARN for expected failures (not found,
duplicate), ERROR for unexpected/downstream failures. No request ever logs a password or token.
`CorrelationIdFilter` (bonus) attaches an `X-Correlation-Id` to every request and to the SLF4J MDC so all log
lines for one request can be grepped together (`logging.pattern.console` in `application.properties`).

## 7. Pagination, Sorting & Searching

`GET /api/v1/employees` accepts `page`, `size`, `sortBy`, `direction`. Name search and department filtering
are separate, also-paginated endpoints (`/search`, `/department/{id}`) rather than overloading one endpoint
with every optional filter, to keep each endpoint's contract explicit.

## 8. Transaction Management

`POST /api/v1/employees/with-audit` (`EmployeeServiceImpl#createEmployeeWithAudit`) saves an `Employee` and
an `EmployeeAudit` row inside one `@Transactional` method. Both writes share the same transaction/EntityManager;
if the audit write throws (missing `performedBy`, or a DB failure), Spring's default rollback-on-RuntimeException
behavior rolls back the employee insert too — you never end up with an employee that has no audit trail.

## 9. External REST API Integration

`EmployeeExternalIntegrationService` calls the public mock API `https://jsonplaceholder.typicode.com/users/{id}`
via Spring's `RestClient` (`GET /api/v1/employees/external-profile/{externalId}`). Demonstrates: a custom
header, an explicit connect/read timeout (`RestClientConfig`), mapping the raw JSON into `ExternalProfileDTO`,
and translating `HttpClientErrorException` / `HttpServerErrorException` / `ResourceAccessException` into typed
`ResourceNotFoundException` / `ExternalApiException` responses instead of leaking RestClient exceptions.

## 10. Swagger / OpenAPI

Once running: **Swagger UI** → `http://localhost:8080/swagger-ui.html`, raw spec → `/v3/api-docs`.
All controllers are annotated with `@Tag` / `@Operation` for descriptions.

## 11. Unit Testing

`EmployeeServiceImplTest` and `DepartmentServiceImplTest` (JUnit 5 + Mockito + AssertJ) cover: success paths,
not-found, duplicate data, invalid input, and repository failures for both services. Run:

```bash
mvn test
```

Coverage report (JaCoCo) is generated at `target/site/jacoco/index.html` after `mvn test` — open it in a
browser and screenshot it for the deliverable.

## Database Setup

```bash
mysql -u root -p < sql/schema.sql
```

or simply start the app with the `local`/`docker` profile (`ddl-auto=update`) and let Hibernate create it.

## Spring Profiles

| Profile | Purpose | ddl-auto |
|---|---|---|
| `local` | Individual dev machine | update |
| `dev` | Shared dev environment | validate |
| `qa` | QA/test environment | validate |
| `docker` | docker-compose stack | update |
| `test` | Unit/integration tests (H2, in-memory) | create-drop |

Set with `--spring.profiles.active=<name>` or `SPRING_PROFILES_ACTIVE` env var.
DB credentials are read from environment variables (`LOCAL_DB_PASSWORD`, `DEV_DB_PASSWORD`, `QA_DB_PASSWORD`,
`MYSQL_PASSWORD`) rather than committed in plaintext.

## Running the App

```bash
mvn spring-boot:run
# or
mvn clean package && java -jar target/employee-management.jar
```

### With Docker

```bash
docker compose up --build
```
Starts MySQL + the app together; the app is reachable at `http://localhost:8080`.

## Deliverables Checklist

- [x] Source code (layered: Controller → DTO → Service → Repository → Entity)
- [x] `sql/schema.sql` — database setup script
- [x] `postman/Employee-Management.postman_collection.json`
- [x] Swagger/OpenAPI (`/swagger-ui.html`)
- [x] Unit tests (`mvn test`) + JaCoCo coverage report (screenshot it yourself after running)
- [x] This README (setup, profiles, DB config, APIs, execution steps)
- [x] Git repository with history

## Bonus Tasks

| Task | Status |
|---|---|
| Dockerize the application | ✅ `Dockerfile` + `docker-compose.yml` |
| Request/correlation ID | ✅ `CorrelationIdFilter` |
| Redis caching | ⬜ Not implemented — natural extension point: `@Cacheable` on `DepartmentServiceImpl#getAll` |
| JWT authentication | ⬜ Not implemented — would sit in `config/` as a `SecurityFilterChain` + JWT filter |
| CompletableFuture async processing | ⬜ Not implemented — a candidate use case is the external profile lookup |
