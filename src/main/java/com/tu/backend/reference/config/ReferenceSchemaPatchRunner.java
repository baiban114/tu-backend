package com.tu.backend.reference.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class ReferenceSchemaPatchRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceSchemaPatchRunner.class);

    @Bean
    @Order(0)
    CommandLineRunner patchReferenceSchema(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            String productName = detectDatabaseProductName(dataSource);
            if (productName == null) {
                return;
            }

            String normalized = productName.toLowerCase();
            try {
                if (normalized.contains("mysql") || normalized.contains("mariadb")) {
                    jdbcTemplate.execute("ALTER TABLE internal_reference_record MODIFY COLUMN block_id VARCHAR(1024) NOT NULL");
                    jdbcTemplate.execute("ALTER TABLE internal_reference_record MODIFY COLUMN target_block_id VARCHAR(1024) NULL");
                    jdbcTemplate.execute("ALTER TABLE external_reference_occurrence MODIFY COLUMN block_id VARCHAR(1024) NOT NULL");
                } else if (normalized.contains("postgresql")) {
                    jdbcTemplate.execute("ALTER TABLE internal_reference_record ALTER COLUMN block_id TYPE VARCHAR(1024)");
                    jdbcTemplate.execute("ALTER TABLE internal_reference_record ALTER COLUMN target_block_id TYPE VARCHAR(1024)");
                    jdbcTemplate.execute("ALTER TABLE external_reference_occurrence ALTER COLUMN block_id TYPE VARCHAR(1024)");
                }
            } catch (Exception ex) {
                log.warn("Failed to patch reference schema widths: {}", ex.getMessage());
            }
        };
    }

    private String detectDatabaseProductName(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception ex) {
            log.warn("Failed to detect database product name: {}", ex.getMessage());
            return null;
        }
    }
}
