package com.tu.backend.contenttree.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class HeadingSearchSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HeadingSearchSchemaInitializer.class);

    private final HeadingSearchElasticsearchClient headingClient;

    public HeadingSearchSchemaInitializer(
        @Autowired(required = false) HeadingSearchElasticsearchClient headingClient
    ) {
        this.headingClient = headingClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (headingClient == null) {
            return;
        }
        try {
            headingClient.ensureIndex();
        } catch (Exception ex) {
            log.warn("failed to ensure headings search index", ex);
        }
    }
}
