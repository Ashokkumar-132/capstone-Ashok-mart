# AshokMart

AshokMart is a Java-based multi-vendor e-commerce web application. The repository currently contains the project foundation, database schema and infrastructure, authentication, role-based authorization, product catalog UI, buyer cart, transactional buyer checkout, buyer order history, seller dashboard/product CRUD, and **Commit 13: seller order management and status**. Reviews UI and administrator product management are intentionally deferred to later commits.

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
| `/cart/*`, `/checkout`, `/order-confirmation`, `/orders/*` | `BUYER` |

There is no implicit role hierarchy. A buyer cannot access seller or admin routes, a seller cannot access admin routes, and an admin does not automatically receive buyer or seller permissions. Guests attempting to access a protected route are redirected to `/login`; authenticated users with the wrong role receive HTTP 403 and the safe `WEB-INF/views/error/403.jsp` page. The route namespaces are protected even though their business features will be implemented in later commits.

Home, login, registration, authentication resources, and public CSS/JavaScript/images remain public. Navigation visibility is only a convenience; the filters are the authoritative security boundary against manually typed URLs. `SecurityHeadersFilter` adds conservative `nosniff`, frame-denial, referrer-policy, and authenticated-page cache headers. This is a focused authorization layer, not a claim of complete production security hardening.

## Seller dashboard and product management

Commit 12 adds the seller workspace at `/seller/dashboard` and the owned product list at `/seller/products`. The dashboard displays seller-scoped product, active-product, and out-of-stock counts with quick actions. Sellers can create products at `/seller/products/new`, edit their own products, and activate or deactivate them through POST-only mutations. Seller navigation is visible only to authenticated `SELLER` users.

Seller ownership is enforced at every layer. The authenticated seller ID is derived from the session; no form or URL seller ID is trusted. DAO reads and writes use ownership-qualified SQL such as `WHERE id = ? AND seller_id = ?`, and cross-seller edits or status changes return safe not-found/error behavior without modifying the other seller's product. Server validation covers name, description, category existence, non-negative `BigDecimal` price with at most two decimal places, non-negative stock, and HTTP(S) or application-path image values.

Hard deletion is intentionally not exposed because the existing foreign keys from cart items, order items, and reviews protect active and historical product references. Deactivation is the compatible lifecycle operation, so historical orders remain intact while deactivated products disappear from the buyer catalog. The seller product list uses JSTL, responsive styling, PRG redirects, success/error messages, and mobile-friendly create/edit forms.

## Seller order management

Commit 13 adds seller-scoped order management at `GET /seller/orders`, seller-owned detail views at `GET /seller/orders/view?id=...`, and POST-only status updates at `/seller/orders/status`. DAO queries join `orders`, `order_items`, and `products` and verify both the persisted order-item seller and product seller against the authenticated session seller. A seller sees only orders containing that seller's products, only that seller's line items, and a seller-specific subtotal calculated from stored historical `order_items.subtotal` values.

The existing schema stores fulfillment status only on `orders`, not on `order_items`. Accordingly, status updates are permitted only when the seller owns at least one item in the order, but the resulting shared status is visible to the buyer for the full order. The service uses conservative transitions: `PENDING → CONFIRMED/CANCELLED`, `CONFIRMED → PROCESSING/CANCELLED`, `PROCESSING → SHIPPED/CANCELLED`, and `SHIPPED → DELIVERED`; delivered and cancelled orders are terminal. This limitation is displayed in the seller detail view rather than presenting a false claim of independent per-seller fulfillment.

## Product catalog backend

The catalog layer includes `Category` and `Product` persistence models, `CategoryDaoImpl` and `ProductDaoImpl` JDBC implementations, and service-layer validation through `CategoryServiceImpl` and `ProductServiceImpl`. Public catalog queries return only enabled products and provide product details with category and seller names. Inactive products remain available only to future seller-owned management operations and are not exposed through public product lookup.

`ProductSearchCriteria` supports case-insensitive name/description search, category filtering, minimum and maximum price, in-stock filtering, and pagination. `ProductPage` returns products together with the current page, page size, total results, and total pages. Sorting is limited to the `ProductSort` whitelist (`NEWEST`, `PRICE_ASC`, `PRICE_DESC`, `NAME_ASC`, and `NAME_DESC`); trusted SQL fragments are selected in the DAO and user input is always bound as a prepared-statement parameter. Page numbers and sizes are normalized to safe bounds, and invalid price ranges are rejected.

The service provides product detail retrieval, seller-product lookup, and an ownership helper that compares the persisted product seller ID with the authenticated user ID. The same product infrastructure also powers seller CRUD, while stock deduction and ratings remain separate modules. The schema adds focused indexes for product category, seller, enabled status, and price to support the catalog workload.

## Product catalog UI

