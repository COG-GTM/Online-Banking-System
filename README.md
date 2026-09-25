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

The datasource credentials are not committed. Set `DB_USERNAME` and `DB_PASSWORD` (and optionally `DB_URL`) for a least-privilege database user before starting the application; startup fails if they are missing.

The default configuration assumes HTTPS: plain HTTP requests are redirected, the session and remember-me cookies are Secure, HSTS is sent, and the schema is validated rather than generated. Terminate TLS at a proxy (`X-Forwarded-*` headers are honoured) or set `SSL_ENABLED=true` with `SSL_KEY_STORE`/`SSL_KEY_STORE_PASSWORD`.

For local development over plain HTTP, run with the `dev` profile (`-Dspring.profiles.active=dev`), which disables the HTTPS requirement and Secure cookies and lets Hibernate create the schema.
