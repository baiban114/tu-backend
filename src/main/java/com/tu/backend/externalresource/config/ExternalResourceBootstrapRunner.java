package com.tu.backend.externalresource.config;

import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ExternalResourceBootstrapRunner implements ApplicationRunner {

    private static final String BOOK_TYPE_CODE = "book";
    private static final String BOOK_TYPE_NAME = "图书";

    private final ResourceTypeRepository typeRepository;

    public ExternalResourceBootstrapRunner(ResourceTypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ResourceTypeEntity entity = typeRepository.findByCode(BOOK_TYPE_CODE)
            .or(() -> typeRepository.findByName(BOOK_TYPE_NAME))
            .orElseGet(() -> {
                ResourceTypeEntity created = new ResourceTypeEntity();
                created.setId("rt-" + UUID.randomUUID().toString().replace("-", ""));
                return created;
            });

        entity.setCode(BOOK_TYPE_CODE);
        entity.setName(BOOK_TYPE_NAME);
        if (entity.getIcon() == null || entity.getIcon().isBlank()) {
            entity.setIcon("book");
        }
        if (entity.getDescription() == null || entity.getDescription().isBlank()) {
            entity.setDescription("图书资源，支持节选片段管理");
        }
        if (entity.getIdentityFieldKey() == null || entity.getIdentityFieldKey().isBlank()) {
            entity.setIdentityFieldKey("isbn");
        }
        if (entity.getIdentityFieldLabel() == null || entity.getIdentityFieldLabel().isBlank()) {
            entity.setIdentityFieldLabel("ISBN");
        }
        typeRepository.save(entity);
    }
}
