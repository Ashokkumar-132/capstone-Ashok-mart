# AshokMart

AshokMart is a Java-based multi-vendor e-commerce web application. The repository currently contains the project foundation and **Commit 02: the database schema foundation**. Authentication, product CRUD, cart services, checkout, orders workflow, reviews UI, and seller or administrator dashboards are intentionally not implemented yet.

## Technology stack

- Java 17
- Maven
- Apache Tomcat 9
- Java Servlets, JSP, and JSTL
- JDBC with HikariCP (pooling is reserved for a later infrastructure commit)
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

## Database foundation

The structural H2 schema is located at `src/main/resources/schema.sql`. It creates the following entities:

- `users`
- `categories`
- `products`
- `cart`
- `cart_items`
- `orders`
- `order_items`
- `reviews`

The schema includes primary keys, foreign keys, unique constraints, sensible not-null constraints, non-negative price and monetary checks, positive quantity checks, supported user roles, supported order statuses, review rating bounds, and duplicate prevention for cart products and buyer/product reviews. Foreign keys preserve historical order information rather than casually cascading deletes. Cart items may be removed with their cart, and a user's cart is removed with that user.

No fake marketplace or production seed data is included.

## Schema initialization

`com.ashokmart.util.SchemaInitializer` reads `schema.sql` from the classpath and executes it with a plain JDBC connection. The no-argument initializer uses the local development/test configuration in `DatabaseConfig`:

```text
ASHOKMART_DB_URL      default: jdbc:h2:mem:ashokmart_dev;DB_CLOSE_DELAY=-1
ASHOKMART_DB_USER     default: sa
ASHOKMART_DB_PASSWORD default: empty
```

These environment variables are intended for local or future deployment configuration; no production credentials are stored in the repository. HikariCP pooling and DAO/service integration will be added in later commits.

## Local setup

1. Install Java 17 and Maven.
2. Clone this repository.
3. Run the schema tests and package the application with the commands below.
4. For the web foundation, copy `target/AshokMart.war` to Tomcat 9's `webapps/` directory and open `http://localhost:8080/AshokMart/` after starting Tomcat.

## Maven commands

```bash
mvn clean test
mvn package
```

The schema tests use isolated in-memory H2 databases and verify table creation, key relationships, uniqueness constraints, numeric checks, order status validation, quantity validation, and review rating rules. The generated WAR is written to `target/AshokMart.war` for later Tomcat or Railway deployment.
