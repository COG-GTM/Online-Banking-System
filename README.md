# Online-Banking-System
Spring Boot Online Banking System

## About
This is a project for practicing Spring + Thymeleaf. The idea was to build an online banking system.

It is built with Spring Boot 2.7.18, Spring Security, Thymeleaf, Spring Data JPA (Hibernate 5.6), JavaScript, jQuery and Bootstrap. Data is stored in MySQL; the test suite uses an in-memory H2 database instead.

## Online Banking Requirements

The Banking system consists of two parts: User-Front and Admin-Portal. User-Front is a user-facing system and it includes such modules as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and it involves User Account and Appointment modules.

This repository is the User-Front. The Admin-Portal is a separate front end that is not in this repository; it calls the JSON endpoints under `/api` (`/api/user/**`, `/api/appointment/**`), and the CORS filter allows it from `http://localhost:4200`.

## Tech stack

| | Version (from `pom.xml`) |
|---|---|
| Java | 8 (`java.version` 1.8) |
| Spring Boot | 2.7.18 (Spring Security 5.7, Hibernate 5.6, embedded Tomcat 9) |
| Database | MySQL (`com.mysql:mysql-connector-j`), dialect `MySQL5Dialect` |
| Test database | H2 in-memory, MySQL mode (`src/test/resources/application.properties`) |
| Build | Maven (`./mvnw` wrapper, or Maven 3.5+) |

## Running locally

### Prerequisites
- JDK 8 or newer (the build and tests pass on JDK 8 and 17)
- A MySQL server on `localhost:3306`

### 1. Create the database
`src/main/resources/application.properties` connects to `jdbc:mysql://localhost:3306/OnlineBankingSystem` as `root` with an empty password. Create the database (Hibernate creates the tables on startup with `ddl-auto=update`):

```sql
CREATE DATABASE OnlineBankingSystem;
```

To start a matching MySQL with Docker instead:

```bash
docker run -d --name obs-mysql -p 127.0.0.1:3306:3306 \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_DATABASE=OnlineBankingSystem mysql:8.0
```

To use other credentials, edit `spring.datasource.*` in `application.properties` or override them on the command line, e.g. `--spring.datasource.password=secret`.

### 2. Start the app

```bash
./mvnw spring-boot:run
```

The app starts on http://localhost:8080 and redirects to the login page at `/index`.

### 3. Seed the user role (once)
Sign-up assigns every new user the `ROLE_USER` role, which the app does not create itself. After the first start (so the tables exist), insert it, otherwise sign-up fails with "Entity must not be null":

```sql
INSERT INTO role (role_id, name) VALUES (1, 'ROLE_USER');
```

Then sign up at http://localhost:8080/signup and log in.

## Running the tests

```bash
./mvnw test
```

The tests use H2 in memory, so they need no MySQL server.

## Er diagram

![er diagram](https://user-images.githubusercontent.com/34470526/37703339-8e85fcae-2d1f-11e8-900f-94cb2046d97f.png)

## Online banking system detail diagram

![online banking system detail diagram](https://user-images.githubusercontent.com/34470526/37703353-999023fe-2d1f-11e8-96f6-db40724c5d14.png)
