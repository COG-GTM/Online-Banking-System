package com.userfront.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Brings account and ledger tables created by earlier releases up to the shape the
 * optimistic locking and transactional debit path requires: InnoDB storage, a non-null
 * version column and exact DECIMAL money columns. Hibernate's schema update neither
 * converts the storage engine, backfills a column it added to an earlier schema, nor
 * changes the type of an existing column.
 *
 * Runs once the entity manager factory has applied its schema update and before the
 * web server accepts requests, and each statement is skipped when the table already
 * has the required shape.
 */
@Component
@DependsOn("entityManagerFactory")
public class AccountSchemaMigration implements InitializingBean {

    private static final String MONEY_TYPE = "DECIMAL(19,2)";

    private static final String[] VERSIONED_TABLES = {"primary_account", "savings_account"};
    private static final String[] TRANSACTIONAL_TABLES = {
        "primary_account", "savings_account", "primary_transaction", "savings_transaction"
    };

    private static final Map<String, String[]> MONEY_COLUMNS = new LinkedHashMap<>();

    static {
        MONEY_COLUMNS.put("primary_account", new String[] {"account_balance"});
        MONEY_COLUMNS.put("savings_account", new String[] {"account_balance"});
        MONEY_COLUMNS.put("primary_transaction", new String[] {"amount", "available_balance"});
        MONEY_COLUMNS.put("savings_transaction", new String[] {"amount", "available_balance"});
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() {
        for (String table : TRANSACTIONAL_TABLES) {
            requireTable(table);

            if (!isInnoDb(table)) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ENGINE=InnoDB");
            }
        }

        for (Map.Entry<String, String[]> entry : MONEY_COLUMNS.entrySet()) {
            for (String column : entry.getValue()) {
                if (!isMoneyColumn(entry.getKey(), column)) {
                    jdbcTemplate.execute(
                        "ALTER TABLE " + entry.getKey() + " MODIFY " + column + " " + MONEY_TYPE);
                }
            }
        }

        for (String table : VERSIONED_TABLES) {
            if (versionColumnIsNullable(table)) {
                jdbcTemplate.update("UPDATE " + table + " SET version = 0 WHERE version IS NULL");
                jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY version BIGINT NOT NULL DEFAULT 0");
            }
        }
    }

    private void requireTable(String table) {
        if (engineOf(table).isEmpty()) {
            throw new IllegalStateException(
                "Table " + table + " is missing, so the money schema cannot be migrated.");
        }
    }

    private boolean isInnoDb(String table) {
        List<String> engines = engineOf(table);

        return !engines.isEmpty() && "InnoDB".equalsIgnoreCase(engines.get(0));
    }

    private List<String> engineOf(String table) {
        return jdbcTemplate.queryForList(
            "SELECT engine FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            String.class, table);
    }

    private boolean isMoneyColumn(String table, String column) {
        List<String> types = jdbcTemplate.queryForList(
            "SELECT column_type FROM information_schema.columns"
                + " WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
            String.class, table, column);

        return !types.isEmpty() && MONEY_TYPE.equalsIgnoreCase(types.get(0).replace(" ", ""));
    }

    private boolean versionColumnIsNullable(String table) {
        List<String> nullable = jdbcTemplate.queryForList(
            "SELECT is_nullable FROM information_schema.columns"
                + " WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'version'",
            String.class, table);

        return !nullable.isEmpty() && "YES".equalsIgnoreCase(nullable.get(0));
    }
}
