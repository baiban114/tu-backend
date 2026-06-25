package com.tu.backend.knowledgerelation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class KnowledgeRelationSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRelationSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeRelationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String database = databaseProductName();
            if (database.contains("mysql") || database.contains("mariadb")) {
                ensureMysqlSchema();
                return;
            }
            if (database.contains("postgresql")) {
                ensurePostgresqlSchema();
                return;
            }
            if (database.contains("h2")) {
                ensureH2Schema();
            }
        } catch (Exception ex) {
            log.error("failed to ensure knowledge relation schema", ex);
            throw ex;
        }
    }

    private void ensureMysqlSchema() {
        jdbcTemplate.execute("""
            create table if not exists relation_type_def (
              id varchar(64) not null primary key,
              kb_id varchar(64) null,
              type_key varchar(64) not null,
              label varchar(64) not null,
              color varchar(32) null,
              icon varchar(64) null,
              bidirectional bit not null,
              is_system bit not null,
              enabled bit not null,
              created_at datetime(6) not null,
              updated_at datetime(6) not null,
              constraint uk_relation_type_def_kb_key unique (kb_id, type_key)
            )
            """);
        migrateLegacySystemColumn("mysql");
        jdbcTemplate.execute("""
            create table if not exists knowledge_relation (
              id varchar(64) not null primary key,
              kb_id varchar(64) not null,
              relation_type_key varchar(64) not null,
              from_anchor_kind varchar(32) not null,
              from_locator varchar(512) not null,
              from_snapshot_json longtext null,
              to_anchor_kind varchar(32) not null,
              to_locator varchar(512) not null,
              to_snapshot_json longtext null,
              note varchar(1024) null,
              metadata_json longtext null,
              source_provenance varchar(32) not null,
              status varchar(16) not null,
              created_at datetime(6) not null,
              updated_at datetime(6) not null,
              index idx_kr_kb_from (kb_id, from_locator),
              index idx_kr_kb_to (kb_id, to_locator),
              index idx_kr_kb_type (kb_id, relation_type_key)
            )
            """);
        log.info("ensured knowledge relation tables for mysql");
    }

    private void ensurePostgresqlSchema() {
        jdbcTemplate.execute("""
            create table if not exists relation_type_def (
              id varchar(64) not null primary key,
              kb_id varchar(64) null,
              type_key varchar(64) not null,
              label varchar(64) not null,
              color varchar(32) null,
              icon varchar(64) null,
              bidirectional boolean not null,
              is_system boolean not null,
              enabled boolean not null,
              created_at timestamp(6) not null,
              updated_at timestamp(6) not null,
              constraint uk_relation_type_def_kb_key unique (kb_id, type_key)
            )
            """);
        migrateLegacySystemColumn("postgresql");
        jdbcTemplate.execute("""
            create table if not exists knowledge_relation (
              id varchar(64) not null primary key,
              kb_id varchar(64) not null,
              relation_type_key varchar(64) not null,
              from_anchor_kind varchar(32) not null,
              from_locator varchar(512) not null,
              from_snapshot_json text null,
              to_anchor_kind varchar(32) not null,
              to_locator varchar(512) not null,
              to_snapshot_json text null,
              note varchar(1024) null,
              metadata_json text null,
              source_provenance varchar(32) not null,
              status varchar(16) not null,
              created_at timestamp(6) not null,
              updated_at timestamp(6) not null
            )
            """);
        jdbcTemplate.execute("create index if not exists idx_kr_kb_from on knowledge_relation (kb_id, from_locator)");
        jdbcTemplate.execute("create index if not exists idx_kr_kb_to on knowledge_relation (kb_id, to_locator)");
        jdbcTemplate.execute("create index if not exists idx_kr_kb_type on knowledge_relation (kb_id, relation_type_key)");
        log.info("ensured knowledge relation tables for postgresql");
    }

    private void ensureH2Schema() {
        jdbcTemplate.execute("""
            create table if not exists relation_type_def (
              id varchar(64) not null primary key,
              kb_id varchar(64) null,
              type_key varchar(64) not null,
              label varchar(64) not null,
              color varchar(32) null,
              icon varchar(64) null,
              bidirectional boolean not null,
              is_system boolean not null,
              enabled boolean not null,
              created_at timestamp not null,
              updated_at timestamp not null,
              constraint uk_relation_type_def_kb_key unique (kb_id, type_key)
            )
            """);
        migrateLegacySystemColumn("h2");
        jdbcTemplate.execute("""
            create table if not exists knowledge_relation (
              id varchar(64) not null primary key,
              kb_id varchar(64) not null,
              relation_type_key varchar(64) not null,
              from_anchor_kind varchar(32) not null,
              from_locator varchar(512) not null,
              from_snapshot_json clob null,
              to_anchor_kind varchar(32) not null,
              to_locator varchar(512) not null,
              to_snapshot_json clob null,
              note varchar(1024) null,
              metadata_json clob null,
              source_provenance varchar(32) not null,
              status varchar(16) not null,
              created_at timestamp not null,
              updated_at timestamp not null
            )
            """);
        jdbcTemplate.execute("create index if not exists idx_kr_kb_from on knowledge_relation (kb_id, from_locator)");
        jdbcTemplate.execute("create index if not exists idx_kr_kb_to on knowledge_relation (kb_id, to_locator)");
        jdbcTemplate.execute("create index if not exists idx_kr_kb_type on knowledge_relation (kb_id, relation_type_key)");
        log.info("ensured knowledge relation tables for h2");
    }

    private void migrateLegacySystemColumn(String database) {
        if (!tableExists("relation_type_def")) {
            return;
        }
        boolean hasLegacySystem = columnExists("relation_type_def", "system");
        boolean hasIsSystem = columnExists("relation_type_def", "is_system");
        if (!hasLegacySystem) {
            return;
        }
        try {
            if (hasIsSystem) {
                dropLegacySystemColumn(database);
                log.info("dropped legacy relation_type_def.system column");
                return;
            }
            if ("mysql".equals(database)) {
                jdbcTemplate.execute(
                    "alter table relation_type_def change column `system` is_system bit not null"
                );
            } else if ("postgresql".equals(database)) {
                jdbcTemplate.execute(
                    "alter table relation_type_def rename column \"system\" to is_system"
                );
            } else if ("h2".equals(database)) {
                jdbcTemplate.execute(
                    "alter table relation_type_def alter column system rename to is_system"
                );
            }
            log.info("renamed relation_type_def.system column to is_system");
        } catch (Exception ex) {
            log.warn("failed to migrate relation_type_def.system column", ex);
        }
    }

    private void dropLegacySystemColumn(String database) {
        if ("mysql".equals(database)) {
            jdbcTemplate.execute("alter table relation_type_def drop column `system`");
            return;
        }
        if ("postgresql".equals(database)) {
            jdbcTemplate.execute("alter table relation_type_def drop column \"system\"");
            return;
        }
        if ("h2".equals(database)) {
            jdbcTemplate.execute("alter table relation_type_def drop column system");
        }
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.tables
                where table_schema = database() and table_name = ?
                """,
                Integer.class,
                tableName
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            try {
                jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.columns
                where table_schema = database() and table_name = ? and column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
            );
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private String databaseProductName() {
        return jdbcTemplate.execute((ConnectionCallback<String>) (connection) -> {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName().toLowerCase();
        });
    }
}
