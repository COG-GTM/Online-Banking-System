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

Database credentials are read from environment variables and are not committed:

| Variable | Description |
| --- | --- |
| `DB_URL` | JDBC URL (defaults to `jdbc:mysql://localhost:3306/OnlineBankingSystem`) |
| `DB_USERNAME` | Database user (use a least-privilege application account, not `root`) |
| `DB_PASSWORD` | Database password |

By default Hibernate only validates the schema (`ddl-auto=validate`) and SQL logging is off. For local development, activate the `dev` profile to let Hibernate create/update the schema and log SQL. Schema generation does not seed reference data: user signup requires a `ROLE_USER` row in the `role` table.

Example:

```
SPRING_PROFILES_ACTIVE=dev DB_USERNAME=bank_app DB_PASSWORD=... ./mvnw spring-boot:run
```
