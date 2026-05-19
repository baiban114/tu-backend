package com.tu.backend.reference.config;

import com.tu.backend.reference.service.ReferenceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ReferenceBootstrapRunner {

    @Bean
    @Order(10)
    CommandLineRunner rebuildReferencesOnBootstrap(ReferenceService referenceService) {
        return args -> {
            if (referenceService.shouldRunBootstrapRebuild()) {
                referenceService.rebuildAll();
            }
        };
    }
}
