package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.hql.spi.id.IdTableSupport;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.junit.Test;

public class MySqlTemporaryTableBulkIdStrategyTest {

    @Test
    public void applicationPropertiesPinsTemporaryTableStrategy() throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            properties.load(in);
        }

        assertEquals(MySqlTemporaryTableBulkIdStrategy.class.getName(),
                properties.getProperty("spring.jpa.properties.hibernate.hql.bulk_id_strategy"));
    }

    @Test
    public void matchesMySqlDialectDefaultDdl() {
        MultiTableBulkIdStrategy dialectDefault = new MySQL5Dialect().getDefaultMultiTableBulkIdStrategy();
        assertTrue(dialectDefault instanceof LocalTemporaryTableBulkIdStrategy);

        IdTableSupport expected = ((LocalTemporaryTableBulkIdStrategy) dialectDefault).getIdTableSupport();
        IdTableSupport actual = new MySqlTemporaryTableBulkIdStrategy().getIdTableSupport();

        assertEquals(expected.getCreateIdTableCommand(), actual.getCreateIdTableCommand());
        assertEquals(expected.getDropIdTableCommand(), actual.getDropIdTableCommand());
        assertEquals(expected.getCreateIdTableStatementOptions(), actual.getCreateIdTableStatementOptions());
        assertEquals(expected.generateIdTableName("account"), actual.generateIdTableName("account"));
    }
}
