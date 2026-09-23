# AshokMart

AshokMart is a Java-based multi-vendor e-commerce web application. The repository currently contains the project foundation, database schema and infrastructure, authentication backend, and **Commit 05: the authentication UI and complete authentication flow**. Authorization filters, product catalog, cart operations, checkout, reviews UI, and seller or administrator dashboards are intentionally deferred to later commits.

## Technology stack

- Java 17
- Maven
- Apache Tomcat 9
- Java Servlets, JSP, and JSTL
- JDBC with HikariCP
- H2 Database
- Gson and jBCrypt
- JUnit 5 and Mockito
- SLF4J and Logback
- HTML5, CSS3, and vanilla JavaScript

The application uses the Tomcat 9-compatible Java EE 8 `javax.*` web APIs. It does not use Spring, a JavaScript framework, JPA/Hibernate, or MongoDB.

## Architecture

The intended layered request flow is:

```text
Browser
  ↓
JSP + JSTL + HTML + CSS + JavaScript
  ↓
Java Servlets
  ↓
Service Layer
  ↓
DAO Layer
  ↓
JDBC
  ↓
HikariCP
  ↓
H2 Database
```

## Authentication flow

The backend includes a database-backed `User` model and `UserRole` enum with `BUYER`, `SELLER`, and `ADMIN` values. `UserDaoImpl` uses prepared JDBC statements through the shared HikariCP pool. `AuthServiceImpl` validates and normalizes registration data, hashes passwords with BCrypt, rejects duplicate emails, and assigns `BUYER` to every public registration.

The user-facing flow is available through `/register`, `/login`, and `/logout`. Registration and login use POST forms, validation errors are returned to the JSP views without exposing SQL details, and successful registration redirects to login with a success message. Successful login regenerates the session ID and stores only the safe `AuthenticationResult` in HTTP Session. Logout invalidates the session and redirects to the public landing page. Public registration has no role selector, and authorization filters will be added in a later commit.

The JSP views are located under `src/main/webapp/WEB-INF/views/`. Shared session-aware navigation is in `includes/header.jsp`, authentication styling is in `src/main/webapp/css/auth.css`, and password confirmation uses minimal client-side JavaScript in `src/main/webapp/js/auth.js`. Server-side validation remains authoritative.

## Database infrastructure

The structural schema remains at `src/main/resources/schema.sql` and creates `users`, `categories`, `products`, `cart`, `cart_items`, `orders`, `order_items`, and `reviews`. It includes primary keys, foreign keys, unique constraints, role/status checks, numeric checks, and referential-integrity rules.

`DatabaseConnectionPool` owns one application-wide HikariCP datasource. Future DAOs use `pool.getConnection()` in try-with-resources; closing that connection returns it to HikariCP. `DatabaseInitializer` reads the existing `schema.sql` resource and executes it through the datasource. `DatabaseContextListener` creates the pool and initializes the schema once during application startup, then closes the pool during shutdown.

## Configuration

`DatabaseConfig` loads system properties first, then environment variables, then safe local defaults. Supported settings are:

| Setting | System property | Environment variable | Default |
| --- | --- | --- | --- |
| JDBC URL | `ashokmart.db.url` | `ASHOKMART_DB_URL` | `jdbc:h2:./data/ashokmart;DB_CLOSE_ON_EXIT=FALSE` |
| Username | `ashokmart.db.user` | `ASHOKMART_DB_USER` | `sa` |
| Password | `ashokmart.db.password` | `ASHOKMART_DB_PASSWORD` | empty |
| Maximum pool size | `ashokmart.db.maxPoolSize` | `ASHOKMART_DB_MAX_POOL_SIZE` | `10` |
| Minimum idle | `ashokmart.db.minIdle` | `ASHOKMART_DB_MIN_IDLE` | `2` |
| Connection timeout | `ashokmart.db.connectionTimeoutMs` | `ASHOKMART_DB_CONNECTION_TIMEOUT_MS` | `30000` ms |

No production credentials are committed. Local H2 database artifacts and `target/` are ignored by Git. Automated tests use isolated in-memory H2 databases and never depend on a local database file.

## Logging and security

SLF4J with Logback records pool initialization, schema initialization, startup/shutdown, and failures. Database passwords, password hashes, session IDs, and other secrets are never logged or placed in JSP/session state. Credential submissions use POST, logout uses POST, and login session ID regeneration protects against session fixation.

## Local setup

1. Install Java 17 and Maven.
2. Clone this repository.
3. Run `mvn clean test` to verify the schema, infrastructure, DAO, password, registration, login, and existing authentication behavior.
4. Run `mvn package` to build the WAR.
5. Copy `target/AshokMart.war` to Tomcat 9's `webapps/` directory and open `http://localhost:8080/AshokMart/` after starting Tomcat.
6. Open `/register` or `/login` from the public landing page to exercise the authentication flow.

At web-application startup, the listener initializes the configured H2 schema automatically. Product and other marketplace modules are not yet implemented.

## Maven commands

```bash
mvn clean test
mvn package
```

The generated WAR is written to `target/AshokMart.war` for Tomcat 9 or later Railway deployment through GitHub.
