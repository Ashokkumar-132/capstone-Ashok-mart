# AshokMart

AshokMart is a Java-based multi-vendor e-commerce web application. The repository currently contains the project foundation, database schema and infrastructure, and **Commit 04: the authentication backend**. The polished authentication UI, authorization filters, product catalog, cart operations, checkout, reviews UI, and seller or administrator dashboards are intentionally deferred to later commits.

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

## Authentication backend

The backend now includes a database-backed `User` model and `UserRole` enum with `BUYER`, `SELLER`, and `ADMIN` values. `UserDaoImpl` uses prepared JDBC statements through the shared HikariCP pool for user creation, email and ID lookup, duplicate checks, and enabled-status updates.

`AuthServiceImpl` validates and normalizes registration data, hashes passwords with BCrypt, rejects duplicate emails, and assigns `BUYER` to every public registration. Public registration does not accept an arbitrary role and cannot create an administrator account. Login verifies BCrypt passwords, rejects disabled accounts, and returns a generic `Invalid email or password` failure for unknown emails or incorrect credentials.

The minimal backend endpoints are `/register`, `/login`, and `/logout`. `LoginServlet` uses HTTP Session and stores only an `AuthenticationResult` containing user ID, name, email, and role. Passwords and password hashes are never placed in session state. The login servlet changes the session ID when authentication succeeds to reduce session fixation risk. Final JSP screens and authorization filters belong to later commits.

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

## Logging

SLF4J with Logback records pool initialization, schema initialization, startup/shutdown, and failures. Database passwords, password hashes, session IDs, and other secrets are never logged. Development logging is configured in `src/main/resources/logback.xml`.

## Local setup

1. Install Java 17 and Maven.
2. Clone this repository.
3. Run `mvn clean test` to verify the schema, infrastructure, DAO, password, registration, and login behavior.
4. Run `mvn package` to build the WAR.
5. For the web foundation, copy `target/AshokMart.war` to Tomcat 9's `webapps/` directory and open `http://localhost:8080/AshokMart/` after starting Tomcat.

At web-application startup, the listener initializes the configured H2 schema automatically. The authentication backend is ready for the later authentication UI and authorization work, but those modules are not yet implemented.

## Maven commands

```bash
mvn clean test
mvn package
```

The generated WAR is written to `target/AshokMart.war` for Tomcat 9 or later Railway deployment through GitHub.
