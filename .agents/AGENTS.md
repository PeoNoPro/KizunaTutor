# AGENTS.md — KizunaTutor Engineering Standards & AI Agent Guidelines

<!-- ===================================================================== -->
<!-- [CORE SYSTEM GUARDRAILS] NON-NEGOTIABLE PRINCIPLES                     -->
<!-- ===================================================================== -->

> ⛔ **CRITICAL SYSTEM GUARDRAILS (STRICT COMPLIANCE REQUIRED):**
> 
> 1. **ZERO INTERNAL LEAKS ON USER INTERFACE:**
>    - Never expose internal system details, technical exception names, stack traces, raw HTTP errors, database schema names, or raw enum codes (e.g., `ROLE_ADMIN`, `NullPointerException`, `BadCredentialsException`, `500 INTERNAL_SERVER_ERROR`, `user_id`) to the end-user interface.
>    - All user-facing labels, badges, alerts, toasts, and error messages MUST be presented in human-centric, natural Vietnamese. Technical IDs and status codes are strictly reserved for internal API communication and programmatic frontend logic.
>
> 2. **NO ALL-CAPS TEXT ON USER INTERFACE:**
>    - Never use ALL-CAPS text for alert messages, modal titles, toasts, empty state descriptions, or buttons.
>    - Use Sentence case for alerts/descriptions and Title Case for short buttons/headings. ALL-CAPS is permitted solely for standard international acronyms (e.g., `OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`, `JWT`).
>
> 3. **NO FAKE OR MOCK DATA IN PRODUCTION FLOWS:**
>    - Never inject hardcoded mock arrays, fake JSON structures, or fabricated data into controllers, services, or frontend JavaScript to simulate feature completion when an API is not connected.
>    - If data is absent, the system MUST render a dedicated, user-friendly Empty State. Testing fixtures (in `src/test/`) and explicitly configured database seeders (e.g., `KanjiDataSeeder`) are permitted and must be strictly isolated from application runtime logic.

<!-- ===================================================================== -->

---

## 1. Project Context & Scope

### 1.1 Project Information
- **Project Name:** KizunaTutor (Mã dự án: KZT)
- **Group:** SEP490_G6
- **Software Type:** Responsive Web Application (Desktop & Mobile Browser friendly).
- **Core Purpose:** KizunaTutor is a specialized web platform connecting Japanese language learners with tutors while providing comprehensive learning support tools (lesson management, homework assignments, assessments, and Kanji practice).

### 1.2 Target User Roles (Actors)
1. **Learner (`ROLE_LEARNER`):** Japanese language student seeking tutors, booking trial lessons, submitting homework, and practicing Kanji.
2. **Tutor (`ROLE_TUTOR`):** Japanese tutor managing schedules, teaching materials, grading homework, and managing membership subscriptions.
3. **Academic Officer (`ROLE_ACADEMIC_OFFICER`):** Staff responsible for verifying tutor credentials, certificates, and academic profiles.
4. **Customer Service Staff (`ROLE_CS_STAFF`):** Support staff resolving learner and tutor inquiries and managing support tickets.
5. **Manager (`ROLE_MANAGER`):** Operational manager handling escalated support tickets, dispute reviews, and operational reporting.
6. **Administrator (`ROLE_ADMIN`):** System administrator managing staff accounts, platform settings, and system-wide audits.

### 1.3 Core Functional Domains (Planned / Target Modules)
The application architecture is planned around 11 key functional domains:
1. `auth`: Authentication & Authorization (registration, OTP verification, login, password recovery).
2. `profile`: Learner & Tutor Profile Management.
3. `verification`: Tutor Onboarding & Credential Verification.
4. `tutor_search`: Tutor Search, Filtering & Connection Matching.
5. `booking`: Trial Lesson Booking & Learning Space Activation.
6. `classroom`: Learning Materials & Shared Workspace Management.
7. `progress`: Homework Assignments, Submissions & Progress Tracking.
8. `learning_support`: Kanji Practice, Handwriting Recognition, Flashcards & Reflex Games.
9. `payment`: Tutor Membership Subscriptions (VNPay gateway integration).
10. `ticket`: Support Ticket Management & Escalation.
11. `admin`: User Administration & Operational Reporting.

