-- One-off migration for databases created with org.hibernate.dialect.MySQL5Dialect,
-- whose tables default to MyISAM and silently ignore transaction rollback.
-- hibernate.ddl-auto=update does not change the engine of existing tables.
ALTER TABLE primary_account ENGINE=InnoDB;
ALTER TABLE savings_account ENGINE=InnoDB;
ALTER TABLE primary_transaction ENGINE=InnoDB;
ALTER TABLE savings_transaction ENGINE=InnoDB;
ALTER TABLE recipient ENGINE=InnoDB;
ALTER TABLE appointment ENGINE=InnoDB;
ALTER TABLE user ENGINE=InnoDB;
ALTER TABLE role ENGINE=InnoDB;
ALTER TABLE user_role ENGINE=InnoDB;
