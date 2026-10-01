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



Database configuration

The datasource credentials are read from the environment; nothing is committed and there are no defaults, so the application refuses to start if they are missing.

| Variable | Required | Default |
| --- | --- | --- |
| `DB_URL` | no | `jdbc:mysql://localhost:3306/OnlineBankingSystem` |
| `DB_USERNAME` | yes | none |
| `DB_PASSWORD` | yes | none |

Connect with a dedicated least-privilege user rather than `root`. Hibernate runs with `ddl-auto=update`, so the user needs DDL rights on the application schema only:

```sql
CREATE DATABASE IF NOT EXISTS OnlineBankingSystem;
CREATE USER 'obs_app'@'%' IDENTIFIED BY '<strong-random-password>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON OnlineBankingSystem.* TO 'obs_app'@'%';
```

```sh
DB_USERNAME=obs_app DB_PASSWORD='<strong-random-password>' ./mvnw spring-boot:run
```
