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

## Database setup

The application does not create or alter its own tables:
`spring.jpa.hibernate.ddl-auto=validate` makes Hibernate only check the schema at
startup, and `spring.jpa.show-sql=false` keeps SQL statements out of the logs.
Before the first start, create the `OnlineBankingSystem` database and apply the
baseline schema (which also seeds the `hibernate_sequence` row and `ROLE_USER`):

```
mysql -u <admin> -p -e "CREATE DATABASE IF NOT EXISTS OnlineBankingSystem"
mysql -u <admin> -p OnlineBankingSystem < src/main/resources/db/V1__baseline_schema.sql
```

Schema changes ship as a new `src/main/resources/db/V<n>__<description>.sql`
script alongside the entity change.

For a disposable local database only, the `dev` profile restores
`ddl-auto=update` and SQL logging: run with `--spring.profiles.active=dev`
(or `SPRING_PROFILES_ACTIVE=dev`). Never enable it in production.
