# CampusTrace audit and verification

Reviewed and updated on 10 October 2026 (Asia/Calcutta).

## Original behavior inspected before edits

Inspected the entire source tree, both controllers, both entities, both repositories, all 13 HTML templates including the shared layout, CSS, properties, Maven wrapper, POM, and the single original context-load test. No AGENTS.md was found in this workspace or the inspected parent project directories. This workspace was not a Git repository; there was no `.git` history or uncommitted-change list to inspect.

The POM specified Java 17 and Spring Boot 4.1.0. Existing dependencies were MVC, Thymeleaf, Data JPA, MySQL Connector/J and the Boot test starter. The original launch mechanism was Maven/Spring Boot plus an externally running MySQL database, using Hibernate `update`. The original test required MySQL and checked only application-context loading.

Already implemented in source: home/role selection; registration and student login; a separate administrator login; dashboard counts; lost/found submission; listings; type/status filtering; name search; My Posts; resolution; admin item deletion and user listing. Those source paths were present, but the original application was not declared runtime-verified. The resolution method already checked owner email or administrator role on the server. Administrator list/delete methods already checked the session role. Thymeleaf used escaped text, and the CSS already had mobile layout rules and horizontally scrollable tables.

## Confirmed findings and fixes

| Finding | Evidence in original source | Final change |
| --- | --- | --- |
| Plain-text passwords | Registration stored password unchanged; repository queried email/password together | BCrypt encoder and Spring Security authentication; legacy plain-text values are rejected |
| Embedded secrets | Administrator credentials in UserController and database password in properties | Removed; environment-based DB configuration and explicit missing-account administrator initializer |
| Missing CSRF protection | No security dependency or CSRF tokens; mutations used GET | Spring Security with CSRF enabled; Thymeleaf POST forms for registration, login, reporting, resolve, delete and logout |
| Session fixation exposure | Existing session acquired authentication attributes without rotating its ID | Spring Security session authentication; ID rotation and logout invalidation tested |
| Duplicate-account check inconsistency | Raw email checked before trim/lowercase save | Normalize form input first, case-insensitive lookup and DB unique constraint; duplicate race handled without hiding unrelated persistence errors |
| Missing server validation | Registration and item fields accepted unvalidated request parameters | Dedicated validated form objects; nonblank fields, email format, phone digits, lengths, BCrypt byte limit |
| Weak error feedback | Missing/unauthorized item operations silently redirected; item submission had no messages | 400/403/404 as appropriate, friendly error template, success/validation messages and preserved safe input |
| Search/filter split | Name-only `/search`; separate `/items` forms; combined filtering loaded all records | One parameterized JPA query for case-insensitive name/description search, type, status and optional owner |
| Nullable/undefined values | Entity fields had few column constraints; strings unrestricted | Explicit nullability/lengths, role/type/status validation; OPEN?RESOLVED workflow; manual schema scripts |
| Windows wrapper failure | PowerShell indexed `Target[0]` when Target was null | Null-safe target handling |
| Publication/setup gaps | No README/example configuration; secrets were in source | README, audit, configuration example, SQL setup/migration and expanded ignore rules |

The review did **not** confirm a pre-existing object-level authorization bypass in resolve: its owner/admin check was already present. That protection is retained and now tested. No SQL-injection or unescaped-HTML vulnerability was found in the inspected queries/templates. Previously persisted invalid statuses, orphaned owner emails, duplicate differently-cased emails, and the actual MySQL schema are potential data issues, not confirmed database findings. The manual migration preflight exists for that reason.

## Important files changed

- `pom.xml`: security and validation; web/security test tools; H2 confined to test scope.
- `controller/UserController.java`: validated registration, normalization, secure password storage, duplicate handling; custom password/session checks removed.
- `controller/HomeController.java`: shared authenticated reporting logic, combined listing query, safe transitions, transactional item mutation, meaningful error statuses.
- `config/SecurityConfig.java`: route rules, DB-backed account roles, login/logout, BCrypt.
- `config/AdminInitializer.java`: opt-in administrator creation; no promotion or overwrite of existing accounts.
- `web/RegistrationForm.java`, `web/ItemForm.java`: separate input validation without binding user-controlled ownership/roles/status to entities.
- Entities/repositories: lengths/nullability, USER/ADMIN role, allowed type/status validation and combined listing query.
- Properties: environment credentials, explicit schema validation, session timeout/cookie settings, no verbose SQL setting.
- Existing templates/layout/CSS: protected POST actions, safe retained form input, combined filters, My Items naming, alerts, accessible labels, narrow-screen/reduced-motion rules, local font fallback; new friendly error template.
- `mvnw.cmd`: Windows wrapper fix.
- Test sources/resources, README, `.gitignore`, `.env.example`, `docs/schema-new.sql`, `docs/migration-existing.sql`, this audit.

