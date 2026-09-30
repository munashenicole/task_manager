# Task Planner

A full-stack task management web application built with **Java 21 · Spring Boot 3.3 · Thymeleaf · Spring Data JPA · PostgreSQL**.

---

## 🌐 Live Deployment

| Item | Detail |
|---|---|
| **URL** | https://task-planner-64ey.onrender.com |
| **Login required** | ❌ No authentication — the app is publicly accessible |
| **Demo note** | Data is shared; feel free to create, edit, and delete tasks |


---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 21+ |
| Maven | 3.9+ |
| PostgreSQL | 14+ (local) _or_ use Render's hosted DB |
| Git | Any recent version |

---

## Quick Start (Local)

### 1. Clone the repository
```bash
git clone <your-repo-url>
cd Task_Planner
```

### 2. Create the local database
```sql
-- run once as a PostgreSQL superuser
CREATE DATABASE task_planner_db;
```

### 3. Configure credentials
Edit [`src/main/resources/application.properties`](src/main/resources/application.properties) and set the fallback password to match your local PostgreSQL password:
```properties
spring.datasource.password=${DB_PASSWORD:YOUR_PASSWORD_HERE}
```

### 4. Run the application
```powershell
# Windows PowerShell — uses the bundled JDK and Maven
.\run.ps1
```

Or with an explicit Maven path:
```bash
mvn spring-boot:run
```

### 5. Open in browser
```
http://localhost:8080
```

---

## Running Tests

```bash
# Run all tests (uses H2 in-memory DB — no PostgreSQL required)
mvn test

# Run only unit tests
mvn test -Dtest=TaskServiceTest

# Run only repository integration tests
mvn test -Dtest=TaskRepositoryTest

# Run only MVC controller tests
mvn test -Dtest=TaskControllerTest

# Run with verbose output
mvn test -pl . --no-transfer-progress
```

**Expected output:**
```
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> Tests do **not** require a running PostgreSQL instance. H2 is used automatically.


---

## Usage Examples

### Creating a task
1. Click **New Task** in the top-right navbar
2. Fill in: Title (required), Description (optional), Due Date (required, past dates allowed), Priority, Status
3. Click **Create Task** → redirected to dashboard with a success message

### Filtering tasks
- Use the **Search** box to find tasks by title (partial, case-insensitive)
- Use the **Status** or **Priority** dropdowns to filter
- Click **Reset** to clear all filters

### Changing task status
- In the task table, use the **status dropdown** directly on each row — no page navigation needed
- Changing to **Done** immediately removes the task from the overdue count

### Overdue tasks
- A task is overdue when its due date is **before today** AND its status is **not Done**
- Overdue tasks show a red **Overdue** badge in the Due Date column and a highlighted row
- The **Overdue** stat card on the dashboard reflects the live count

---

## Project Structure

```
Task_Planner/
├── Dockerfile                          # Multi-stage Docker build for Render
├── render.yaml                         # Render IaC: web service + PostgreSQL
├── run.ps1                             # Local run helper (Windows PowerShell)
├── pom.xml                             # Spring Boot 3.3.4, Java 21
│
└── src/
    ├── main/
    │   ├── java/com/taskplanner/
    │   │   ├── TaskPlannerApplication.java   # Entry point
    │   │   ├── enums/
    │   │   │   ├── Priority.java             # LOW | MEDIUM | HIGH
    │   │   │   └── Status.java               # TODO | IN_PROGRESS | DONE
    │   │   ├── model/Task.java               # JPA entity (UUID PK, lifecycle hooks)
    │   │   ├── repository/TaskRepository.java # JPQL queries, countByStatus, countOverdue
    │   │   ├── service/TaskService.java       # Business logic & validation
    │   │   └── controller/
    │   │       ├── TaskController.java        # All MVC routes
    │   │       └── RootController.java        # / → /tasks redirect
    │   └── resources/
    │       ├── application.properties         # Env-var driven config
    │       ├── static/css/style.css           # Blue & white design system
    │       └── templates/
    │           ├── index.html                 # Dashboard (stats + table + filters)
    │           └── form.html                  # Create / edit form
    │
    └── test/
        ├── java/com/taskplanner/
        │   ├── service/TaskServiceTest.java       # Unit tests (Mockito)
        │   ├── repository/TaskRepositoryTest.java # Integration tests (H2)
        │   └── controller/TaskControllerTest.java # MVC slice tests (@WebMvcTest)
        └── resources/
            └── application.properties             # H2 config for tests
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| **UUID primary key** | Globally unique without a DB sequence; safe to generate in the application layer before persisting; avoids sequential ID enumeration |
| **`@PrePersist` / `@PreUpdate`** | Timestamps set automatically by JPA — no manual tracking needed in controllers or services |
| **`@Transient isOverdue()`** | Overdue logic lives on the entity so both the template and service can use it without a second query |
| **Nullable filter parameters in JPQL** | `(:status IS NULL OR t.status = :status)` — one query handles all combinations of optional filters |
| **Application-side ID generation** | ID is available before `save()` completes, useful for logging and responses |
| **`ddl-auto=update`** | Schema evolves automatically on new deployments without losing data; would use `validate` in a stricter prod environment |
| **H2 for tests** | Tests run without any external infrastructure; H2's PostgreSQL-compatibility mode exercises the same SQL |
| **Multi-stage Dockerfile** | Build stage uses Maven/JDK image; runtime stage uses a slim JRE-only image, reducing final image size significantly |
| **Environment variables with defaults** | Same `application.properties` works locally (defaults) and on Render (env vars injected) — no separate profiles needed |
| **Blue & white UI, no JS frameworks** | Vanilla CSS + minimal inline JS for the status dropdown — fast, zero dependencies, no build step for the frontend |

---

## Known Limitations

| Limitation | Notes |
|---|---|
| **No authentication** | All tasks are visible and editable by anyone — by design for this demo |
| **No pagination** | All tasks load at once; performance degrades with thousands of records |
| **Free tier cold starts** | Render free services sleep after 15 min inactivity; first request takes ~30s |
| **Free PostgreSQL expiry** | Render free databases expire after 90 days from creation |
| **Timezone is server-fixed** | `Africa/Harare` (CAT, UTC+2) is hardcoded; a multi-user system would need per-user timezone support |
| **No soft delete** | Deleting a task is permanent with no recovery |
| **Status is free-form** | No enforced workflow — a task can jump from TODO straight to DONE |

---

## Acceptance Scenario Verification

| Step | How it is met |
|---|---|
| Create two tasks, one with a past due date | No future-date constraint; past dates accepted |
| Overdue task appears in overdue count | `countOverdue` query: `dueDate < today AND status <> 'DONE'` |
| Mark past-due task Done → leaves overdue list | `isOverdue()` returns `false` when `status == DONE`; count updates immediately |
| Filter by priority | JPQL nullable filter on `priority` param |
| Restart app, records persist | PostgreSQL (Render or local) stores data durably; `ddl-auto=update` never drops tables |

---

## Timezone

All timestamps (`created_at`, `updated_at`) and overdue calculations use **Africa/Harare (CAT, UTC+2)**. Zimbabwe does not observe Daylight Saving Time, so the offset is always UTC+2 with no seasonal change.
