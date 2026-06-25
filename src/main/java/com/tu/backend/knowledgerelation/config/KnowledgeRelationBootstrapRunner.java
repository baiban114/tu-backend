package com.tu.backend.knowledgerelation.config;

import com.tu.backend.knowledgerelation.service.RelationTypeService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class KnowledgeRelationBootstrapRunner {

    @Bean
    @Order(100)
    ApplicationRunner seedRelationTypes(RelationTypeService relationTypeService) {
        return args -> relationTypeService.ensureSystemTypes();
    }
}
