package com.tu.backend.knowledge.service;

import com.tu.backend.common.BusinessException;
import com.tu.backend.knowledge.dto.CreateKnowledgeBaseRequest;
import com.tu.backend.knowledge.dto.KnowledgeBaseDto;
import com.tu.backend.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.tu.backend.knowledge.entity.KnowledgeBaseEntity;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.page.service.PageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PageService pageService;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository, PageService pageService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.pageService = pageService;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseDto> list() {
        return knowledgeBaseRepository.findAll()
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public KnowledgeBaseDto create(CreateKnowledgeBaseRequest request) {
        String normalizedName = request.name().trim();
        if (knowledgeBaseRepository.existsByName(normalizedName)) {
            throw new BusinessException(40009, "knowledge base name already exists");
        }

        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId("kb-" + UUID.randomUUID().toString().replace("-", ""));
        entity.setName(normalizedName);
        entity.setIcon(request.icon() == null || request.icon().isBlank() ? "📘" : request.icon().trim());
        entity.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        return toDto(knowledgeBaseRepository.save(entity));
    }

    @Transactional
    public KnowledgeBaseDto update(String id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "knowledge base not found"));

        String normalizedName = request.name().trim();
        if (!entity.getName().equals(normalizedName) && knowledgeBaseRepository.existsByName(normalizedName)) {
            throw new BusinessException(40009, "knowledge base name already exists");
        }

        entity.setName(normalizedName);
        return toDto(knowledgeBaseRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "knowledge base not found"));
        pageService.deleteByKnowledgeBaseId(id);
        knowledgeBaseRepository.delete(entity);
    }

    private KnowledgeBaseDto toDto(KnowledgeBaseEntity entity) {
        return new KnowledgeBaseDto(
            entity.getId(),
            entity.getName(),
            entity.getIcon(),
            entity.getDescription()
        );
    }
}
