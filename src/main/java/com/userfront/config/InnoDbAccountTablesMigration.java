package com.userfront.config;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Balance updates rely on transactions and row locks, which MyISAM silently ignores.
 * Tables created by earlier deployments with the MyISAM engine are converted to InnoDB at startup,
 * after all singletons are created but before the embedded web server starts accepting requests.
 */
@Component
public class InnoDbAccountTablesMigration implements SmartInitializingSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(InnoDbAccountTablesMigration.class);

    private static final List<String> TABLES = Arrays.asList(
            "primary_account", "savings_account", "primary_transaction", "savings_transaction");

    private final JdbcTemplate jdbcTemplate;

    public InnoDbAccountTablesMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String product = jdbcTemplate.execute(
                (Connection connection) -> connection.getMetaData().getDatabaseProductName());
        String name = product == null ? "" : product.toLowerCase();
        if (!name.contains("mysql") && !name.contains("mariadb")) {
            return;
        }
        for (String table : TABLES) {
            List<String> engines = jdbcTemplate.queryForList(
                    "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    String.class, table);
            if (!engines.isEmpty() && !"InnoDB".equalsIgnoreCase(engines.get(0))) {
                LOG.info("Converting table {} from {} to InnoDB", table, engines.get(0));
                jdbcTemplate.execute("ALTER TABLE `" + table + "` ENGINE=InnoDB");
            }
        }
    }
}