### 1.4 Business Boundaries & Scope Constraints
- **Platform Limitation:** Pure responsive web application. No native mobile applications (iOS/Android) are in scope.
- **Tuition & Contract Boundary:** Learners and tutors independently negotiate lesson fees, payment schedules, and learning agreements outside the platform. **KizunaTutor does NOT handle electronic tuition contracts, does NOT collect tuition fees, and does NOT disburse tutor wages.** Platform payment processing is strictly restricted to **Tutor Membership Subscriptions**.
- **AI Advisory Boundary:** AI features provide automated suggestions and evaluations for study materials and Kanji writing. AI NEVER acts as the final academic authority; tutors retain full pedagogical responsibility and must manually review and confirm grades.

---

## 2. Technology & Architecture

### 2.1 Technology Stack (Confirmed Baseline)
- **Backend:** Java (version 17+), Spring Boot (3.x), Spring Security 6, Spring Data JPA.
- **Frontend:** Vanilla HTML5, CSS3, Vanilla JavaScript (ES6+).
- **Database:** PostgreSQL (relational schema, native UUID, indexing).
- **Architecture Pattern:** Layered Architecture organized by Feature (Package-by-Feature / Vertical Slice).
- **Prohibited:** Do NOT introduce React, Angular, Vue, or heavy frontend SPA frameworks unless explicitly approved by the team.

### 2.2 Backend Package-by-Feature Structure
All backend code is organized into clean, modular feature slices under `com.kizuna`:

```text
com.kizuna
├── core/                               # Shared technical infrastructure ONLY
│   ├── config/                         # SecurityConfig, WebMvcConfig, OpenApiConfig
│   ├── security/                       # JwtFilter, TokenProvider, UserPrincipal
│   ├── exception/                      # GlobalExceptionHandler, Custom Exceptions
│   ├── entity/                         # BaseEntity, User, Role
│   ├── repository/                     # UserRepository, RoleRepository
│   ├── dto/                            # ApiResponse<T>, ErrorResponse
│   └── util/                           # DateUtils, SecurityContextHelper
│
└── features/                           # Independent Functional Features
    ├── auth/
    ├── booking/
    ├── classroom/
    ├── learning_support/
    ├── payment/
    ├── profile/
    ├── progress/
    ├── ticket/
    ├── tutor_search/
    └── verification/
```

#### Mandatory Feature Package Sub-Structure:
Inside each feature package (e.g., `com.kizuna.features.booking`), the following subpackages are **mandatory**:
- `controller/`: REST controllers and page controllers.
- `dto/`: Request DTOs (`*Request`), Response DTOs (`*Response`).
- `entity/`: Domain JPA entities specific to this feature.
- `repository/`: Spring Data JPA repository interfaces.
- `service/`: Service interfaces (`*Service`) and implementations (`*ServiceImpl`).

> **Rule:** Do NOT place `.gitkeep` files in packages that contain source code. Packages must hold legitimate Java classes or be omitted until developed.

### 2.3 Layer Responsibilities
1. **Controller Layer:** Handles HTTP requests, enforces input validation via `@Valid`, delegates execution to services, and packages responses into `ApiResponse<T>`. **Zero business logic is permitted in controllers.**
2. **Service Layer:** Houses all business rules, validation workflows, transaction boundaries (`@Transactional`), and cross-feature orchestration. Inter-feature communication MUST pass through public Service interfaces, never cross-repository calls.
3. **Repository Layer:** Encapsulates data access through Spring Data JPA interfaces.
4. **Entity Layer:** Pure domain models mapping database tables. Extends `BaseEntity` for auditing fields (`createdAt`, `updatedAt`).

### 2.4 Frontend Architecture
- **HTML:** Semantic structure, accessibility, and content rendering (`src/main/resources/templates/` or static HTML).
- **CSS:** Layout, Japanese-inspired visual styling, responsive design (`src/main/resources/static/css/`). No inline styles.
- **JavaScript:** Client-side event handling, DOM manipulation, asynchronous REST API communication (`src/main/resources/static/js/`). No inline script tags in HTML bodies.
- **Modularity:** Group frontend assets by feature (e.g., `css/features/booking.css`, `js/features/booking.js`). Extract shared styles into `css/common/` and reusable utilities into `js/common/`.

---

## 3. Naming Conventions

All naming across Java, Database, API, and Frontend must adhere strictly to these conventions:

### 3.1 Backend (Java)

