# Online-Banking-System
Spring Boot Online Banking System 
About
This is a project for practicing Spring + Thymeleaf. The idea was to build online banking system.

It was made using Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, Spring Data REST, JavaScript, JQuery. Database is in memory sql Workbench.
Online Banking Requirements

About

The Banking system consists of two parts: User-Front and Admin-Portal. User-Front is a user-facing system and it includes such modules as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and it involves User Account and Appointment modules.


Database configuration

The datasource credentials are read from the environment (or a secrets manager) and must be set before starting the application:

```
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/OnlineBankingSystem
export SPRING_DATASOURCE_USERNAME=onlinebanking_app
export SPRING_DATASOURCE_PASSWORD=<password>
```

Use a dedicated least-privilege database user (SELECT/INSERT/UPDATE/DELETE on the application schema only), not the MySQL `root` account:

```sql
CREATE USER 'onlinebanking_app'@'%' IDENTIFIED BY '<password>';
GRANT SELECT, INSERT, UPDATE, DELETE ON OnlineBankingSystem.* TO 'onlinebanking_app'@'%';
```

Er diagram

![er diagram](https://user-images.githubusercontent.com/34470526/37703339-8e85fcae-2d1f-11e8-900f-94cb2046d97f.png)



Online banking system detail diagram

![online banking system detail diagram](https://user-images.githubusercontent.com/34470526/37703353-999023fe-2d1f-11e8-96f6-db40724c5d14.png)

