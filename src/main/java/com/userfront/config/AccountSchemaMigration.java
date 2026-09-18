package com.userfront.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Brings account and ledger tables created by earlier releases up to the shape the
 * optimistic locking and transactional debit path requires: InnoDB storage and a
 * non-null version column. Hibernate's schema update adds the column but never
 * backfills it or converts the storage engine.
 */
@Component
public class AccountSchemaMigration implements ApplicationRunner {

    private static final String[] VERSIONED_TABLES = {"primary_account", "savings_account"};
    private static final String[] TRANSACTIONAL_TABLES = {
        "primary_account", "savings_account", "primary_transaction", "savings_transaction"
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String table : TRANSACTIONAL_TABLES) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ENGINE=InnoDB");
        }

        for (String table : VERSIONED_TABLES) {
            jdbcTemplate.update("UPDATE " + table + " SET version = 0 WHERE version IS NULL");
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY version BIGINT NOT NULL DEFAULT 0");
        }
    }
}
