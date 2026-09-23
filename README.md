# AshokMart

AshokMart is a Java-based multi-vendor e-commerce web application. The repository currently contains the project foundation, the Commit 02 database schema foundation, and **Commit 03: H2 + HikariCP database infrastructure**. Authentication, registration/login, product CRUD, cart operations, checkout, reviews UI, and seller or administrator dashboards are intentionally deferred to later commits.

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

## Database infrastructure

The structural schema remains at `src/main/resources/schema.sql` and creates `users`, `categories`, `products`, `cart`, `cart_items`, `orders`, `order_items`, and `reviews`. It includes primary keys, foreign keys, unique constraints, role/status checks, numeric checks, and referential-integrity rules from Commit 02.

`DatabaseConnectionPool` owns one application-wide HikariCP datasource. Future DAOs can use `pool.getConnection()` in try-with-resources; closing that connection returns it to HikariCP. The pool is not recreated per request and is closed at web-application shutdown.

`DatabaseInitializer` reads the existing `schema.sql` resource and executes it through a pooled datasource. `DatabaseContextListener` creates the pool and initializes the schema once during application startup, stores the pool in the servlet context, and closes it during shutdown. No business logic is placed in the listener.

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

No production credentials are committed. Local file-based H2 artifacts such as `*.mv.db` and `*.trace.db` are ignored by Git. Automated tests use isolated in-memory H2 databases and never depend on the local database file.

## Logging

SLF4J with Logback records pool initialization, schema initialization, startup/shutdown, and failures. Database passwords and other secrets are never logged. Development logging is configured in `src/main/resources/logback.xml`.

## Local setup

1. Install Java 17 and Maven.
2. Clone this repository.
3. Run `mvn clean test` to verify the schema and database infrastructure.
4. Run `mvn package` to build the WAR.
5. For the web foundation, copy `target/AshokMart.war` to Tomcat 9's `webapps/` directory and open `http://localhost:8080/AshokMart/` after starting Tomcat.

At web-application startup, the listener initializes the configured H2 schema automatically. HikariCP pooling and schema initialization are infrastructure only; application business modules will use them in later commits.

## Maven commands

```bash
mvn clean test
mvn package
```

The generated WAR is written to `target/AshokMart.war` for Tomcat 9 or later Railway deployment through GitHub.