| Component | Convention | Correct Example | Incorrect Example |
| :--- | :--- | :--- | :--- |
| **Package** | lowercase, snake_case for feature domain | `com.kizuna.features.tutor_search.service` | `com.kizuna.features.tutorSearch`, `com.kizuna.FE05` |
| **Class / Interface** | PascalCase | `TrialBookingService`, `SupportTicket` | `trialBookingService`, `trial_booking` |
| **Method** | camelCase, verb-first | `findTutorsBySkill()`, `confirmBooking()` | `FindTutors()`, `booking_confirm()` |
| **Variable / Field** | camelCase | `hourlyRate`, `totalLessonsCount` | `HourlyRate`, `hourly_rate` |
| **Constant** | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS`, `DEFAULT_PAGE_SIZE` | `maxLoginAttempts`, `MaxAttempts` |
| **Enum Type / Item** | PascalCase (Type) / UPPER_SNAKE_CASE (Item) | `UserStatus.ACTIVE`, `TicketPriority.HIGH` | `user_status.active`, `UserStatus.Active` |
| **Entity** | PascalCase, singular noun | `TrialBooking`, `TutorProfile` | `TrialBookings`, `tbl_booking` |
| **DTO Request** | PascalCase + `Request` | `CreateBookingRequest`, `UpdateProfileRequest` | `BookingReq`, `CreateBookingDto` |
| **DTO Response** | PascalCase + `Response` | `BookingDetailResponse`, `TutorSummaryResponse` | `BookingRes`, `TutorDataDTO` |
| **Controller** | PascalCase + `Controller` | `TrialBookingController` | `TrialBookingCtrl`, `BookingEndpoint` |
| **Service / Impl** | PascalCase + `Service` / `ServiceImpl` | `TrialBookingService`, `TrialBookingServiceImpl` | `BookingManager`, `IBookingService` |
| **Repository** | PascalCase + `Repository` | `TrialBookingRepository` | `TrialBookingDao`, `BookingRepoImpl` |
| **Exception** | PascalCase + `Exception` | `ResourceNotFoundException`, `InvalidTokenException` | `ResourceError`, `NotFoundExc` |
| **Configuration** | PascalCase + `Config` | `SecurityConfig`, `WebMvcConfig` | `SecurityConfigurationHelper` |

### 3.2 Database (PostgreSQL)

| Component | Convention | Correct Example | Incorrect Example |
| :--- | :--- | :--- | :--- |
| **Table** | snake_case, plural nouns | `users`, `trial_bookings`, `support_tickets` | `user`, `tbl_users`, `TrialBookings` |
| **Column** | snake_case | `user_id`, `created_at`, `hourly_rate` | `userId`, `CreatedAt`, `hourlyRate` |
| **Primary Key** | snake_case, `<singular_table>_id` (UUID) | `booking_id`, `ticket_id`, `user_id` | `id`, `ID`, `bookingId` |
| **Foreign Key** | snake_case, `<referenced_table_singular>_id` | `tutor_id`, `learner_id`, `role_id` | `fk_tutor`, `tutorRef` |
| **Index** | `idx_<table>_<column>` | `idx_users_email`, `idx_bookings_status` | `index1`, `users_email_idx` |
| **Unique Constraint**| `uq_<table>_<column>` | `uq_users_email`, `uq_roles_name` | `unique_email` |
| **Foreign Constraint**| `fk_<table>_<referenced_table>` | `fk_bookings_tutors`, `fk_tickets_users` | `booking_tutor_fk` |

### 3.3 RESTful API Endpoints

| Rule | Correct Example | Incorrect Example |
| :--- | :--- | :--- |
| **Resource Collection** (lowercase, plural, kebab-case) | `GET /api/v1/trial-bookings` | `GET /api/v1/getTrialBookings`, `GET /api/v1/trial_booking` |
| **Specific Resource Item** | `GET /api/v1/trial-bookings/{bookingId}` | `GET /api/v1/trial-bookings?id=123` |
| **State Transition / RPC Action** | `POST /api/v1/trial-bookings/{id}/confirm`<br>`POST /api/v1/support-tickets/{id}/resolve` | `POST /api/v1/confirmBooking`<br>`POST /api/v1/set-status-resolve` |
| **Sub-resource Collection** | `GET /api/v1/tutors/{tutorId}/reviews` | `GET /api/v1/tutor-reviews/{tutorId}` |
| **Query Parameters** (camelCase) | `GET /api/v1/tutors?jlptLevel=N2&minRating=4` | `GET /api/v1/tutors?jlpt_level=N2&min-rating=4` |

### 3.4 Frontend (HTML, CSS, JavaScript)

| Component | Convention | Correct Example | Incorrect Example |
| :--- | :--- | :--- | :--- |
| **HTML File** | lowercase, kebab-case | `tutor-profile.html`, `find-tutor.html` | `TutorProfile.html`, `tutor_profile.html` |
| **CSS File** | lowercase, kebab-case | `tutor-search.css`, `dashboard.css` | `TutorSearch.css` |
| **CSS Class** | lowercase, kebab-case | `.tutor-card`, `.btn-primary`, `.kanji-grid` | `.tutorCard`, `.btn_primary` |
| **HTML ID** | camelCase | `id="bookingModal"`, `id="kanjiCanvas"` | `id="booking-modal"`, `id="Kanji_Canvas"` |
| **Data Attribute** | lowercase, kebab-case | `data-tutor-id="123"`, `data-lesson-status="pending"` | `data-tutorId="123"` |
| **JavaScript File**| lowercase, kebab-case | `booking-service.js`, `kanji-canvas.js` | `BookingService.js` |
| **JS Function / Variable** | camelCase | `function calculateScore()`, `const tutorList` | `function CalculateScore()`, `let tutor_list` |
| **JS Constant** | UPPER_SNAKE_CASE | `const MAX_RETRY_COUNT = 3;` | `const maxRetryCount = 3;` |
| **JS Class** | PascalCase | `class KanjiDrawer { ... }` | `class kanjiDrawer { ... }` |

---

## 4. Coding Standards

### 4.1 General Clean Code Rules
- **Single Responsibility Principle (SRP):** Every class, service method, and JavaScript function must have one clear responsibility. Keep methods under 40 lines.
- **DRY (Don't Repeat Yourself):** Extract repeated logic into shared services or utilities.
- **KISS & YAGNI:** Do not introduce premature abstractions, unnecessary interfaces, or multi-layered wrapper patterns.
- **Clean Imports:** Remove unused imports, commented-out dead code, and temporary debugging statements before submitting work.
- **Secrets & Credentials:** NEVER hardcode secrets, passwords, or API keys in Java or JavaScript. Inject all values via environment variables through `application.properties`.

### 4.2 Backend Java & Spring Standards
- **Controller Purity:** Controllers must ONLY validate inputs (`@Valid`), call service methods, and return response envelopes (`ApiResponse<T>`). No repository calls or calculations in controllers.
- **Service Boundaries:** Annotate modifying operations with `@Transactional`. Ensure rollbacks on runtime exceptions.
- **Repository Queries:** Optimize queries with `JOIN FETCH` for `@ManyToOne` associations to eliminate Hibernate N+1 queries.
- **Exception Handling:** Handle all exceptions centrally via `@RestControllerAdvice` (`GlobalExceptionHandler`). Always translate exceptions into clean, user-friendly Vietnamese messages.
- **Validation:** Enforce validation rules on all Request DTOs using Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Min`, `@Max`).

