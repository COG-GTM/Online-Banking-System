# Online-Banking-System
Spring Boot Online Banking System 
About
This is a project for practicing Spring + Thymeleaf. The idea was to build online banking system.

It was made using Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, Spring Data REST, JavaScript, JQuery. Database is in memory sql Workbench.
Online Banking Requirements

About

The Banking system consists of two parts: User-Front and Admin-Portal. User-Front is a user-facing system and it includes such modules as User Signup/Login, Account, Transfer, Appointment, Transaction and User Profile. Admin-Portal is mainly used by Admin and it involves User Account and Appointment modules.


Database engine

Balance updates run in a transaction and take row locks, which MySQL only honours on InnoDB tables. New schemas are generated as InnoDB. A database created by an earlier version may still hold MyISAM tables; `ddl-auto=update` does not convert them, so run a one-time migration per account/transaction table:

    ALTER TABLE primary_account ENGINE=InnoDB;
    ALTER TABLE savings_account ENGINE=InnoDB;
    ALTER TABLE primary_transaction ENGINE=InnoDB;
    ALTER TABLE savings_transaction ENGINE=InnoDB;


Er diagram

![er diagram](https://user-images.githubusercontent.com/34470526/37703339-8e85fcae-2d1f-11e8-900f-94cb2046d97f.png)



Online banking system detail diagram

![online banking system detail diagram](https://user-images.githubusercontent.com/34470526/37703353-999023fe-2d1f-11e8-96f6-db40724c5d14.png)

