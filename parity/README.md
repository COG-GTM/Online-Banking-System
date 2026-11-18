# HTTP parity harness

Replays a fixed 48-request script (35 distinct endpoints: public pages, signup,
form login, deposits, withdrawals, transfers, recipients, appointments, profile,
the admin `/api/**` REST resources, enable/disable, logout) against a running
app on a freshly reset MySQL database, and saves status, content type,
`Location` and a normalized body (sorted JSON keys; ids, dates, passwords,
account numbers and sessions masked) per request.

`baseline/` was captured from `master` (Spring Boot 2.7.18) built and run on JDK 8.

```bash
# MySQL 5.7 on localhost:3306, root with empty password (what application.properties expects)
docker run -d --name obs-mysql -p 3306:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
  -e MYSQL_DATABASE=OnlineBankingSystem mysql:5.7

# build, capture, compare
mvn -B package
parity/run.sh target/userFront-0.0.1-SNAPSHOT.jar "$JAVA_HOME" /tmp/parity-migrated
parity/compare.py parity/baseline /tmp/parity-migrated
```

`run.sh` drops and recreates the `OnlineBankingSystem` database, starts the jar,
seeds `ROLE_USER`/`ROLE_ADMIN` and replays `capture.py`. Set `MYSQL_ROOT` to point
it at a different MySQL client command.
