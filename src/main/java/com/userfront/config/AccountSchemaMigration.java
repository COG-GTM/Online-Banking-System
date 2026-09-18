package com.userfront.config;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Brings account and ledger tables created by earlier releases up to the shape the
 * optimistic locking and transactional debit path requires: InnoDB storage and a
 * non-null version column. Hibernate's schema update neither converts the storage
 * engine nor backfills a column it added to an earlier schema.
 *
 * Runs once the entity manager factory has applied its schema update and before the
 * web server accepts requests, and each statement is skipped when the table already
 * has the required shape.
 */
@Component
@DependsOn("entityManagerFactory")
public class AccountSchemaMigration implements InitializingBean {

    private static final String[] VERSIONED_TABLES = {"primary_account", "savings_account"};
    private static final String[] TRANSACTIONAL_TABLES = {
        "primary_account", "savings_account", "primary_transaction", "savings_transaction"
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() {
        for (String table : TRANSACTIONAL_TABLES) {
            if (!isInnoDb(table)) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ENGINE=InnoDB");
            }
        }

        for (String table : VERSIONED_TABLES) {
            if (versionColumnIsNullable(table)) {
                jdbcTemplate.update("UPDATE " + table + " SET version = 0 WHERE version IS NULL");
                jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY version BIGINT NOT NULL DEFAULT 0");
            }
        }
    }

    private boolean isInnoDb(String table) {
        List<String> engines = jdbcTemplate.queryForList(
            "SELECT engine FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            String.class, table);

        return engines.isEmpty() || "InnoDB".equalsIgnoreCase(engines.get(0));
    }

    private boolean versionColumnIsNullable(String table) {
        List<String> nullable = jdbcTemplate.queryForList(
            "SELECT is_nullable FROM information_schema.columns"
                + " WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'version'",
            String.class, table);

        return !nullable.isEmpty() && "YES".equalsIgnoreCase(nullable.get(0));
    }
}
