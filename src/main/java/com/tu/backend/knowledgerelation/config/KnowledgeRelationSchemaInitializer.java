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
        ensureKnowledgePointTables("mysql");
        ensureKnowledgePointAliasTables("mysql");
        migrateKnowledgeRelationPointColumns("mysql");
        log.info("ensured knowledge relation tables for mysql");
    }

    private void ensureKnowledgePointTables(String database) {
        if ("mysql".equals(database)) {
            jdbcTemplate.execute("""
                create table if not exists knowledge_point (
                  id varchar(64) not null primary key,
                  kb_id varchar(64) not null,
                  parent_id varchar(64) null,
                  title varchar(255) not null,
                  summary longtext null,
                  status varchar(16) not null,
                  estimated_hours decimal(10,2) null,
                  sort_order int not null,
                  metadata_json longtext null,
                  created_at datetime(6) not null,
                  updated_at datetime(6) not null
                )
                """);
            jdbcTemplate.execute("""
                create table if not exists knowledge_point_anchor (
                  id varchar(64) not null primary key,
                  knowledge_point_id varchar(64) not null,
                  anchor_kind varchar(32) not null,
                  locator varchar(512) not null,
                  snapshot_json longtext null,
                  role varchar(32) not null,
                  is_primary bit not null,
                  created_at datetime(6) not null,
                  updated_at datetime(6) not null,
                  index idx_kpa_point (knowledge_point_id),
                  index idx_kpa_locator (locator)
                )
                """);
            return;
        }
        if ("postgresql".equals(database)) {
            jdbcTemplate.execute("""
                create table if not exists knowledge_point (
                  id varchar(64) not null primary key,
                  kb_id varchar(64) not null,
                  parent_id varchar(64) null,
                  title varchar(255) not null,
                  summary text null,
                  status varchar(16) not null,
                  estimated_hours numeric(10,2) null,
                  sort_order int not null,
                  metadata_json text null,
                  created_at timestamp(6) not null,
                  updated_at timestamp(6) not null
                )
                """);
            jdbcTemplate.execute("""
                create table if not exists knowledge_point_anchor (
                  id varchar(64) not null primary key,
                  knowledge_point_id varchar(64) not null,
                  anchor_kind varchar(32) not null,
                  locator varchar(512) not null,
                  snapshot_json text null,
                  role varchar(32) not null,
                  is_primary boolean not null,
                  created_at timestamp(6) not null,
                  updated_at timestamp(6) not null
                )
                """);
            jdbcTemplate.execute("create index if not exists idx_kpa_point on knowledge_point_anchor (knowledge_point_id)");
            jdbcTemplate.execute("create index if not exists idx_kpa_locator on knowledge_point_anchor (locator)");
            return;
        }
        if ("h2".equals(database)) {
            jdbcTemplate.execute("""
                create table if not exists knowledge_point (
                  id varchar(64) not null primary key,
                  kb_id varchar(64) not null,
                  parent_id varchar(64) null,
                  title varchar(255) not null,
                  summary clob null,
                  status varchar(16) not null,
                  estimated_hours decimal(10,2) null,
                  sort_order int not null,
                  metadata_json clob null,
                  created_at timestamp not null,
                  updated_at timestamp not null
                )
                """);
            jdbcTemplate.execute("""
                create table if not exists knowledge_point_anchor (
                  id varchar(64) not null primary key,
                  knowledge_point_id varchar(64) not null,
                  anchor_kind varchar(32) not null,
                  locator varchar(512) not null,
                  snapshot_json clob null,
                  role varchar(32) not null,
                  is_primary boolean not null,
                  created_at timestamp not null,
                  updated_at timestamp not null
                )
                """);
            jdbcTemplate.execute("create index if not exists idx_kpa_point on knowledge_point_anchor (knowledge_point_id)");
            jdbcTemplate.execute("create index if not exists idx_kpa_locator on knowledge_point_anchor (locator)");
        }
    }

    private void ensureKnowledgePointAliasTables(String database) {
        if ("mysql".equals(database)) {
            jdbcTemplate.execute("""
                create table if not exists knowledge_point_alias (
                  id varchar(64) not null primary key,
                  knowledge_point_id varchar(64) not null,
                  alias varchar(255) not null,
                  created_at datetime(6) not null,
                  updated_at datetime(6) not null,
                  constraint uk_kpa_point_alias unique (knowledge_point_id, alias),
                  index idx_kpa_alias_point (knowledge_point_id)
                )
                """);
            return;
        }
        if ("postgresql".equals(database)) {
            jdbcTemplate.execute("""
                create table if not exists knowledge_point_alias (
                  id varchar(64) not null primary key,
                  knowledge_point_id varchar(64) not null,
                  alias varchar(255) not null,
                  created_at timestamp(6) not null,
                  updated_at timestamp(6) not null,
                  constraint uk_kpa_point_alias unique (knowledge_point_id, alias)
                )
                """);
            jdbcTemplate.execute("create index if not exists idx_kpa_alias_point on knowledge_point_alias (knowledge_point_id)");
            return;
        }
        if ("h2".equals(database)) {
            jdbcTemplate.execute("""
                create table if not exists knowledge_point_alias (
                  id varchar(64) not null primary key,
                  knowledge_point_id varchar(64) not null,
                  alias varchar(255) not null,
                  created_at timestamp not null,
                  updated_at timestamp not null,
                  constraint uk_kpa_point_alias unique (knowledge_point_id, alias)
                )
                """);
            jdbcTemplate.execute("create index if not exists idx_kpa_alias_point on knowledge_point_alias (knowledge_point_id)");
        }
    }

    private void migrateKnowledgeRelationPointColumns(String database) {
        if (!tableExists("knowledge_relation")) {
            return;
        }
        addColumnIfMissing("knowledge_relation", "from_point_id", switch (database) {
            case "mysql" -> "varchar(64) null";
            case "postgresql" -> "varchar(64)";
            default -> "varchar(64)";
        });
        addColumnIfMissing("knowledge_relation", "to_point_id", switch (database) {
            case "mysql" -> "varchar(64) null";
            case "postgresql" -> "varchar(64)";
            default -> "varchar(64)";
        });
        relaxColumnNullable(database, "knowledge_relation", "from_anchor_kind");
        relaxColumnNullable(database, "knowledge_relation", "from_locator");
        relaxColumnNullable(database, "knowledge_relation", "to_anchor_kind");
        relaxColumnNullable(database, "knowledge_relation", "to_locator");
    }

    private void addColumnIfMissing(String tableName, String columnName, String sqlType) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("alter table " + tableName + " add column " + columnName + " " + sqlType);
    }

    private void relaxColumnNullable(String database, String tableName, String columnName) {
        if (!columnExists(tableName, columnName)) {
            return;
        }
        try {
            if ("mysql".equals(database)) {
                String type = jdbcTemplate.queryForObject(
                    "select column_type from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                    String.class,
                    tableName,
                    columnName
                );
                jdbcTemplate.execute("alter table " + tableName + " modify column " + columnName + " " + type + " null");
            }
        } catch (Exception ex) {
            log.warn("failed to relax nullable column {}.{}", tableName, columnName, ex);
        }
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
        ensureKnowledgePointTables("postgresql");
        ensureKnowledgePointAliasTables("postgresql");
        migrateKnowledgeRelationPointColumns("postgresql");
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
        ensureKnowledgePointTables("h2");
        ensureKnowledgePointAliasTables("h2");
        migrateKnowledgeRelationPointColumns("h2");
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
