package com.tu.backend.annotation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.annotation.dto.OrphanedAnnotationDto;
import com.tu.backend.annotation.entity.AnnotationEntity;
import com.tu.backend.annotation.repository.AnnotationRepository;
import com.tu.backend.common.BusinessException;
import com.tu.backend.common.PageResponse;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrphanedAnnotationService {

    private static final Logger log = LoggerFactory.getLogger(OrphanedAnnotationService.class);

    private final AnnotationRepository annotationRepository;
    private final PageContentRepository pageContentRepository;
    private final PageRepository pageRepository;
    private final ObjectMapper objectMapper;

    public OrphanedAnnotationService(
        AnnotationRepository annotationRepository,
        PageContentRepository pageContentRepository,
        PageRepository pageRepository,
        ObjectMapper objectMapper
    ) {
        this.annotationRepository = annotationRepository;
        this.pageContentRepository = pageContentRepository;
        this.pageRepository = pageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int orphanAnnotationsForPages(List<String> pageIds) {
        if (pageIds.isEmpty()) {
            return 0;
        }

        Map<String, String> pageTitles = pageRepository.findAllById(pageIds)
            .stream()
            .collect(Collectors.toMap(PageEntity::getId, PageEntity::getTitle));

        List<PageContentEntity> contents = pageContentRepository.findAllById(pageIds);
        List<AnnotationEntity> orphaned = new ArrayList<>();

        for (PageContentEntity content : contents) {
            String pageId = content.getPageId();
            String pageTitle = pageTitles.getOrDefault(pageId, "未知页面");

            try {
                JsonNode blocks = objectMapper.readTree(content.getBlocksJson());
                if (blocks instanceof com.fasterxml.jackson.databind.node.ArrayNode array) {
                    for (JsonNode block : array) {
                        extractAnnotationsFromBlock(block, pageId, pageTitle, orphaned);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to extract annotations from page {}: {}", pageId, ex.getMessage());
            }
        }

        if (orphaned.isEmpty()) {
            return 0;
        }

        annotationRepository.saveAll(orphaned);
        log.info("Orphaned {} annotations from {} pages", orphaned.size(), pageIds.size());
        return orphaned.size();
    }

    private void extractAnnotationsFromBlock(JsonNode block, String pageId, String pageTitle, List<AnnotationEntity> result) {
        if (!(block instanceof com.fasterxml.jackson.databind.node.ObjectNode)) {
            return;
        }

        String blockId = block.path("id").asText("");
        String blockType = block.path("type").asText("");

        JsonNode metadata = block.get("metadata");
        if (metadata instanceof com.fasterxml.jackson.databind.node.ObjectNode metaNode) {
            JsonNode annotations = metaNode.get("annotations");
            if (annotations instanceof com.fasterxml.jackson.databind.node.ArrayNode annArray) {
                for (JsonNode ann : annArray) {
                    AnnotationEntity entity = new AnnotationEntity();
                    entity.setId("ann-" + UUID.randomUUID().toString().replace("-", ""));
                    entity.setPageId(pageId);
                    entity.setBlockId(blockId);
                    entity.setPageTitle(pageTitle);
                    entity.setBlockType(blockType);

                    entity.setSelectedText(ann.path("selectedText").asText(""));
                    entity.setContextBefore(ann.path("contextBefore").asText(""));
                    entity.setContextAfter(ann.path("contextAfter").asText(""));
                    entity.setNote(ann.path("note").asText(""));
                    entity.setColor(ann.path("color").asText(""));
                    entity.setScope(ann.path("scope").asText("text"));

                    JsonNode fromNode = ann.get("from");
                    if (fromNode != null && fromNode.isInt()) {
                        entity.setFrom(fromNode.asInt());
                    }
                    JsonNode toNode = ann.get("to");
                    if (toNode != null && toNode.isInt()) {
                        entity.setTo(toNode.asInt());
                    }

                    entity.setStatus("orphaned");
                    entity.setOrphanedAt(LocalDateTime.now());

                    result.add(entity);
                }
            }
        }

        JsonNode children = block.get("children");
        if (children instanceof com.fasterxml.jackson.databind.node.ArrayNode childArray) {
            for (JsonNode child : childArray) {
                extractAnnotationsFromBlock(child, pageId, pageTitle, result);
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<OrphanedAnnotationDto> listOrphaned(int page, int pageSize) {
        Page<AnnotationEntity> entityPage = annotationRepository
            .findByStatusOrderByOrphanedAtDesc("orphaned", PageRequest.of(page, pageSize));

        List<OrphanedAnnotationDto> items = entityPage.getContent()
            .stream()
            .map(this::toDto)
            .toList();

        return PageResponse.of(items, entityPage.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public long countOrphaned() {
        return annotationRepository.countByStatus("orphaned");
    }

    @Transactional
    public void deleteOrphaned(String id) {
        AnnotationEntity entity = annotationRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "annotation not found"));
        if (!"orphaned".equals(entity.getStatus())) {
            throw new BusinessException(40000, "only orphaned annotations can be deleted via this endpoint");
        }
        annotationRepository.delete(entity);
    }

    @Transactional
    public int clearAllOrphaned() {
        List<AnnotationEntity> all = annotationRepository.findByStatusOrderByOrphanedAtDesc("orphaned",
            PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        if (all.isEmpty()) {
            return 0;
        }
        annotationRepository.deleteAll(all);
        log.info("Cleared {} orphaned annotations", all.size());
        return all.size();
    }

    private OrphanedAnnotationDto toDto(AnnotationEntity entity) {
        OrphanedAnnotationDto dto = new OrphanedAnnotationDto();
        dto.setId(entity.getId());
        dto.setPageId(entity.getPageId());
        dto.setBlockId(entity.getBlockId());
        dto.setSelectedText(entity.getSelectedText());
        dto.setContextBefore(entity.getContextBefore());
        dto.setContextAfter(entity.getContextAfter());
        dto.setNote(entity.getNote());
        dto.setColor(entity.getColor());
        dto.setScope(entity.getScope());
        dto.setFrom(entity.getFrom());
        dto.setTo(entity.getTo());
        dto.setPageTitle(entity.getPageTitle());
        dto.setBlockType(entity.getBlockType());
        dto.setOrphanedAt(entity.getOrphanedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
