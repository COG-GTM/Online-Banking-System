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


## Database schema

By default the application only validates the database schema against the JPA entities
(`spring.jpa.hibernate.ddl-auto=validate`) and does not log SQL statements, so the schema must be
created/migrated outside the application. For local development, activate the `dev` profile
(`--spring.profiles.active=dev` or `SPRING_PROFILES_ACTIVE=dev`) to let Hibernate create/update the
local schema and log SQL. `SPRING_JPA_DDL_AUTO` / `SPRING_JPA_SHOW_SQL` can also override the defaults.
