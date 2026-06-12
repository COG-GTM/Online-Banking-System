# Online-Banking-System

Spring Boot Online Banking System.

## About

This is a project for practicing Spring + Thymeleaf. The idea was to build an online banking system.

It was made using Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, Spring Data REST, JavaScript, JQuery. The database is MySQL.

The Banking system consists of two parts: **User-Front** and **Admin-Portal**. User-Front is a user-facing system and includes modules such as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and involves the User Account and Appointment modules.

## Multi-module structure

The project is organized as a multi-module Maven build:

```
pom.xml                  parent POM (packaging=pom)
common/                  shared domain entities, DAOs and service interfaces
userfront-service/       customer-facing Thymeleaf web app  (port 8080)
admin-service/           admin REST API                     (port 8081)
```

- **common** (`online-banking-common`) — a plain JAR containing the JPA domain
  entities (`com.userfront.domain`), Spring Data DAOs (`com.userfront.dao`) and
  the service interfaces (`com.userfront.service`). Shared by both services.
- **userfront-service** — the `@SpringBootApplication` for the server-rendered
  Thymeleaf customer portal. Uses form-based login.
- **admin-service** — a stateless REST API exposing the admin endpoints under
  `/api/**`, secured with HTTP Basic and `ROLE_ADMIN`.

Both services share the same MySQL database (`OnlineBankingSystem`). The
`userfront-service` owns the schema (`spring.jpa.hibernate.ddl-auto=update`); the
`admin-service` only validates it (`spring.jpa.hibernate.ddl-auto=validate`).

## Database

Create a MySQL database named `OnlineBankingSystem` and adjust the credentials in
each service's `src/main/resources/application.properties` if needed (defaults:
user `root`, empty password, `jdbc:mysql://localhost:3306/OnlineBankingSystem`).

## Build

From the project root:

```bash
mvn clean install
```

This builds and installs all three modules (`common` first, then the two
services).

> The project targets Java 8 and Spring Boot 2.0.0.M7. Build with a Java 8 JDK.

## Run

Run each service independently from its module directory:

```bash
# Customer-facing web app on http://localhost:8080
cd userfront-service && mvn spring-boot:run

# Admin REST API on http://localhost:8081
cd admin-service && mvn spring-boot:run
```

Both can run simultaneously against the same MySQL database.

## Running with Docker Compose

### Prerequisites
- Docker and Docker Compose installed

### Start all services

```bash
docker-compose up --build
```

This builds the `userfront-service` and `admin-service` images (multi-stage Maven
builds with the build context set to the repo root) and starts them alongside a
MySQL 5.7 container. The services connect to MySQL over the Compose network using
the `mysql` hostname; the datasource settings are supplied via the
`SPRING_DATASOURCE_*` environment variables, which override the `localhost`
defaults in each service's `application.properties`.

### Access
- **User-Front:** http://localhost:8080
- **Admin API:** http://localhost:8081/api/

### Stop

```bash
docker-compose down
```

### Reset database

```bash
docker-compose down -v
```

### Admin API endpoints (admin-service, port 8081)

All endpoints require a user with `ROLE_ADMIN` (HTTP Basic):

- `GET /api/user/all`
- `GET /api/user/primary/transaction?username={username}`
- `GET /api/user/savings/transaction?username={username}`
- `GET /api/user/{username}/enable`
- `GET /api/user/{username}/disable`
- `GET /api/appointment/all`
- `GET /api/appointment/{id}/confirm`
