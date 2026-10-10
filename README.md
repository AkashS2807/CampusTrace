# CampusTrace ? Campus Lost and Found Portal

A small Java internship project for HTGE Technologies Pvt. Ltd. Students register, sign in, report lost or found belongings, search campus listings, view their own items, and mark them resolved. Administrators can resolve any item, delete a selected listing, and view registered users. The original Spring MVC/Thymeleaf design is preserved.

## Technology and architecture

Java 17; Spring Boot 4.1.0; Spring MVC; Thymeleaf; Spring Security (BCrypt, session authentication, CSRF); Jakarta Validation; Spring Data JPA/Hibernate; MySQL. Maven wrapper 3.9.16 builds the application. H2, JUnit and MockMvc are used only for tests.

Browser ? Spring Security ? MVC controllers ? JPA repositories ? MySQL. Thymeleaf renders escaped HTML with the existing responsive CSS. Two entities use generated numeric IDs: `User` (name, unique email, BCrypt password, phone, USER/ADMIN role) and `Item` (name, description, LOST/FOUND type, OPEN/RESOLVED status, owner email/name, phone, optional location). Ownership remains an email string; no foreign key or cascading deletion was added. Email is immutable in this application's UI. A JPA association was deferred to avoid rewriting existing data, especially old administrator posts.

## Windows setup

Install JDK 17 and MySQL 8.0.16+ (with Workbench or the mysql client). Run commands from this project directory in PowerShell. Dependencies require internet access on the first build. The MySQL schema is validated at startup; the app never automatically deletes or migrates your database.

### Recommended: separate, empty report/demo database

Open `docs/schema-new.sql` in MySQL Workbench, review it, and execute it. It creates `campustrace_demo` separately from an existing `campustrace` database. It contains no user accounts or item records. CREATE IF NOT EXISTS does not migrate previously existing tables.

As a MySQL administrator, create a dedicated local database account using your own password:

```sql
CREATE USER 'campustrace_app'@'localhost' IDENTIFIED BY 'choose-a-unique-local-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON campustrace_demo.* TO 'campustrace_app'@'localhost';
```

Then set environment variables (example strings must be replaced):

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/campustrace_demo'
$env:DB_USERNAME = 'campustrace_app'
$env:DB_PASSWORD = 'your-local-database-password'
$env:ADMIN_INIT_ENABLED = 'true'
$env:ADMIN_EMAIL = 'admin@example.test'
$env:ADMIN_PASSWORD = 'your-unique-admin-password'
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Visit http://localhost:8080. The opt-in initializer creates only a missing administrator account, stores a BCrypt hash, and refuses to promote an existing student. If an administrator already exists, it leaves its password unchanged. After the first successful startup, stop with Ctrl+C, remove the initialization password, and restart:

```powershell
$env:ADMIN_INIT_ENABLED = 'false'
Remove-Item Env:ADMIN_PASSWORD
.\mvnw.cmd spring-boot:run
```

`.env.example` documents variables; `.env` is NOT automatically loaded. `COOKIE_SECURE=false` permits local HTTP. Set it to `true` when serving over HTTPS. Database URL defaults to the original `campustrace` database if DB_URL is omitted; always set DB_URL deliberately.

If this Windows environment resolves Maven's repository as `C:\.m2\repository`, use:

```powershell
.\mvnw.cmd "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean verify
.\mvnw.cmd "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" spring-boot:run
```

Tests use an isolated in-memory H2 database and do not touch MySQL. The packaged jar can also be run, with the same environment variables:

```powershell
java -jar target\campustrace-0.0.1-SNAPSHOT.jar
```

### Existing database and plain-text accounts

Back up existing data before any manual DDL. Review `docs/migration-existing.sql`, run its preflight queries, and stop if rows need attention. Verify actual table/column names using SHOW CREATE TABLE. The migration adds a role column and explicit field constraints; it does not normalize emails, change passwords, drop records, or reassign ownership. MySQL DDL commits immediately, so use a backup for recovery.

Legacy plain-text passwords are rejected. There is no permanent plain-text login fallback. Prefer the separate demo database and register new test accounts through the UI. If an old test account must be retained, explicitly reset only that account: locally generate a BCrypt hash with the same `BCryptPasswordEncoder`, then parameterize an UPDATE of `user.password` by the verified account ID. Obtain the data owner's approval before this reset. Do not paste passwords into reports, public SQL files, or commits. Do not use a blanket DELETE or DROP to reset your old database. Old administrator credentials were hardcoded rather than persisted; create a new administrator with the opt-in initializer.

