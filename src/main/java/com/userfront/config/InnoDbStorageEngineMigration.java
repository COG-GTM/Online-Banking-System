package com.userfront.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Account balances and their ledgers are updated inside transactions that
 * rely on row locks and rollback, which MyISAM (the engine Hibernate's
 * MySQL5Dialect used to generate) does not provide. Converts any of those
 * tables still on a non-transactional engine to InnoDB at startup, after
 * Hibernate's schema update has run. No-op on other databases.
 */
@Component
public class InnoDbStorageEngineMigration implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InnoDbStorageEngineMigration.class);

    private static final List<String> TRANSACTIONAL_TABLES = Arrays.asList(
            "primary_account", "savings_account", "primary_transaction", "savings_transaction");

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        if (!isMySql()) {
            return;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> nonTransactional = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables"
                        + " where table_schema = database() and engine <> 'InnoDB'"
                        + " and lower(table_name) in (?, ?, ?, ?)",
                String.class, TRANSACTIONAL_TABLES.toArray());

        for (String table : nonTransactional) {
            LOG.warn("Converting table {} to InnoDB so balance updates are transactional", table);
            jdbcTemplate.execute("alter table `" + table + "` engine=InnoDB");
        }
    }

    private boolean isMySql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        }
    }
}
