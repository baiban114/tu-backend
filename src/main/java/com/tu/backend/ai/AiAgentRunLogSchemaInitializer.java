package com.tu.backend.ai;

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
public class AiAgentRunLogSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiAgentRunLogSchemaInitializer.class);
    private static final String TABLE_NAME = "ai_agent_run_log";
    private static final List<String> TEXT_COLUMNS = List.of(
        "system_prompt",
        "user_prompt",
        "request_body_json",
        "raw_response_body",
        "output_text",
        "error_message"
    );

    private final JdbcTemplate jdbcTemplate;

    public AiAgentRunLogSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String database = databaseProductName();
            if (!tableExists(database)) {
                return;
            }
            if (database.contains("mysql") || database.contains("mariadb")) {
                for (String column : TEXT_COLUMNS) {
                    if (shouldAlterColumn(database, column, "longtext")) {
                        jdbcTemplate.execute("alter table " + TABLE_NAME + " modify column " + column + " longtext null");
                    }
                }
                return;
            }
            if (database.contains("postgresql")) {
                for (String column : TEXT_COLUMNS) {
                    if (shouldAlterColumn(database, column, "text")) {
                        jdbcTemplate.execute("alter table " + TABLE_NAME + " alter column " + column + " type text");
                    }
                }
            }
        } catch (Exception ex) {
            log.error("failed to ensure ai agent run log large text columns", ex);
        }
    }

    private String databaseProductName() {
        return jdbcTemplate.execute((ConnectionCallback<String>) (connection) -> {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName().toLowerCase();
        });
    }

    private boolean tableExists(String database) {
        try {
            Integer count;
            if (database.contains("mysql") || database.contains("mariadb")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                    Integer.class,
                    TABLE_NAME
                );
                return count != null && count > 0;
            }
            if (database.contains("postgresql")) {
                count = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = current_schema() and table_name = ?",
                    Integer.class,
                    TABLE_NAME
                );
                return count != null && count > 0;
            }
            return false;
        } catch (Exception ex) {
            log.warn("failed to check ai agent run log table existence", ex);
            return false;
        }
    }

    private boolean shouldAlterColumn(String database, String column, String expectedType) {
        try {
            String actualType;
            if (database.contains("mysql") || database.contains("mariadb")) {
                actualType = jdbcTemplate.queryForObject(
                    "select data_type from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                    String.class,
                    TABLE_NAME,
                    column
                );
            } else if (database.contains("postgresql")) {
                actualType = jdbcTemplate.queryForObject(
                    "select data_type from information_schema.columns where table_schema = current_schema() and table_name = ? and column_name = ?",
                    String.class,
                    TABLE_NAME,
                    column
                );
            } else {
                return false;
            }
            return actualType == null || !expectedType.equalsIgnoreCase(actualType);
        } catch (Exception ex) {
            log.warn("failed to inspect ai agent run log column type; column={}", column, ex);
            return false;
        }
    }
}