## Routes and permissions

| Routes | Access and behavior |
| --- | --- |
| GET `/`, `/role`, `/userChoice`, `/newUser`, `/registeredUser`, `/admin`, `/css/**` | Public home, choices, registration/login forms, CSS |
| POST `/saveUser`, `/userLogin` | Public registration/login, valid CSRF token required |
| GET `/dashboard`, `/lostItem`, `/foundItem` | Signed-in users |
| POST `/saveLostItem`, `/saveFoundItem` | Signed-in users, validation and CSRF; owner derived from authentication |
| GET `/items`, `/search`, `/myPosts` | Signed-in users; keyword, type and status combine; own-items page keeps owner scope |
| POST `/resolve/{id}` | Owner or ADMIN, CSRF; OPEN ? RESOLVED; repeats are harmless |
| GET `/manageItems`, `/allUsers` | ADMIN only |
| POST `/deleteItem/{id}` | ADMIN only, CSRF; deletes exactly the selected item |
| POST `/logout` | Invalidates session, clears authentication and session cookie |

Missing item IDs return 404, unauthorized mutations return 403, invalid IDs/filters return 400. State-changing GET links were removed. The old POST `/adminLogin` route is removed; both login forms use `/userLogin`, and the stored account role controls authorization. Registration has no password-confirmation field in the existing form; it now redirects to sign-in after success.

## Dummy data and screenshot checklist

Register `Demo Student` / `student@example.test` and `Demo Helper` / `helper@example.test` using your own test passwords (8?72 UTF-8 bytes) and dummy phone `0000000000`. Do not use real personal data. Sign in as each user and submit:

| Item | Type | Description | Location |
| --- | --- | --- | --- |
| Blue Notebook | LOST | Blue cover with a mathematics label | Library |
| Steel Water Bottle | FOUND | Silver bottle with a green cap | Block A |
| Black Umbrella | LOST | Foldable umbrella with a blue strap | Canteen |

Resolve the notebook as its owner to demonstrate both statuses. Sign in with the configured administrator to inspect/manage the same listings. Accounts/data are created only by your explicit UI actions or opt-in administrator creation; no demo seed runs against your existing database.

Capture: home, registration, student login, report lost, report found, all-items search with combined filters, My Items, resolved badge, and administrator dashboard/manage-items/users pages. No screenshots or performance claims are fabricated.

## Verification and limitations

See `docs/AUDIT.md` for the findings and actual executed verification. Automated tests cover registration, credentials, legacy-password rejection, session rotation/logout, authentication boundaries, item persistence, combined search, ownership, admin operations, missing IDs, CSRF, escaped rendering, and template rendering. Browser appearance and a real MySQL startup must still be checked locally if MySQL is unavailable in the review environment. H2 does not prove MySQL migration compatibility.

The project intentionally has no account deletion, edit-email, password recovery, item-editing, claims/approvals, uploads, or pagination. Clicking a submit button once uses POST/redirect/GET; refreshing the resulting listing does not repeat the POST. Two independent valid submissions can still create two items, as in the original simple workflow.

## GitHub publication

This supplied workspace had no `.git` directory, so tracked-file and history checks were unavailable. The source contained hardcoded administrator and database credentials; both were removed. Rotate the original database password and any reused administrator password before publishing. `.gitignore` excludes `.env`, local config, IDE files, target, logs, dumps, and caches; it cannot untrack files already committed elsewhere.

Review everything before initializing/committing. Commands below are instructions only; no push was performed:

```powershell
git init
git status --short
git add .
git diff --cached --check
git diff --cached
git commit -m "Harden and polish CampusTrace workflows"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/campustrace.git
# Only after your explicit decision to publish:
git push -u origin main
```

If another checkout has history, inspect `git log --all -- src/main/resources/application.properties` and controller history without sharing secrets. Rotate exposed credentials first. Remove sensitive files/values from history using a reviewed `git filter-repo` procedure on a backup clone, coordinating with collaborators; do not force-push or overwrite history automatically. Avoid copying generated `target` content, database backups, or private IDE settings into GitHub.

## Internship report summary

CampusTrace is a server-rendered campus lost-and-found portal developed with Java 17, Spring Boot, MVC, Thymeleaf, JPA and MySQL. Students can register and authenticate, submit lost/found reports, combine keyword and status/type filters, and resolve their own reports. Administrators can view users and manage listings. BCrypt password hashing, session authentication, CSRF-protected forms, server-side validation and ownership checks protect the principal workflows. Its two-entity database and simple controller/repository structure retain the scope of a learning-based internship project.
