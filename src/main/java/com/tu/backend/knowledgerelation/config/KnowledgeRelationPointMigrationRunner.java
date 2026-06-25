package com.tu.backend.knowledgerelation.config;

import com.tu.backend.knowledgerelation.service.KnowledgeRelationService;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class KnowledgeRelationPointMigrationRunner {

    @Bean
    @Order(110)
    ApplicationRunner migrateLegacyRelationsToPoints(
        KnowledgeRelationService knowledgeRelationService,
        KnowledgeBaseRepository knowledgeBaseRepository
    ) {
        return args -> {
            for (String kbId : knowledgeBaseRepository.findAll().stream().map(entity -> entity.getId()).toList()) {
                knowledgeRelationService.migrateLegacyAnchorRelations(kbId);
            }
        };
    }
}
