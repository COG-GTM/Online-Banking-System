package com.userfront.config;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;

/**
 * Multi-table bulk HQL update/delete strategy that stages ids in a MySQL
 * session-local temporary table, mirroring MySQLDialect's default. Pinned via
 * hibernate.hql.bulk_id_strategy so the inline-ids strategies (which render id
 * values directly into SQL, CVE-2026-0603) can never be selected.
 */
public class MySqlTemporaryTableBulkIdStrategy extends LocalTemporaryTableBulkIdStrategy {

    public MySqlTemporaryTableBulkIdStrategy() {
        super(new MySqlIdTableSupport(), AfterUseAction.DROP, TempTableDdlTransactionHandling.NONE);
    }

    static class MySqlIdTableSupport extends IdTableSupportStandardImpl {

        @Override
        public String getCreateIdTableCommand() {
            return "create temporary table if not exists";
        }

        @Override
        public String getDropIdTableCommand() {
            return "drop temporary table";
        }
    }
}
