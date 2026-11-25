# Real Google OAuth2 Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable real Gmail/Google OAuth2 login and registration (with `ROLE_LEARNER` / `ROLE_TUTOR` selection on `/register`) using Spring Security `oauth2Login()` and existing `AuthServiceImpl` user provisioning.

**Architecture:** Enable `.oauth2Login()` on `SecurityConfig.securityFilterChain` with a custom `OAuth2AuthorizationRequestResolver` (blocks placeholder `GOOGLE_CLIENT_ID` and saves whitelisted `role` to `HttpSession`), an `OAuth2AuthenticationSuccessHandler` (provisions/loads user via `AuthService.authenticateWithGoogle`, issues JWT + `access_token` cookie, redirects via relative `Location: /login?...`), and an `OAuth2AuthenticationFailureHandler`. Extract shared user+profile creation in `AuthServiceImpl` so OTP registration, password registration, and Google OAuth2 reuse one code path.

**Tech Stack:** Java 17, Spring Boot 3.3.4, Spring Security OAuth2 Client (`spring-boot-starter-oauth2-client`), JUnit 5, Mockito, MockMvc, Vanilla JS (`frontend/pages/register/register.js`, `frontend/pages/login/login.js`).

**Spec:** [2026-11-26-real-google-oauth2-login-design.md](file:///C:/Users/Administrator/Desktop/KizunaTutor/docs/superpowers/specs/2026-11-26-real-google-oauth2-login-design.md)

## Global Constraints

- Do not alter `users` or `learners` / `tutors` database tables.
- Keep `/api/auth/mock-google-login` disabled by default (`app.auth.mock-google.enabled:false`).
- Never commit real `GOOGLE_CLIENT_ID` or `GOOGLE_CLIENT_SECRET` into the repository; read from env vars `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `GOOGLE_REDIRECT_URI` (default `http://localhost:3000/login/oauth2/code/google`).
- Reject empty/null/placeholder `GOOGLE_CLIENT_ID` (`1029384756-kizunatutor-google-client.apps.googleusercontent.com`) by redirecting to `/login?error=oauth2_not_configured`.
- Only allow `ROLE_LEARNER` or `ROLE_TUTOR` for new Google OAuth2 accounts (whitelist; fallback to `ROLE_LEARNER`). Never overwrite the existing role of an already-registered user.
- Reject blocked/soft-deleted accounts (`deletedAt != null || accountStatus != ACTIVE`).
- Keep `restAuthErrorHandlers` returning `401`/`403` JSON for `/api/**` routes (never 302 redirect API calls to `/oauth2/authorization/google`).
- Keep `AuthServiceImpl` 9-argument constructor unchanged so existing unit tests instantiate cleanly.

## Review Focus

- `email_verified` is `false` or `email` attribute is blank/missing from Google OAuth2 user: redirect to `/login?error=email_not_provided` without creating a user.
- Proxy host mismatch (`frontend/server.js` proxies `localhost:3000` -> `127.0.0.1:8085` with `Host: 127.0.0.1:8085`): redirect handlers must emit a relative `Location` header (`/login?...`) via `response.setHeader("Location", ...)` rather than `sendRedirect` resolving to `:8085`.
- Existing user with `ROLE_ADMIN`, `ROLE_TUTOR`, or `ROLE_LEARNER` logs in via `/register` while a different radio role is checked: existing user keeps their original DB role.
- `fullName` > 100 chars or `avatarUrl` > 500 chars returned from Google: truncate safely to column lengths (`100` / `500`) and fall back to `"Học Viên Kizuna"` if `name` is blank.
- Unauthenticated `/api/**` request after enabling `.oauth2Login()`: still returns HTTP 401 JSON (`UNAUTHENTICATED`), not HTTP 302 redirect.

---

### Task 1: Add `AuthService.authenticateWithGoogle` with shared user provisioning

**Files:**
- Modify: `backend/src/main/java/com/kizuna/features/auth/service/AuthService.java:14-24`
- Modify: `backend/src/main/java/com/kizuna/features/auth/service/impl/AuthServiceImpl.java:256-450`
- Test: `backend/src/test/java/com/kizuna/features/auth/AuthServiceImplTest.java`

**Interfaces:**
- Consumes: `UserRepository`, `RoleRepository`, `LearnerRepository`, `TutorRepository`, `PasswordEncoder`, `JwtTokenProvider`, `LoginActivityRecorder`.
- Produces:
  - `LoginResponse AuthService.authenticateWithGoogle(String email, String fullName, String avatarUrl, String requestedRole)`
  - `String AuthServiceImpl.sanitizeOAuthRole(String requestedRole)` (package/public helper or reused inside `AuthServiceImpl`)

- [ ] **Step 1: Write the failing unit tests in `AuthServiceImplTest.java`**

Add nested test class `GoogleOAuthTests` in `AuthServiceImplTest.java` testing:
1. `authenticateWithGoogle_newLearner_createsUserAndLearner`: mocks `userRepository.findByEmail("new.learner@gmail.com")` -> empty, `roleRepository.findByRoleName("ROLE_LEARNER")` -> `ROLE_LEARNER`, verifies `userRepository.save` (ACTIVE, encoded random password, truncated avatar/name), `learnerRepository.save`, `tutorRepository` never saved, returns JWT & `redirectUrl = "/pages/home/"`.
2. `authenticateWithGoogle_newTutor_createsUserAndTutor`: `requestedRole = "ROLE_TUTOR"`, verifies `tutorRepository.save` with `TutorVerificationStatus.UNVERIFIED` & `TutorMembershipStatus.FREE`, `learnerRepository` never saved, returns `redirectUrl = "/tutor/dashboard"`.
3. `authenticateWithGoogle_disallowedRole_fallsBackToLearner`: `requestedRole = "ROLE_ADMIN"` -> queries `ROLE_LEARNER`, creates `Learner`, never assigns `ROLE_ADMIN`.
4. `authenticateWithGoogle_existingUser_keepsExistingRoleAndFillsBlankAvatar`: existing `ROLE_TUTOR` with `avatarUrl = null`, called with `requestedRole = "ROLE_LEARNER"` -> preserves `ROLE_TUTOR`, updates `avatarUrl`, saves user, does not recreate `Learner`/`Tutor`.
5. `authenticateWithGoogle_blockedOrDeletedUser_throws403BusinessException`: existing user with `UserAccountStatus.LOCKED` (and another with `deletedAt != null`) -> throws `BusinessException` with `status == 403`.
6. `authenticateWithGoogle_longNameAndAvatar_truncatesToColumnLimits`: 150-char name -> <= 100 chars; 600-char avatarUrl -> <= 500 chars; blank name -> `"Học Viên Kizuna"`.

- [ ] **Step 2: Run `AuthServiceImplTest` to verify it fails**

Run: `.\mvnw.cmd -f backend\pom.xml test -Dtest=AuthServiceImplTest`
Expected: FAIL compilation — `authenticateWithGoogle` does not exist on `AuthService`.

- [ ] **Step 3: Implement shared provisioning helper and `authenticateWithGoogle` in `AuthService` & `AuthServiceImpl`**

In `AuthService.java`:
- Add `LoginResponse authenticateWithGoogle(String email, String fullName, String avatarUrl, String requestedRole);`

In `AuthServiceImpl.java`:
- Extract `private User createUserWithProfile(String cleanEmail, String fullName, String encodedPassword, String avatarUrl, String roleName, String targetJlpt)` used by `completeRegistration`, `register`, and `authenticateWithGoogle` (avoiding duplicated `User` + `Tutor`/`Learner` builder blocks).
- Implement `@Override @Transactional public LoginResponse authenticateWithGoogle(String email, String fullName, String avatarUrl, String requestedRole)`:
  - Normalize `cleanEmail = email == null ? "" : email.trim().toLowerCase()`; throw `BadRequestException` if blank.
  - Whitelist `safeRole = ("ROLE_TUTOR".equals(requestedRole)) ? "ROLE_TUTOR" : "ROLE_LEARNER"`.
  - Truncate `safeName` (default `"Học Viên Kizuna"`, max 100 chars) and `safeAvatar` (null if blank, max 500 chars).
  - Lookup `userRepository.findByEmail(cleanEmail)`:
    - If present: check `user.getDeletedAt() != null || user.getAccountStatus() != UserAccountStatus.ACTIVE` -> throw `new BusinessException(403, "Tài khoản của bạn hiện đang bị tạm khóa/cấm. Vui lòng liên hệ bộ phận hỗ trợ.")`. If `user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()`, set `safeAvatar` and `userRepository.save(user)`. Keep existing `user.getRole()`.
    - If absent: lookup `roleRepository.findByRoleName(safeRole)` (fallback `ROLE_LEARNER`), call `createUserWithProfile(cleanEmail, safeName, passwordEncoder.encode(UUID.randomUUID().toString()), safeAvatar, role.getRoleName(), "N3")`.
  - Call `recordLogin(user)`, `tokenProvider.generateToken(user)`, build `LoginResponse` (same structure as `login`).

- [ ] **Step 4: Run `AuthServiceImplTest` to verify all tests pass**

Run: `.\mvnw.cmd -f backend\pom.xml test -Dtest=AuthServiceImplTest`
Expected: PASS (all existing tests + 6 new `GoogleOAuthTests`).

---

### Task 2: Add OAuth2 request resolver (placeholder guard + role session) and success/failure handlers

**Files:**
- Create: `backend/src/main/java/com/kizuna/features/auth/security/GoogleOAuth2AuthorizationRequestResolver.java`
- Create: `backend/src/main/java/com/kizuna/features/auth/security/GoogleOAuth2SuccessHandler.java`
- Create: `backend/src/main/java/com/kizuna/features/auth/security/GoogleOAuth2FailureHandler.java`
- Modify: `backend/src/main/java/com/kizuna/features/auth/controller/GoogleOAuthController.java:1-48`
- Test: `backend/src/test/java/com/kizuna/features/auth/security/GoogleOAuth2HandlersTest.java`

**Interfaces:**
- Consumes: `AuthService.authenticateWithGoogle(String, String, String, String)`, `ClientRegistrationRepository`, `OAuth2AuthorizationRequestResolver`.
- Produces:
  - Session attribute constant `GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE = "OAUTH2_SELECTED_ROLE"`
  - `GoogleOAuth2AuthorizationRequestResolver` (implements `OAuth2AuthorizationRequestResolver`; redirects `/login?error=oauth2_not_configured` if placeholder/blank Client ID, stores whitelisted `role` param in `HttpSession`, delegates to `DefaultOAuth2AuthorizationRequestResolver`)
  - `GoogleOAuth2SuccessHandler` (implements `AuthenticationSuccessHandler`)
  - `GoogleOAuth2FailureHandler` (implements `AuthenticationFailureHandler`)

- [ ] **Step 1: Write the failing tests in `GoogleOAuth2HandlersTest.java`**

Test:
1. `resolver_whenClientIdIsPlaceholder_redirectsToNotConfiguredAndReturnsNull`: mocks `ClientRegistrationRepository` with placeholder ID -> `resolve(request)` sets status 302 + `Location: /login?error=oauth2_not_configured` and returns `null`.
2. `resolver_whenValidClientIdAndTutorRole_storesTutorRoleInSession`: request `/oauth2/authorization/google?role=ROLE_TUTOR` -> `session.getAttribute("OAUTH2_SELECTED_ROLE")` is `"ROLE_TUTOR"` and returns non-null `OAuth2AuthorizationRequest`.
3. `resolver_whenAdminRoleRequested_storesLearnerRoleInSession`: request `/oauth2/authorization/google?role=ROLE_ADMIN` -> stores `"ROLE_LEARNER"`.
4. `successHandler_whenEmailMissingOrUnverified_redirectsEmailNotProvided`: `OAuth2User` missing `email` or `email_verified = false` -> `Location: /login?error=email_not_provided`.
5. `successHandler_whenValidGoogleUser_setsCookieAndRedirectsRelativeLoginUrl`: calls `authService.authenticateWithGoogle("hocvien@gmail.com", "Hoc Vien", "https://pic", "ROLE_TUTOR")`, verifies `access_token` HttpOnly cookie, clears `OAUTH2_SELECTED_ROLE` from session, and verifies `Location` starts with `/login?token=` (relative, never `127.0.0.1:8085`).
6. `successHandler_whenUserBlocked_redirectsAccountBlocked`: `authService.authenticateWithGoogle` throws `BusinessException(403, ...)` -> `Location: /login?error=account_blocked`.
7. `failureHandler_redirectsOAuth2Failure`: `onAuthenticationFailure` -> `Location: /login?error=oauth2_failure`.

- [ ] **Step 2: Run `GoogleOAuth2HandlersTest` to verify it fails**

Run: `.\mvnw.cmd -f backend\pom.xml test -Dtest=GoogleOAuth2HandlersTest`
Expected: FAIL compilation — handler/resolver classes do not exist yet.

- [ ] **Step 3: Implement resolver, success handler, failure handler, and update `GoogleOAuthController`**

- Create `GoogleOAuth2AuthorizationRequestResolver.java`:
  - Uses `DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")`.
  - When request matches `google` registration (either `resolve(request)` on `/oauth2/authorization/google` or `resolve(request, "google")`): checks `ClientRegistration.getClientId()`; if null/blank or equals `GoogleOAuthController.PLACEHOLDER_CLIENT_ID`, writes relative redirect `Location: /login?error=oauth2_not_configured` on `ServletRequestAttributes` / `HttpServletResponse`? Note: `OAuth2AuthorizationRequestRedirectFilter` calls `resolve(request)` — if `resolve` returns `null`, the filter passes down `chain.doFilter`! To cleanly redirect inside `GoogleOAuth2AuthorizationRequestResolver` OR handle via `GoogleOAuthController`, wait: if `resolve(request)` returns `null` when placeholder, the filter calls `chain.doFilter(request, response)`, which reaches `GoogleOAuthController.authorizeGoogle(...)` -> which ALREADY checks placeholder and redirects to `/login?error=oauth2_not_configured`! AND if `GoogleOAuth2AuthorizationRequestResolver` is tested standalone or sets redirect directly when response is available (`ServletRequestAttributes`) and `OAuth2AuthorizationRequestRedirectFilter` sees `response.isCommitted()`, OR even simpler: let `GoogleOAuth2AuthorizationRequestResolver` return `null` when placeholder/blank so `GoogleOAuthController` handles the redirect (preserving `GoogleOAuthControllerTest` 100% AND avoiding `response.isCommitted()` surprises in Spring filter chain)! Wait: let's verify what `GoogleOAuth2AuthorizationRequestResolver` should do in unit test #1: `resolve(request)` returns `null` when placeholder/blank! And `GoogleOAuthControllerTest` tests `/login?error=oauth2_not_configured` on `GoogleOAuthController`!
  - Let's make sure `GoogleOAuthController.authorizeGoogle` also uses relative `Location: /login?error=oauth2_not_configured`? Wait: `GoogleOAuthControllerTest` verifies `verify(response, times(1)).sendRedirect("/login?error=oauth2_not_configured")`! So keep `GoogleOAuthController.authorizeGoogle` calling `response.sendRedirect(...)` so `GoogleOAuthControllerTest` continues to pass untouched!
  - For `GoogleOAuth2AuthorizationRequestResolver`: when `clientId` is valid (not blank and not placeholder), read `request.getParameter("role")`, normalize via whitelist (`"ROLE_TUTOR"` else `"ROLE_LEARNER"`), store in `request.getSession(true).setAttribute(SESSION_SELECTED_ROLE, safeRole)`, and delegate to `defaultResolver`. When `clientId` is blank/placeholder, return `null` so `GoogleOAuthController` handles `/login?error=oauth2_not_configured`.
- Create `GoogleOAuth2SuccessHandler.java`:
  - Helper `redirectRelative(HttpServletResponse response, String location)` sets `response.setStatus(HttpServletResponse.SC_FOUND)` and `response.setHeader("Location", location)` (keeps relative path across `localhost:3000` proxy).
  - Extracts `email`, `email_verified`, `name`, `picture` from `OAuth2User`. Rejects missing/blank email or explicit `Boolean.FALSE.equals(emailVerified)` -> `/login?error=email_not_provided`.
  - Reads & removes `OAUTH2_SELECTED_ROLE` from `HttpSession` (if session exists).
  - Calls `authService.authenticateWithGoogle(...)`. Catches `BusinessException` (403 -> `/login?error=account_blocked`) and unexpected `Exception` (-> `/login?error=oauth2_process_error`).
  - Sets `access_token` HttpOnly cookie (`MaxAge=86400`, `Path=/`) and redirects to `/login?token=...&role=...&email=...&name=...` (with URL-encoded query params matching `login.js`).
- Create `GoogleOAuth2FailureHandler.java`:
  - Clears `OAUTH2_SELECTED_ROLE` from session if present and sets relative `Location: /login?error=oauth2_failure`.

- [ ] **Step 4: Run `GoogleOAuth2HandlersTest` and `GoogleOAuthControllerTest`**

Run: `.\mvnw.cmd -f backend\pom.xml test -Dtest=GoogleOAuth2HandlersTest,GoogleOAuthControllerTest`
Expected: PASS.

---

### Task 3: Wire `oauth2Login()` in `SecurityConfig` and `application.properties`

**Files:**
- Modify: `backend/src/main/java/com/kizuna/core/security/SecurityConfig.java:87-136`
- Modify: `backend/src/main/resources/application.properties:63-68`
- Test: `backend/src/test/java/com/kizuna/features/auth/security/SecurityConfigOAuth2Test.java`

**Interfaces:**
- Consumes: `GoogleOAuth2AuthorizationRequestResolver`, `GoogleOAuth2SuccessHandler`, `GoogleOAuth2FailureHandler`, `RestAuthErrorHandlers`.
- Produces: Active `.oauth2Login(...)` on `SecurityConfig.securityFilterChain`.

- [ ] **Step 1: Write failing Spring Security integration/MockMvc test in `SecurityConfigOAuth2Test.java`**

Using `@WebMvcTest` or `@SpringBootTest` / `MockMvc` (check how existing controller/security tests in `backend/src/test` are written first — match project pattern, mocking repositories/ClientRegistrationRepository without requiring live DB):
1. `apiEndpoint_unauthenticated_stillReturns401JsonNot302`: `GET /api/tutor/verification/status` without JWT -> status `401` and JSON `"code":"UNAUTHENTICATED"` (proves `.oauth2Login()` did NOT break REST API 401 behavior).
2. `oauth2AuthorizationGoogle_whenPlaceholder_redirectsNotConfigured`: `GET /oauth2/authorization/google` with default placeholder `client-id` -> status `302`, `Location` ends with `/login?error=oauth2_not_configured`.

- [ ] **Step 2: Run `SecurityConfigOAuth2Test` to verify it fails**

Run: `.\mvnw.cmd -f backend\pom.xml test -Dtest=SecurityConfigOAuth2Test`
Expected: FAIL (until `SecurityConfig` wires `.oauth2Login` or test aligns with wired beans).

- [ ] **Step 3: Update `SecurityConfig.java` and `application.properties`**

- In `application.properties`:
  - Update line 67: `spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:3000/login/oauth2/code/google}`
  - Add `app.auth.google-redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:3000/login/oauth2/code/google}`
- In `SecurityConfig.java`:
  - Add `ObjectProvider<ClientRegistrationRepository>` (or `ClientRegistrationRepository`), `GoogleOAuth2SuccessHandler`, `GoogleOAuth2FailureHandler` to `securityFilterChain` parameters (using `ObjectProvider` so existing `@WebMvcTest` slices that don't load OAuth2 auto-config won't fail to start!).
  - When `clientRegistrationRepository` is available, configure `.oauth2Login(oauth2 -> oauth2.authorizationEndpoint(auth -> auth.authorizationRequestResolver(new GoogleOAuth2AuthorizationRequestResolver(clientRegistrationRepository))).successHandler(googleOAuth2SuccessHandler).failureHandler(googleOAuth2FailureHandler))`.
  - Keep `.exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthErrorHandlers.authenticationEntryPoint()).accessDeniedHandler(restAuthErrorHandlers.accessDeniedHandler()))` unchanged so `/api/**` returns 401/403 JSON.

- [ ] **Step 4: Run `SecurityConfigOAuth2Test` and all existing auth/security tests**

Run: `.\mvnw.cmd -f backend\pom.xml test -Dtest="*Auth*Test,*OAuth*Test,*Security*Test"`
Expected: PASS.

---

### Task 4: Frontend role propagation on `/register` & `account_blocked` message on `/login`

**Files:**
- Modify: `frontend/pages/register/index.html:169-179`
- Modify: `frontend/pages/register/register.js:472-501`
- Modify: `frontend/pages/login/login.js:373-380`
- Test: `frontend/tests/google-oauth-role.test.mjs`

**Interfaces:**
- Consumes: `/oauth2/authorization/google?role=ROLE_LEARNER|ROLE_TUTOR` and `/login?error=account_blocked`.
- Produces: `syncGoogleRegisterHref` helper (exportable or testable logic) + UI message for `account_blocked`.

- [ ] **Step 1: Write failing Node test in `frontend/tests/google-oauth-role.test.mjs`**

Test using Node's `node:test` / `assert`:
1. `buildGoogleOAuthUrl('ROLE_TUTOR')` -> `'/oauth2/authorization/google?role=ROLE_TUTOR'`
2. `buildGoogleOAuthUrl('ROLE_LEARNER')` -> `'/oauth2/authorization/google?role=ROLE_LEARNER'`
3. `buildGoogleOAuthUrl('ROLE_ADMIN')` -> `'/oauth2/authorization/google?role=ROLE_LEARNER'`
4. `resolveLoginErrorMessage('account_blocked')` contains `'tạm khóa'` and `resolveLoginErrorMessage('oauth2_not_configured')` contains `'Client ID'`.

- [ ] **Step 2: Run Node test to verify it fails**

Run: `node --test frontend/tests/google-oauth-role.test.mjs`
Expected: FAIL — helper functions not exported/implemented yet.

- [ ] **Step 3: Update `register/index.html`, `register.js`, and `login.js`**

- In `frontend/pages/register/index.html`: add `id="btnGoogleRegister"` on the Google OAuth2 anchor (`<a id="btnGoogleRegister" href="/oauth2/authorization/google?role=ROLE_LEARNER" ...>`).
- In `frontend/pages/register/register.js`:
  - Add helper `buildGoogleOAuthUrl(role)` (exported, returning `/oauth2/authorization/google?role=${safeRole}` for `ROLE_LEARNER` | `ROLE_TUTOR`).
  - On DOMContentLoaded (after applying `?role=tutor` preselection), sync `#btnGoogleRegister.href` immediately and on every `change` event of `input[name="userRole"]` and on click.
  - Guard `document.addEventListener` cleanly if imported in Node (e.g., `if (typeof document !== 'undefined')` or keep pure helper in a small shared module if `register.js` imports browser `/js/main.js`). Note: importing `/js/main.js` in Node fails because `/js/...` is an absolute web path — so place `buildGoogleOAuthUrl` and `resolveLoginErrorMessage` in `frontend/js/auth-oauth-helpers.js` (pure ES module with zero DOM/web-root imports) and import it from both `register.js`, `login.js`, and `frontend/tests/google-oauth-role.test.mjs`!
- In `frontend/pages/login/login.js`:
  - Use `resolveLoginErrorMessage(error)` from `/js/auth-oauth-helpers.js`, adding `account_blocked` handling (`'Tài khoản của bạn hiện đang bị tạm khóa/cấm. Vui lòng liên hệ bộ phận hỗ trợ.'`).

- [ ] **Step 4: Run Node test and full backend auth test suite**

Run: `node --test frontend/tests/google-oauth-role.test.mjs` and `.\mvnw.cmd -f backend\pom.xml test -Dtest="*Auth*Test,*OAuth*Test,*Security*Test"`
Expected: PASS on both frontend Node test and backend Maven tests.

---

## Self-Review Check

- **Spec coverage**:
  - `GOOGLE_REDIRECT_URI` default `http://localhost:3000/login/oauth2/code/google` -> Task 3.
  - Placeholder guard (`oauth2_not_configured`) -> Task 2 & Task 3.
  - Role selection on `/register` (`ROLE_LEARNER` / `ROLE_TUTOR`) + whitelist + session -> Task 2 & Task 4.
  - Shared `User` + `Learner`/`Tutor` provisioning + existing user keeps role + blocked check + JWT + cookie + relative redirect -> Task 1 & Task 2.
  - API 401/403 JSON preserved -> Task 3.
  - Unit tests covering all Review Focus items -> Tasks 1–4.
- **Type & signature consistency**:
  - `AuthService.authenticateWithGoogle(String email, String fullName, String avatarUrl, String requestedRole)` matches across Tasks 1 & 2.
  - `GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE` matches across Task 2.
  - `buildGoogleOAuthUrl` & `resolveLoginErrorMessage` in `frontend/js/auth-oauth-helpers.js` match across Task 4.
