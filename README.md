# AshokMart

AshokMart is a Java-based multi-vendor e-commerce web application. The repository currently contains the project foundation, database schema and infrastructure, authentication, role-based authorization, product catalog UI, and **Commit 09: the buyer cart module**. Checkout, reviews UI, seller CRUD, and administrator product management are intentionally deferred to later commits.

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

The user-facing flow is available through `/register`, `/login`, and `/logout`. Registration and login use POST forms, validation errors are returned to the JSP views without exposing SQL details, and successful registration redirects to login with a success message. Successful login regenerates the session ID and stores only the safe `AuthenticationResult` in HTTP Session. Logout invalidates the session and redirects to the public landing page. Public registration has no role selector.

The JSP views are located under `src/main/webapp/WEB-INF/views/`. Shared session-aware navigation is in `includes/header.jsp`, authentication styling is in `src/main/webapp/css/auth.css`, and password confirmation uses minimal client-side JavaScript in `src/main/webapp/js/auth.js`. Server-side validation remains authoritative.

## Role-based authorization

Authentication establishes who is signed in; authorization determines whether that authenticated user may access a protected route. Commit 06 adds server-side `AuthenticationFilter` and `AuthorizationFilter` protection for these explicit route groups:

| Route group | Required role |
| --- | --- |
| `/buyer/*` | `BUYER` |
| `/seller/*` | `SELLER` |
| `/admin/*` | `ADMIN` |

There is no implicit role hierarchy. A buyer cannot access seller or admin routes, a seller cannot access admin routes, and an admin does not automatically receive buyer or seller permissions. Guests attempting to access a protected route are redirected to `/login`; authenticated users with the wrong role receive HTTP 403 and the safe `WEB-INF/views/error/403.jsp` page. The route namespaces are protected even though their business features will be implemented in later commits.

Home, login, registration, authentication resources, and public CSS/JavaScript/images remain public. Navigation visibility is only a convenience; the filters are the authoritative security boundary against manually typed URLs. `SecurityHeadersFilter` adds conservative `nosniff`, frame-denial, referrer-policy, and authenticated-page cache headers. This is a focused authorization layer, not a claim of complete production security hardening.

## Product catalog backend

The catalog layer includes `Category` and `Product` persistence models, `CategoryDaoImpl` and `ProductDaoImpl` JDBC implementations, and service-layer validation through `CategoryServiceImpl` and `ProductServiceImpl`. Public catalog queries return only enabled products and provide product details with category and seller names. Inactive products remain available only to future seller-owned management operations and are not exposed through public product lookup.

`ProductSearchCriteria` supports case-insensitive name/description search, category filtering, minimum and maximum price, in-stock filtering, and pagination. `ProductPage` returns products together with the current page, page size, total results, and total pages. Sorting is limited to the `ProductSort` whitelist (`NEWEST`, `PRICE_ASC`, `PRICE_DESC`, `NAME_ASC`, and `NAME_DESC`); trusted SQL fragments are selected in the DAO and user input is always bound as a prepared-statement parameter. Page numbers and sizes are normalized to safe bounds, and invalid price ranges are rejected.

The service provides product detail retrieval, seller-product lookup, and an ownership helper that compares the persisted product seller ID with the authenticated user ID. It does not trust a submitted seller ID and does not implement seller CRUD, stock deduction, ratings, or checkout. The schema adds focused indexes for product category, seller, enabled status, and price to support the catalog workload.

## Product catalog UI

The customer-facing catalog is available at `GET /products`, with query parameters `q`, `category`, `minPrice`, `maxPrice`, `stock`, `sort`, `page`, and `size`. `ProductCatalogServlet` parses and validates those parameters, loads categories through `CategoryService`, loads products through `ProductService`, and forwards to the JSTL `products.jsp` view. Search, filters, sorting, and pagination preserve their state through safe `c:url`/`c:param` links.

`GET /product?id=...` displays an active product detail page through `ProductDetailServlet`. Missing or inactive products receive a clean not-found state. The listing and detail views use the AshokMart responsive design system, accessible labels and focus states, real database image URLs when present, and a neutral placeholder when an image is unavailable. Stock quantities and prices are rendered from the backend. Buyers can submit the real **Add to cart** form; cart mutations remain server-authoritative. Ratings are reserved for the later reviews module.

## Buyer cart module

Commit 09 adds the database-backed buyer cart at `GET /cart`. State-changing operations use POST and redirect back to the cart with a flash message: `/cart/add`, `/cart/update`, `/cart/remove`, and `/cart/clear`. `CartDaoImpl` uses prepared statements against the existing `cart` and `cart_items` tables, prevents duplicate cart rows per user, and joins current product values for cart display.

`CartServiceImpl` derives the user ID from the authenticated session at the servlet boundary and never accepts a browser-supplied user ID. It validates active products, positive quantities, stock limits, and authenticated ownership. Prices, line totals, item counts, and subtotal are recalculated with `BigDecimal` from current database prices on every cart read. Buyer-only authentication and authorization filters protect `/cart` and all cart mutation routes. Checkout remains a disabled future action and is not implemented in this commit.

## Database infrastructure

The structural schema remains at `src/main/resources/schema.sql` and creates `users`, `categories`, `products`, `cart`, `cart_items`, `orders`, `order_items`, and `reviews`. It includes primary keys, foreign keys, unique constraints, role/status checks, numeric checks, referential-integrity rules, and focused catalog indexes.

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
3. Run `mvn clean test` to verify the schema, infrastructure, authentication, authorization, catalog, cart DAO/service behavior, cart mutations, totals, stock validation, and servlet flows.
4. Run `mvn package` to build the WAR.
5. Copy `target/AshokMart.war` to Tomcat 9's `webapps/` directory and open `http://localhost:8080/AshokMart/` after starting Tomcat.
6. Open `/products` from the public landing page to browse the catalog, or open `/register` and `/login` to exercise authentication.
7. Register/login as a buyer, open a product, add it to the cart, and use `/cart` to update, remove, or clear items.

At web-application startup, the listener initializes the configured H2 schema automatically. The catalog UI and buyer cart are available, while checkout, reviews, seller product CRUD, and other marketplace transaction modules are not yet implemented.

## Maven commands

```bash
mvn clean test
mvn package
```

The generated WAR is written to `target/AshokMart.war` for Tomcat 9 or later Railway deployment through GitHub.