### 4.3 Frontend Coding Rules
- **Semantic HTML:** Use proper tags (`<header>`, `<nav>`, `<main>`, `<section>`, `<article>`, `<aside>`, `<footer>`) instead of generic nested `<div>` structures.
- **No Inline Code:** Do not use inline `style="..."` or inline event handlers (`onclick="..."`). Attach listeners via `addEventListener` in external JavaScript files.
- **CSS Discipline:** Avoid `!important` unless overriding third-party utility styles. Utilize predictable CSS classes instead of deeply nested, brittle selectors (`div > ul > li > span`).
- **Modern JavaScript:**
  - Default to `const`. Use `let` only when reassignment is mandatory. Strictly avoid `var`.
  - Avoid polluting the global window object. Encapsulate logic in modules, namespaces, or self-executing functions.
  - Implement robust error handling around all `fetch()` calls and DOM lookups.
- **Responsive Layout:** Build fluid, adaptive web layouts supporting standard desktop, tablet, and mobile viewport resolutions.

---

## 5. Database Standards

- **Primary Keys:** Standardize on native UUID keys (`UUID PRIMARY KEY DEFAULT gen_random_uuid()`).
- **Audit Columns:** Every business table MUST include `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP` and `updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
- **Entity Mapping:**
  ```java
  @Entity
  @Table(name = "trial_bookings")
  @Getter
  @Setter
  @NoArgsConstructor
  public class TrialBooking extends BaseEntity {
      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      @Column(name = "booking_id", updatable = false, nullable = false)
      private UUID bookingId;
      // fields...
  }
  ```
- **PostgreSQL Enum Mapping:** When mapping native PostgreSQL enum types, explicitly apply Hibernate 6 `@JdbcType`:
  ```java
  @Enumerated(EnumType.STRING)
  @Column(name = "booking_status", columnDefinition = "booking_status_enum", nullable = false)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private BookingStatus status;
  ```
- **Foreign Keys:** Explicitly define foreign key constraints with indexed columns to maintain referential integrity.
- **Migration Discipline:** Schema modifications MUST be executed through controlled, incremental SQL migration scripts in `database/migrations/`. **Never execute `DROP ... CASCADE` or destructive queries on live databases.**

---

## 6. API Standards

### 6.1 Standard Response Envelope
All REST API endpoints MUST return responses wrapped in `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Đặt lịch học thử thành công.",
  "data": {
    "bookingId": "c3b8a1e2-9d3b-4c2d-8e4f-5a6b7c8d9e0f",
    "status": "PENDING"
  },
  "timestamp": "2026-10-31T18:00:00+07:00"
}
```

### 6.2 Standard Error Envelope
On errors or validation failures, return a structured error response:

```json
{
  "success": false,
  "message": "Dữ liệu gửi lên không hợp lệ.",
  "fieldErrors": [
    {
      "field": "lessonTime",
      "message": "Thời gian học phải diễn ra trong tương lai."
    }
  ],
  "timestamp": "2026-10-31T18:00:00+07:00"
}
```

### 6.3 Standard HTTP Status Codes
- `200 OK`: Request succeeded.
- `201 CREATED`: Resource created successfully.
- `204 NO_CONTENT`: Successful action with empty response body.
- `400 BAD_REQUEST`: Validation failed or invalid payload syntax.
- `401 UNAUTHORIZED`: Authentication required or invalid/expired token.
- `403 FORBIDDEN`: Insufficient role permissions for this resource.
- `404 NOT_FOUND`: Resource does not exist.
- `409 CONFLICT`: Business state conflict or optimistic lock clash.
- `500 INTERNAL_SERVER_ERROR`: Unhandled server exception (mapped to user-friendly message).

### 6.4 Client-Side API Communication
- Centralize all API requests in dedicated JavaScript service modules (e.g., `js/services/api-client.js`).
- Send the CSRF token via the `X-CSRF-TOKEN` header on all modifying requests (POST, PUT, DELETE).
- NEVER expose API keys, internal tokens, or database credentials in client-side code.

---

## 7. Business Rules (KizunaTutor Core)

AI Agents and developers must understand and strictly respect these business rules:
1. **Tutor Matching & Trial Lessons:**
   - Learners can only book trial lessons with tutors whose credential status is `APPROVED`.
   - After a trial lesson, a shared Learning Space is only activated after **both** learner and tutor confirm the continuation of lessons.
2. **Tuition & Payment Demarcation:**
   - Learners and tutors negotiate tuition and payment schedules directly outside the platform.
   - The platform never collects tuition, never generates binding electronic legal contracts, and never disburses tutor salaries.
   - Platform financial transactions are strictly limited to **Tutor Membership Subscriptions** (via VNPay).
3. **Homework & Progress Evaluation:**
   - Learners submit homework assignments and Kanji stroke exercises.
   - AI generates automated suggestions, rubric scores, and stroke feedback.
   - **Tutors retain final grading authority.** The tutor reviews, modifies, and confirms the final score and remarks.
4. **Scope Discipline:**
   - Do NOT invent unapproved features or arbitrary business states.
   - Always analyze cross-feature dependencies before modifying existing domain logic.

---

## 8. AI Integration Rules (Pedagogical & Technical)

For all AI-assisted features (homework assistance, Kanji handwriting analysis, stroke evaluation):
1. **Backend Isolation:** All AI model interactions must execute strictly on the backend. Frontend JavaScript must NEVER invoke AI APIs or store AI API keys.
2. **Schema & Score Validation:**
   - Every AI response MUST be parsed and validated against strict schema rules (e.g., rubric structure, score bounds 0–100) before persistence.
   - If AI returns invalid formatting or scores outside acceptable ranges, the payload must be rejected.
3. **Data Segregation:**
   - Store AI evaluation suggestions in distinct columns (`ai_suggested_score`, `ai_feedback`).
   - Store human tutor ratings in separate columns (`final_score`, `tutor_feedback`).
   - Never overwrite human-confirmed records with automated AI data.
4. **Graceful Fallback on Failure:**
   - If an AI API call fails, times out, or encounters quota exhaustion, the system must log the incident internally, display a friendly Vietnamese notification, and **seamlessly enable manual grading by the tutor**. AI failure must never block student progress.
5. **No Speculative Architecture:**
   - Do NOT introduce unconfirmed AI models or external microservices (e.g., standalone Agent Memory servers) unless officially requested and verified by the team. Keep the architecture lean: Spring Boot $\rightarrow$ Model Client $\rightarrow$ PostgreSQL.

---

## 9. Security Rules

1. **Defense in Depth:** Never trust client-side validation. Backend services MUST validate all inputs independently.
2. **Authentication & RBAC:** Protect every sensitive endpoint using role-based access control (`@PreAuthorize("hasRole('TUTOR')")`).
3. **UI Hiding is NOT Security:** Hiding a button or menu item in HTML/CSS does not constitute access control. Backend APIs must independently reject unauthorized requests with HTTP `403 FORBIDDEN`.
4. **Password Security:** Passwords must be hashed using strong one-way hashing (BCrypt) with appropriate work factors. Never log or store plain-text passwords.
5. **Data Protection:** Strip sensitive fields (passwords, internal hashes, private tokens) from all DTO responses before transmission.

---

## 10. Git & Teamwork Rules

1. **Branching Strategy:** Work exclusively on assigned feature branches (`feature/<feature-name>`). Never commit or push directly to `main` or `dev`.
2. **Strict Local-Only Execution:** **NEVER execute `git push` autonomously.** All pushes to remote repositories must be performed manually by the developer after reviewing changes.
3. **Respect Peer Code:** Do not modify, refactor, or delete code authored by teammates outside your assigned task scope.
4. **Minimal Blast Radius:** Limit modifications strictly to the files necessary for the task. Avoid broad, unsolicited reformatting that clutters git diffs.

---

## 11. Mandatory Operational Protocol for AI Agents

Whenever assigned a coding task, every AI Agent MUST execute the following 10-step protocol:

1. **Read AGENTS.md:** Review this entire specification to anchor rules, conventions, and architectural constraints.
2. **Assess Real Codebase State:** Check existing code to understand current implementation reality. Do not assume unbuilt features exist.
3. **Confirm Scope:** Identify the exact functional domain and boundary. Do not add out-of-scope features.
4. **Plan File Changes:** Determine the minimal set of files to create or modify.
5. **Adhere to Packaging:** For new feature slices, create the required 5 subpackages (`controller`, `dto`, `entity`, `repository`, `service`) and omit `.gitkeep`.
6. **Apply Naming Conventions:** Verify that every class, method, variable, table, endpoint, and CSS class follows Section 3.
7. **Ensure Error Resilience:** Implement central exception handling, user-friendly Vietnamese messages, and AI fallbacks where applicable.
8. **Verify Compilation:** Run `./mvnw test-compile` to verify zero syntax/compilation errors across production and test code.
9. **Verify Targeted Tests:** Execute relevant tests (`./mvnw test -Dtest=<TargetTest>`) when altering business logic to prevent regressions.
10. **Report Results:** Conclude by providing a concise summary of created/modified files, compilation verification results, and any technical decisions needing team confirmation.

---

## 12. Technical Decisions Requiring Team Confirmation

The following technical specifications are intentionally left open and require formal confirmation by the engineering team:
1. **Frontend Styling Approach:** Confirm whether to use pure custom CSS, Tailwind CSS CLI, or Bootstrap for utility classes and grid layout.
2. **Build & Test Profiles:** Confirm test database configuration (Dockerized PostgreSQL container vs dedicated test instance profile).
3. **AI Model & Provider:** Confirm the approved AI vendor (Google Gemini vs OpenAI), official model name (e.g., `gemini-1.5-flash`), and token quota limits.
4. **Payment Gateway Credentials:** Confirm VNPay sandbox parameters and IPN webhook callback URL mappings.
5. **Asset Storage:** Confirm whether uploaded files (avatars, homework documents) will be stored in PostgreSQL `bytea`, local filesystem storage, or cloud object storage (S3/Cloudinary).
