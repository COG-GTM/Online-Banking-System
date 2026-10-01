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


## Database configuration

Datasource credentials are not stored in the repository; they are read from the environment
at startup and the application refuses to start if they are missing.

| Variable      | Required | Default                                          |
|---------------|----------|--------------------------------------------------|
| `DB_URL`      | no       | `jdbc:mysql://localhost:3306/OnlineBankingSystem` |
| `DB_USERNAME` | yes      | -                                                |
| `DB_PASSWORD` | yes      | -                                                |

Use a dedicated application account limited to the application's schema instead of `root`:

```sql
CREATE DATABASE IF NOT EXISTS OnlineBankingSystem;
CREATE USER 'onlinebanking_app'@'%' IDENTIFIED BY '<password from your secrets manager>';
-- runtime data access
GRANT SELECT, INSERT, UPDATE, DELETE ON OnlineBankingSystem.* TO 'onlinebanking_app'@'%';
-- required while spring.jpa.hibernate.ddl-auto=update manages the schema
GRANT CREATE, ALTER, INDEX, REFERENCES ON OnlineBankingSystem.* TO 'onlinebanking_app'@'%';
```

```
export DB_USERNAME=onlinebanking_app
export DB_PASSWORD=...   # inject from your secrets manager; do not commit it
./mvnw spring-boot:run
```
