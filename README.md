# AshokMart

AshokMart is a Java-based multi-vendor e-commerce web application. This repository currently contains **Commit 01: the project foundation only**. Business features such as authentication, products, carts, checkout, orders, reviews, and seller or administrator dashboards have not been implemented yet.

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

The foundation keeps presentation, controller, business, and persistence responsibilities separate so later commits can add features without changing the required architecture.

## Project layout

```text
src/main/java/com/ashokmart/   Java package roots for models, DAOs, services,
                               servlets, filters, and utilities
src/main/resources/             Application resources
src/main/webapp/                JSP and static web resources
src/test/java/com/ashokmart/    Test source root
```

## Local setup

1. Install Java 17 and Maven.
2. Install or run Apache Tomcat 9.
3. Clone this repository.
4. Build the WAR with the Maven commands below.
5. Copy `target/AshokMart.war` to Tomcat's `webapps/` directory, or configure it as the application artifact in a later Railway deployment.
6. Start Tomcat and open `http://localhost:8080/AshokMart/`.

The current verification page is intentionally minimal and confirms that the foundation WAR can be served.

## Maven commands

```bash
mvn clean
mvn test
mvn package
```

The generated WAR is written to `target/AshokMart.war`. It is intended for later deployment to Apache Tomcat 9 and, eventually, Railway through GitHub.