## Executed verification

Both a clean build and the final package verification succeeded:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\Users\AKASH SURESH\.m2\repository' clean verify
.\mvnw.cmd '-Dmaven.repo.local=C:\Users\AKASH SURESH\.m2\repository' verify -B -ntp
```

Final result: **BUILD SUCCESS; 26 tests, 0 failures, 0 errors, 0 skipped**. Application tests: 21; administrator initialization tests: 5. Artifact: `target/campustrace-0.0.1-SNAPSHOT.jar` (executable Spring Boot jar).

The automated suite verifies:

- New registration, normalized email, secure hash and USER-only default role.
- Duplicate email rejection and invalid input with safe retained fields; passwords are not echoed into form inputs.
- Correct login, incorrect/missing credentials, rejection of legacy plain-text passwords.
- Session ID rotation, authenticated dashboard, logout invalidation and protected-page redirects.
- Authenticated lost/found submissions, correct persisted types/OPEN status, ignoring spoofed owner/status parameters; invalid reports do not persist.
- Case-insensitive name/description matching; combined type/status filters; empty/no-result search; My Items owner scope; resolved filtering.
- Owner resolution, harmless repeated resolution, forbidden cross-user resolution/deletion.
- Normal-user denial of administrator pages, actual persisted ADMIN role after login, administrator resolve/delete, preservation of unrelated items.
- Missing IDs (404), malformed IDs/invalid filters (400), mutations without CSRF (403), and prevention of state-changing GETs.
- Public/protected template rendering, correct lost/found forms, generated CSRF fields and escaped item content.
- Embedded Tomcat startup on a random TCP port and an actual HTTP login-page request, including its CSRF field and `nosniff` header.
- Disabled admin initialization does nothing; enabled initialization creates a hashed ADMIN; existing students cannot be promoted; existing administrators are unchanged; invalid setup is rejected.

Earlier runs exposed a stale IDE-produced class file and one path-detection problem in the test environment; clean rebuild and context-aware request-URI detection corrected them. Final reported results are from the successful run, not those earlier failures.

A source scan found no remaining old hardcoded credential patterns, plaintext credential-query method, `th:utext`, or CSRF-disable call. This is a targeted source review, not a claim that every possible secret is absent. Git tracked-file/history review could not execute because the supplied folder has no Git repository. No commit, push, history rewrite, or remote publication was performed.

## Verification limits and remaining local steps

The machine has an installed MySQL Server 9.0 directory, and localhost TCP port 3306 was reachable. No DB_URL/DB_USERNAME/DB_PASSWORD environment variables were configured for a verified connection. The review did not connect to the existing database, execute either SQL script, migrate passwords, or modify existing MySQL records. MySQL schema compatibility, SQL-script execution, and a MySQL-backed application startup remain unverified. Automated persistence tests used isolated H2 in MySQL compatibility mode; that does not substitute for MySQL migration testing.

No interactive browser session or screenshots were produced. Functional requests, rendered forms, navigation targets and persistence were exercised by automated tests; visual appearance on actual desktop/mobile browsers and the screenshot capture remain local manual checks. No claims are made about performance, accessibility certification or production deployment.

Use README's separate demo schema, configure environment variables, create the opt-in administrator once, and register dummy users through the UI. Follow its screenshot checklist and review the SQL before executing it. Existing passwords require explicit reset or separate recreated test accounts; no compatibility fallback was introduced. Rotate the original exposed database password and any reused administrator password.

## Deliberately preserved scope

The application remains Spring Boot + MVC + Thymeleaf + JPA + MySQL with the original recognizable design. Ownership remains the existing immutable owner-email string, with no foreign key/cascade redesign. No account deletion existed and none was added; therefore there is no new user-deletion route. No REST/React rewrite, Docker, cloud service, uploads, claim approvals or major dashboard redesign was introduced. POST/redirect/GET prevents refresh resubmission; independent repeated valid submissions are still independent reports. A persisted owner association, pagination and password recovery are future work rather than necessary additions to this internship pass.

README contains the verified feature list, architecture/entity descriptions, exact Windows run commands, safe configuration, administrator/test-account setup, Git review/commit/push instructions, screenshot checklist and a report-ready technical summary.

## Reference checked during implementation

Spring Security's official [form-login documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html) and [CSRF documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) describe the session-based login and Thymeleaf form-token integration used here. The existing Java/Boot versions were preserved and exercised by the build.
