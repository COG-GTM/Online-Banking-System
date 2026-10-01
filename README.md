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


## Running

HTTPS is required by default: every request is redirected to HTTPS, HSTS is sent, and the session and remember-me cookies are marked `Secure`.

- **TLS at a reverse proxy / load balancer:** have the proxy set `X-Forwarded-Proto` (and `X-Forwarded-For`); `server.use-forward-headers=true` makes the app treat those requests as secure.
- **TLS in the app:** provide a keystore through the environment, e.g. `SERVER_SSL_ENABLED=true SERVER_SSL_KEY_STORE=file:/path/keystore.p12 SERVER_SSL_KEY_STORE_PASSWORD=... SERVER_SSL_KEY_STORE_TYPE=PKCS12 SERVER_PORT=8443`.
- **Local plain-HTTP development:** run with `-Dspring.profiles.active=dev` (`application-dev.properties` sets `app.security.require-ssl=false`). Never enable this profile in a deployed environment.
