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

The datasource credentials are read from the environment, so nothing is committed to the repository. Create a least-privilege application user (no DDL/admin rights beyond what Hibernate needs for the schema) and export:

```
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/OnlineBankingSystem
export SPRING_DATASOURCE_USERNAME=onlinebanking_app
export SPRING_DATASOURCE_PASSWORD='your-password'
```

Example MySQL setup:

```sql
CREATE USER 'onlinebanking_app'@'%' IDENTIFIED BY 'your-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON OnlineBankingSystem.* TO 'onlinebanking_app'@'%';
```

The application fails to start when `SPRING_DATASOURCE_USERNAME` or `SPRING_DATASOURCE_PASSWORD` is missing.
