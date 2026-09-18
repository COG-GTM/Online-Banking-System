# Online-Banking-System
Spring Boot Online Banking System 
About
This is a project for practicing Spring + Thymeleaf. The idea was to build online banking system.

It was made using Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, Spring Data REST, JavaScript, JQuery. Database is in memory sql Workbench.

Configuration

The datasource credentials are read from the environment, so nothing secret is stored in the repository. Create a dedicated, least-privilege MySQL user for the application (not root) and export:

    export DB_URL=jdbc:mysql://localhost:3306/OnlineBankingSystem   # optional, this is the default
    export DB_USERNAME=obs_app
    export DB_PASSWORD=<password from your secrets manager>

Example of a least-privilege user:

    CREATE USER 'obs_app'@'%' IDENTIFIED BY '<password>';
    GRANT SELECT, INSERT, UPDATE, DELETE ON OnlineBankingSystem.* TO 'obs_app'@'%';

Local development also uses `spring.jpa.hibernate.ddl-auto = update`, which additionally needs CREATE, ALTER, INDEX and REFERENCES on that schema. In production, manage the schema separately and keep the application user limited to DML.

The application fails to start when DB_USERNAME or DB_PASSWORD is unset.

Online Banking Requirements

About

The Banking system consists of two parts: User-Front and Admin-Portal. User-Front is a user-facing system and it includes such modules as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and it involves User Account and Appointment modules.


Er diagram

![er diagram](https://user-images.githubusercontent.com/34470526/37703339-8e85fcae-2d1f-11e8-900f-94cb2046d97f.png)



Online banking system detail diagram

![online banking system detail diagram](https://user-images.githubusercontent.com/34470526/37703353-999023fe-2d1f-11e8-96f6-db40724c5d14.png)