The customer-facing catalog is available at `GET /products`, with query parameters `q`, `category`, `minPrice`, `maxPrice`, `stock`, `sort`, `page`, and `size`. `ProductCatalogServlet` parses and validates those parameters, loads categories through `CategoryService`, loads products through `ProductService`, and forwards to the JSTL `products.jsp` view. Search, filters, sorting, and pagination preserve their state through safe `c:url`/`c:param` links.

`GET /product?id=...` displays an active product detail page through `ProductDetailServlet`. Missing or inactive products receive a clean not-found state. The listing and detail views use the AshokMart responsive design system, accessible labels and focus states, real database image URLs when present, and a neutral placeholder when an image is unavailable. Stock quantities and prices are rendered from the backend. Buyers can submit the real **Add to cart** form; cart mutations remain server-authoritative. Ratings are reserved for the later reviews module.

## Buyer cart module

Commit 09 adds the database-backed buyer cart at `GET /cart`. State-changing operations use POST and redirect back to the cart with a flash message: `/cart/add`, `/cart/update`, `/cart/remove`, and `/cart/clear`. `CartDaoImpl` uses prepared statements against the existing `cart` and `cart_items` tables, prevents duplicate cart rows per user, and joins current product values for cart display.

`CartServiceImpl` derives the user ID from the authenticated session at the servlet boundary and never accepts a browser-supplied user ID. It validates active products, positive quantities, stock limits, and authenticated ownership. Prices, line totals, item counts, and subtotal are recalculated with `BigDecimal` from current database prices on every cart read. Buyer-only authentication and authorization filters protect `/cart` and all cart mutation routes.

## Checkout and order transaction

Commit 10 adds the buyer checkout flow at `GET /checkout` and `POST /checkout`, followed by the database-backed confirmation route `GET /order-confirmation?id=...`. The checkout review shows the server-loaded cart values, while the POST request submits no authoritative price or total. The service re-reads the cart and current product rows inside one pooled JDBC transaction, validates active products and stock, calculates totals with `BigDecimal`, creates the order and order items, conditionally deducts stock, clears the cart, and commits only when every operation succeeds.

Stock deduction uses `UPDATE products ... WHERE enabled = TRUE AND stock_quantity >= ?` and checks the affected-row count to prevent overselling. Any validation, stock conflict, order-item, stock, or cart-clearing failure rolls the complete transaction back, leaving no partial order and preserving the cart and stock. Order confirmation reloads the order and items through buyer-scoped DAO queries, so a buyer cannot view another user's order by changing the URL ID. Checkout uses the existing `PENDING` order status and does not implement payment, shipping, or order-management screens.

## Buyer order history

Commit 11 adds `GET /orders` and buyer-scoped `GET /orders/view?id=...` detail pages. `OrderDaoImpl` retrieves order summaries with item counts and retrieves order details only when the authenticated buyer ID matches the order's `buyer_id`. The service returns an empty history for buyers without orders and an empty detail result for nonexistent or another buyer's order, avoiding ownership leaks.

Order details display the stored `order_items.unit_price` and `subtotal` values created during checkout rather than today's product price. Product names and available image URLs are included for presentation, while order total, status, and creation timestamp come from the persisted order. The buyer-only `My Orders` navigation link appears in the shared header, and the checkout confirmation links to order history. Both history pages include responsive empty states and continue-shopping navigation.

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
3. Run `mvn clean test` to verify the schema, infrastructure, authentication, authorization, catalog, cart, checkout transaction, stock rollback, order history, seller CRUD, seller order isolation, status transitions, ownership, validation, and servlet flows.
4. Run `mvn package` to build the WAR.
5. Copy `target/AshokMart.war` to Tomcat 9's `webapps/` directory and open `http://localhost:8080/AshokMart/` after starting Tomcat.
6. Open `/products` from the public landing page to browse the catalog, or open `/register` and `/login` to exercise authentication.
7. Register/login as a buyer, open a product, add it to the cart, and use `/cart` to update, remove, or clear items.
8. Open `/checkout`, review the server-calculated total, place the order, and verify the confirmation page and cleared cart.
9. Open **My Orders**, open an order detail, and verify its stored prices and status.
10. Log in as a seller, open **Seller Dashboard**, create or edit a product, and activate/deactivate it from **My Products**.
11. Open **Seller Orders**, verify only owned items and seller subtotal are shown, then advance a relevant order through a valid status transition.

At web-application startup, the listener initializes the configured H2 schema automatically. The catalog UI, buyer cart, transactional checkout, buyer order history, seller product management, and seller order management are available, while reviews and other marketplace management modules are not yet implemented.

## Maven commands

```bash
mvn clean test
mvn package
```

The generated WAR is written to `target/AshokMart.war` for Tomcat 9 or later Railway deployment through GitHub.
