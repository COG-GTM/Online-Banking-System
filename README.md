# Online-Banking-System
Spring Boot Online Banking System 
About
This is a project for practicing Spring + Thymeleaf. The idea was to build online banking system.

It was made using Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, Spring Data REST, JavaScript, JQuery. Database is in memory sql Workbench.
Online Banking Requirements

About

The Banking system consists of two parts: User-Front and Admin-Portal. User-Front is a user-facing system and it includes such modules as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and it involves User Account and Appointment modules.


Running locally

The default configuration runs with `spring.jpa.hibernate.ddl-auto=validate` and SQL logging disabled, so the application never mutates the schema it connects to. For local development, where Hibernate should create/update the schema and echo SQL, start the app with the `dev` profile:

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Both settings can also be overridden per environment with the `SPRING_JPA_DDL_AUTO` and `SPRING_JPA_SHOW_SQL` environment variables.

Er diagram

![er diagram](https://user-images.githubusercontent.com/34470526/37703339-8e85fcae-2d1f-11e8-900f-94cb2046d97f.png)



Online banking system detail diagram

![online banking system detail diagram](https://user-images.githubusercontent.com/34470526/37703353-999023fe-2d1f-11e8-96f6-db40724c5d14.png)

