package com.userfront.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tables created by the old MySQL5Dialect use MyISAM, which ignores transactions and
 * row locks, so balance updates could neither be serialized nor rolled back. Converts
 * any remaining MyISAM tables in the application schema to InnoDB at startup, after
 * Hibernate has updated the schema and before the web server accepts requests, and fails
 * startup if any table cannot be converted.
 */
@Component
public class InnoDbStorageEngineMigration implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(InnoDbStorageEngineMigration.class);

    private final DataSource dataSource;
    private final boolean enabled;

    // EntityManagerFactory is injected only so this runs after Hibernate's schema update.
    public InnoDbStorageEngineMigration(DataSource dataSource, EntityManagerFactory entityManagerFactory,
            @Value("${userfront.convert-myisam-to-innodb:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        if (!enabled || !isMySql()) {
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = database() and table_type = 'BASE TABLE' and engine = 'MyISAM'",
                String.class);
        List<String> failed = new ArrayList<>();
        for (String table : tables) {
            String sql = "ALTER TABLE `" + table.replace("`", "``") + "` ENGINE=InnoDB";
            try {
                jdbc.execute(sql);
                LOG.info("Converted table {} from MyISAM to InnoDB", table);
            } catch (RuntimeException e) {
                LOG.error("Could not convert table {} to InnoDB. Run manually: {}", table, sql, e);
                failed.add(table);
            }
        }
        if (!failed.isEmpty()) {
            throw new IllegalStateException("Refusing to start: tables " + failed
                    + " are still MyISAM, which ignores transactions and row locks, so balance updates"
                    + " would not be atomic. Convert them to InnoDB or set userfront.convert-myisam-to-innodb=false.");
        }
    }

    private boolean isMySql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return "MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }
}
