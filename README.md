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



## Run it locally

This guide runs the User-Front app (the only module in this repo) against MySQL in Docker, on the current Spring Boot `2.0.0.M7` base. Steps marked **[changes after Boot upgrade]** are different once the Spring Boot 2.7.18 upgrade ([#2104](https://github.com/COG-GTM/Online-Banking-System/pull/2104)) lands; each one says what changes.

### 1. Prerequisites

- **JDK 8** (`java -version` shows `1.8`). The project compiles to Java 8 (`java.version` 1.8 in `pom.xml`). **[changes after Boot upgrade]** JDK 17 also works after the upgrade; on this base only JDK 8 has been checked.
- **Maven wrapper** (`./mvnw`, included). It downloads Maven 3.5.2 on first run, so no local Maven install is needed. On macOS/Linux, make it executable once (it is committed without the execute bit):

  ```bash
  chmod +x mvnw
  ```

  **[changes after Boot upgrade]** The upgrade commits `mvnw` as executable, so this step goes away. On Windows use `mvnw.cmd`.
- **Docker**, for MySQL 5.7. Start it with an empty root password and the `OnlineBankingSystem` schema, which is what `application.properties` expects:

  ```bash
  docker run -d --name obs-mysql -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_DATABASE=OnlineBankingSystem -p 3306:3306 mysql:5.7
  ```

  Wait about 20 seconds for MySQL to accept connections (`docker logs obs-mysql` shows `ready for connections`). On Apple Silicon add `--platform linux/amd64`.

### 2. Settings in `src/main/resources/application.properties`

| Setting | Default | Change it to |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/OnlineBankingSystem` | `jdbc:mysql://localhost:3306/OnlineBankingSystem?useSSL=false` (required, see Known issues). Change the port too if you mapped MySQL to a different host port. |
| `spring.datasource.username` | `root` | Keep for the Docker command above. |
| `spring.datasource.password` | empty | Keep empty for the Docker command above; set it if your MySQL has a root password. |
| `spring.jpa.hibernate.ddl-auto` | `update` | Keep. Hibernate creates the tables on first start. |
| `spring.jpa.show-sql` | `true` | Optional: `false` for a quieter log. |

**[changes after Boot upgrade]** The upgrade removes `spring.datasource.testWhileIdle` and `spring.datasource.validationQuery` (Boot 2.7 uses HikariCP) and adds `spring.jpa.open-in-view = true`. Don't add those back by hand on this base. The datasource URL change above still applies.

To leave the file untouched, pass the URL at start-up instead: `--spring.datasource.url="jdbc:mysql://localhost:3306/OnlineBankingSystem?useSSL=false"`.

### 3. Build and start

```bash
./mvnw -B package
java -jar target/userFront-0.0.1-SNAPSHOT.jar
```

The app is ready when the log shows `Started UserFrontApplication`. It serves on http://localhost:8080. The first start creates the tables; the database is empty until you load the sample data. `./mvnw spring-boot:run` also works instead of the two commands.

**[changes after Boot upgrade]** Same commands, and they also work on JDK 17. The upgrade adds an H2-backed smoke test to `./mvnw -B package`, so the build needs no running MySQL.

### 4. Load sample data

With the app started at least once (so the tables exist), load one sample user with a primary and a savings account and a few transactions. All values are sample data.

```bash
docker exec -i obs-mysql mysql -uroot OnlineBankingSystem <<'SQL'
-- Role every user needs (sign-up fails without it)
INSERT INTO role (role_id, name) VALUES (1, 'ROLE_USER');

-- Accounts (balances match the transactions below)
INSERT INTO primary_account (id, account_number, account_balance) VALUES (1, 10000001, 1000.00);
INSERT INTO savings_account (id, account_number, account_balance) VALUES (2, 10000002, 2300.00);

-- Sample user: username "demo", password "password" (BCrypt hash)
INSERT INTO user (user_id, username, password, first_name, last_name, email, phone, enabled, primary_account_id, savings_account_id)
VALUES (3, 'demo', '$2a$12$ru9w7/HT3pFXDXkURDO.uOWvOjHLOetYEOrW2ZY24Qoag9u8v4OEO', 'Demo', 'User', 'demo@example.com', '555-010-0100', b'1', 1, 2);
INSERT INTO user_role (user_role_id, user_id, role_id) VALUES (4, 3, 1);

-- Transaction history
INSERT INTO primary_transaction (id, date, description, type, status, amount, available_balance, primary_account_id) VALUES
  (10, '2026-01-05 09:00:00', 'Deposit to Primary Account', 'Account', 'Finished', 1500, 1500.00, 1),
  (11, '2026-01-12 12:30:00', 'Withdraw from Primary Account', 'Account', 'Finished', 200, 1300.00, 1),
  (12, '2026-01-20 16:45:00', 'Between account transfer from Primary to Savings', 'Account', 'Finished', 300, 1000.00, 1);
INSERT INTO savings_transaction (id, date, description, type, status, amount, available_balance, savings_account_id) VALUES
  (20, '2026-01-06 10:15:00', 'Deposit to savings Account', 'Account', 'Finished', 2000, 2000.00, 2),
  (21, '2026-01-20 16:45:00', 'Between account transfer from Primary to Savings', 'Transfer', 'Finished', 300, 2300.00, 2);

-- Move Hibernate's shared id counter past the ids used above
UPDATE hibernate_sequence SET next_val = 100;
SQL
```

No restart is needed. Sign in at http://localhost:8080 as `demo` / `password`. To start over, run `docker rm -f obs-mysql`, re-run the `docker run` command, start the app once, then load the data again.

**[changes after Boot upgrade]** The table names and the BCrypt hash stay the same. After the upgrade, check that the tables match these columns before loading (`docker exec obs-mysql mysql -uroot OnlineBankingSystem -e 'SHOW TABLES'`).

### 5. Pages to click through

Signed in as `demo`:

| Page | URL | What to check |
| --- | --- | --- |
| Sign in | http://localhost:8080/index | `demo` / `password` takes you to the dashboard. |
| Dashboard | `/userFront` | Primary balance $1000.00 and savings balance $2300.00. |
| Primary account | `/account/primaryAccount` | Three transactions (deposit, withdrawal, transfer to savings). |
| Savings account | `/account/savingsAccount` | Two transactions. |
| Deposit / Withdraw | `/account/deposit`, `/account/withdraw` | Pick an account and an amount; the dashboard balance updates and a new row appears in that account's history. |
| Between accounts | `/transfer/betweenAccounts` | Move money from Primary to Savings or back. |
| Recipients | `/transfer/recipient` | Add a sample recipient (any name, `@example.com` email, any account number). |
| To someone else | `/transfer/toSomeoneElse` | Send money to a saved recipient from Primary or Savings. |
| Appointment | `/appointment/create` | Pick a date and time with the date picker, a location and a description. |
| Profile | `/user/profile` | Shows and edits the signed-in user's details. |
| Sign up | `/signup` (signed out) | Creates a new user with an empty primary and savings account. Needs the `ROLE_USER` row from step 4. |

### 6. Known issues

- **MySQL SSL handshake fails on start-up.** The connector that Boot `2.0.0.M7` pulls in (`mysql-connector-java` 5.1.44) tries TLSv1/1.1, which current JDK 8 builds disable, so start-up fails with `SSLHandshakeException: No appropriate protocol`. Add `?useSSL=false` to `spring.datasource.url` (step 2). **[changes after Boot upgrade]** The upgrade switches to `mysql-connector-j` 8; keep `useSSL=false` for local MySQL 5.7 unless you set up TLS.
- **Sign-up silently reloads the form on an empty database.** The sign-up controller looks up `ROLE_USER` and fails with `IllegalArgumentException: Target object must not be null` in the log when that row is missing. Load step 4 (or at least its `role` insert) first.
- **Sign-up after a restart can break.** New account numbers restart at `11223146` every time the app starts, so a user signed up after a restart gets an account number already in use, and that request fails with `NonUniqueResultException`. Users signed up within one run are fine, and the sample accounts (`10000001`, `10000002`) sit outside that range. To recover, reset the database (step 4). This is unchanged by the Boot upgrade.
- **`./mvnw: Permission denied`.** Run `chmod +x mvnw` (step 1). **[changes after Boot upgrade]** Fixed by the upgrade.
- **Maven wrapper download fails with HTTP 429.** Maven Central rate-limits the Maven 3.5.2 download now and then. Retry after a minute.
- **Port 3306 or 8080 already in use.** Map MySQL to another host port (`-p 3307:3306`) and use that port in `spring.datasource.url`; start the app with `--server.port=8081` for a different web port.
- **Appointment dates.** The form posts `yyyy-MM-dd hh:mm`; use the date picker rather than typing the date.
- **Admin-Portal is not in this repo.** The diagrams above describe it, but only User-Front is here.
