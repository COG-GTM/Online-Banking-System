package com.userfront.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tables created by the old MySQL5Dialect use MyISAM, which ignores transactions and
 * row locks, so balance updates could neither be serialized nor rolled back. Converts
 * the application's own MyISAM tables (JPA-mapped tables and Hibernate id generator
 * tables) to InnoDB at startup, after Hibernate has updated the schema and before the
 * web server accepts requests, and fails startup if any of them cannot be converted.
 * Other tables in the same database are left untouched.
 */
@Component
public class InnoDbStorageEngineMigration implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(InnoDbStorageEngineMigration.class);

    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;
    private final boolean enabled;

    // EntityManagerFactory also guarantees this runs after Hibernate's schema update.
    public InnoDbStorageEngineMigration(DataSource dataSource, EntityManagerFactory entityManagerFactory,
            @Value("${userfront.convert-myisam-to-innodb:true}") boolean enabled) {
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
        this.enabled = enabled;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        if (!enabled || !isMySql()) {
            return;
        }
        Set<String> appTables = applicationTables();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = database() and table_type = 'BASE TABLE' and engine = 'MyISAM'",
                String.class);
        List<String> failed = new ArrayList<>();
        for (String table : tables) {
            if (!appTables.contains(table.toLowerCase(Locale.ROOT))) {
                continue;
            }
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

    Set<String> applicationTables() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        Set<String> tables = new TreeSet<>();
        for (EntityPersister persister : sessionFactory.getMetamodel().entityPersisters().values()) {
            if (persister instanceof AbstractEntityPersister) {
                addTable(tables, ((AbstractEntityPersister) persister).getTableName());
            }
            IdentifierGenerator generator = persister.getIdentifierGenerator();
            if (generator instanceof SequenceStyleGenerator) {
                addTable(tables, ((SequenceStyleGenerator) generator).getDatabaseStructure().getName());
            } else if (generator instanceof TableGenerator) {
                addTable(tables, ((TableGenerator) generator).getTableName());
            }
        }
        for (CollectionPersister persister : sessionFactory.getMetamodel().collectionPersisters().values()) {
            if (persister instanceof AbstractCollectionPersister) {
                addTable(tables, ((AbstractCollectionPersister) persister).getTableName());
            }
        }
        return tables;
    }

    private static void addTable(Set<String> tables, String qualifiedName) {
        if (qualifiedName == null) {
            return;
        }
        String name = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1).replace("`", "");
        tables.add(name.toLowerCase(Locale.ROOT));
    }

    private boolean isMySql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return "MySQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }
}
