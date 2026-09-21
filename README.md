# Online-Banking-System
Spring Boot Online Banking System 
About
This is a project for practicing Spring + Thymeleaf. The idea was to build online banking system.

It was made using Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, Spring Data REST, JavaScript, JQuery. Database is in memory sql Workbench.
Online Banking Requirements

About

The Banking system consists of two parts: User-Front and Admin-Portal. User-Front is a user-facing system and it includes such modules as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and it involves User Account and Appointment modules.


Er diagram

![er diagram](https://user-images.githubusercontent.com/34470526/37703339-8e85fcae-2d1f-11e8-900f-94cb2046d97f.png)



Online banking system detail diagram

![online banking system detail diagram](https://user-images.githubusercontent.com/34470526/37703353-999023fe-2d1f-11e8-96f6-db40724c5d14.png)


## Configuration

Database credentials and security-sensitive settings are supplied through environment
variables instead of being committed to `application.properties`:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/OnlineBankingSystem` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | empty | database user |
| `SPRING_DATASOURCE_PASSWORD` | empty | database password |
| `SPRING_JPA_DDL_AUTO` | `validate` | Hibernate schema handling (`update` for local bootstrapping) |
| `SPRING_JPA_SHOW_SQL` | `false` | SQL logging |
| `APP_SECURITY_REQUIRE_HTTPS` | `true` | require HTTPS and mark cookies secure |
| `SESSION_COOKIE_SECURE` | `true` | secure session cookie |
| `APP_CORS_ALLOWED_ORIGINS` | empty | comma separated CORS origins (none by default) |

For local HTTP development, run with `APP_SECURITY_REQUIRE_HTTPS=false SESSION_COOKIE_SECURE=false`.
