package com.tu.backend.externalresource.config;

import java.sql.DatabaseMetaData;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExternalResourceSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExternalResourceSchemaInitializer.class);

    private static final List<TableSpec> TABLES = List.of(
        new TableSpec(
            "external_resource_item",
            List.of(
                new ColumnSpec("work_id", "varchar(64)"),
                new ColumnSpec("identity_value", "varchar(512)")
            )
        ),
        new TableSpec(
            "external_resource_excerpt",
            List.of(
                new ColumnSpec("excerpt_text", "text")
            )
        )
    );

    private final JdbcTemplate jdbcTemplate;

    public ExternalResourceSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String database = databaseProductName();
            for (TableSpec table : TABLES) {
                if (!tableExists(database, table.name())) {
                    continue;
                }
                for (ColumnSpec column : table.columns()) {
                    relaxNotNull(database, table.name(), column);
                }
            }
        } catch (Exception ex) {
            log.error("failed to relax external resource nullable columns", ex);
        }
    }

    private void relaxNotNull(String database, String tableName, ColumnSpec column) {
        if (!columnExists(database, tableName, column.name()) || columnAllowsNull(database, tableName, column.name())) {
            return;
        }
        try {
            if (database.contains("mysql") || database.contains("mariadb")) {
                jdbcTemplate.execute(
                    "alter table " + tableName + " modify column " + column.name() + " " + column.sqlType() + " null"
                );
                log.info("relaxed external resource column nullability; table={}, column={}", tableName, column.name());
                return;
            }
            if (database.contains("postgresql")) {
                jdbcTemplate.execute(
                    "alter table " + tableName + " alter column " + column.name() + " drop not null"
                );
                log.info("relaxed external resource column nullability; table={}, column={}", tableName, column.name());
                return;
            }
            if (database.contains("h2")) {
                jdbcTemplate.execute(
                    "alter table " + tableName + " alter column " + column.name() + " " + column.sqlType() + " null"
                );
                log.info("relaxed external resource column nullability; table={}, column={}", tableName, column.name());
            }
        } catch (Exception ex) {
            log.warn("failed to relax external resource column; table={}, column={}", tableName, column.name(), ex);
        }
    }

    private String databaseProductName() {
        return jdbcTemplate.execute((ConnectionCallback<String>) (connection) -> {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName().toLowerCase();
        });
    }

    private boolean tableExists(String database, String tableName) {
        try {
            Integer count;
            if (database.contains("mysql") || database.contains("mariadb")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                    Integer.class,
                    tableName
                );
                return count != null && count > 0;
            }
            if (database.contains("postgresql")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = current_schema() and table_name = ?",
                    Integer.class,
                    tableName
                );
                return count != null && count > 0;
            }
            if (database.contains("h2")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where lower(table_name) = ?",
                    Integer.class,
                    tableName.toLowerCase()
                );
                return count != null && count > 0;
            }
            return false;
        } catch (Exception ex) {
            log.warn("failed to check external resource table existence; table={}", tableName, ex);
            return false;
        }
    }

    private boolean columnExists(String database, String tableName, String column) {
        try {
            Integer count;
            if (database.contains("mysql") || database.contains("mariadb")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                    Integer.class,
                    tableName,
                    column
                );
                return count != null && count > 0;
            }
            if (database.contains("postgresql")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.columns where table_schema = current_schema() and table_name = ? and column_name = ?",
                    Integer.class,
                    tableName,
                    column
                );
                return count != null && count > 0;
            }
            if (database.contains("h2")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.columns where lower(table_name) = ? and lower(column_name) = ?",
                    Integer.class,
                    tableName.toLowerCase(),
                    column.toLowerCase()
                );
                return count != null && count > 0;
            }
            return false;
        } catch (Exception ex) {
            log.warn("failed to inspect external resource column; table={}, column={}", tableName, column, ex);
            return false;
        }
    }

    private boolean columnAllowsNull(String database, String tableName, String column) {
        try {
            String nullable;
            if (database.contains("mysql") || database.contains("mariadb")) {
                nullable = jdbcTemplate.queryForObject(
                    "select is_nullable from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                    String.class,
                    tableName,
                    column
                );
            } else if (database.contains("postgresql")) {
                nullable = jdbcTemplate.queryForObject(
                    "select is_nullable from information_schema.columns where table_schema = current_schema() and table_name = ? and column_name = ?",
                    String.class,
                    tableName,
                    column
                );
            } else if (database.contains("h2")) {
                nullable = jdbcTemplate.queryForObject(
                    "select is_nullable from information_schema.columns where lower(table_name) = ? and lower(column_name) = ?",
                    String.class,
                    tableName.toLowerCase(),
                    column.toLowerCase()
                );
            } else {
                return true;
            }
            return "YES".equalsIgnoreCase(nullable) || "TRUE".equalsIgnoreCase(nullable);
        } catch (Exception ex) {
            log.warn("failed to inspect external resource column nullability; table={}, column={}", tableName, column, ex);
            return false;
        }
    }

    private record TableSpec(String name, List<ColumnSpec> columns) {
    }

    private record ColumnSpec(String name, String sqlType) {
    }
}
