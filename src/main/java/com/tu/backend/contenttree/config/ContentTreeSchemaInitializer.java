package com.tu.backend.contenttree.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContentTreeSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentTreeSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public ContentTreeSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            migrateResourceChapters();
        } catch (Exception ex) {
            log.error("failed to migrate external_resource_chapter into content_tree_node", ex);
        }
    }

    private void migrateResourceChapters() {
        if (!tableExists("external_resource_chapter") || !tableExists("content_tree_node")) {
            return;
        }
        Integer migratedCount = jdbcTemplate.queryForObject(
            "select count(*) from content_tree_node where scope_type = 'resource_item'",
            Integer.class
        );
        if (migratedCount != null && migratedCount > 0) {
            return;
        }
        Integer chapterCount = jdbcTemplate.queryForObject(
            "select count(*) from external_resource_chapter",
            Integer.class
        );
        if (chapterCount == null || chapterCount == 0) {
            return;
        }
        jdbcTemplate.update("""
            insert into content_tree_node (
              id, scope_type, scope_id, parent_id, title, sort_order,
              locator, note, created_at, updated_at
            )
            select
              id,
              'resource_item',
              resource_item_id,
              parent_id,
              title,
              sort_order,
              locator,
              note,
              created_at,
              updated_at
            from external_resource_chapter
            """);
        log.info("migrated {} resource chapters into content_tree_node", chapterCount);
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
}
