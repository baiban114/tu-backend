package com.tu.backend.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import com.tu.backend.content.dto.PageContentDto;
import com.tu.backend.content.dto.SavePageContentRequest;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.reference.service.ReferenceService;
import com.tu.backend.index.PageIndexCoordinator;
import com.tu.backend.knowledgerelation.service.KnowledgeRelationRebuildService;
import com.tu.backend.knowledgerelation.service.KnowledgeRelationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageContentService {

    private static final TypeReference<List<Object>> OBJECT_LIST_TYPE = new TypeReference<>() {
    };

    private static final int SCHEMA_VERSION_V2 = 2;

    private final PageContentRepository pageContentRepository;
    private final PageRepository pageRepository;
    private final ObjectMapper objectMapper;
    private final PageIndexCoordinator pageIndexCoordinator;
    private final ReferenceService referenceService;
    private final KnowledgeRelationRebuildService knowledgeRelationRebuildService;
    private final KnowledgeRelationService knowledgeRelationService;

    public PageContentService(
        PageContentRepository pageContentRepository,
        PageRepository pageRepository,
        ObjectMapper objectMapper,
        PageIndexCoordinator pageIndexCoordinator,
        ReferenceService referenceService,
        KnowledgeRelationRebuildService knowledgeRelationRebuildService,
        KnowledgeRelationService knowledgeRelationService
    ) {
        this.pageContentRepository = pageContentRepository;
        this.pageRepository = pageRepository;
        this.objectMapper = objectMapper;
        this.pageIndexCoordinator = pageIndexCoordinator;
        this.referenceService = referenceService;
        this.knowledgeRelationRebuildService = knowledgeRelationRebuildService;
        this.knowledgeRelationService = knowledgeRelationService;
    }

    @Transactional(readOnly = true)
    public PageContentDto getContent(String pageId) {
        ensurePageExists(pageId);
        return pageContentRepository.findById(pageId)
            .map(entity -> toPageContentDto(pageId, deserializeBlocks(entity.getBlocksJson())))
            .orElseGet(() -> emptyPageContentDto(pageId));
    }

    @Transactional
    public PageContentDto saveContent(String pageId, SavePageContentRequest request) {
        ensurePageExists(pageId);

        PageContentEntity entity = pageContentRepository.findById(pageId)
            .orElseGet(() -> {
                PageContentEntity created = new PageContentEntity();
                created.setPageId(pageId);
                return created;
            });
        entity.setBlocksJson(serializeBlocks(toBlocks(request)));

        PageContentEntity saved = pageContentRepository.save(entity);
        referenceService.rebuildPageReferences(saved.getPageId(), saved.getBlocksJson());
        knowledgeRelationRebuildService.rebuildPageRelations(saved.getPageId(), saved.getBlocksJson());
        pageIndexCoordinator.onPageContentChanged(saved.getPageId());
        return toPageContentDto(saved.getPageId(), deserializeBlocks(saved.getBlocksJson()));
    }

    @Transactional
    public void deleteByPageIds(List<String> pageIds) {
        if (pageIds.isEmpty()) {
            return;
        }
        referenceService.deleteByPageIds(pageIds);
        knowledgeRelationService.deleteByPageIds(pageIds);
        pageContentRepository.deleteByPageIdIn(pageIds);
    }

    private PageContentDto emptyPageContentDto(String pageId) {
        return new PageContentDto(pageId, "", List.of(), List.of(), Map.of(), List.of(), null, null);
    }

    private void ensurePageExists(String pageId) {
        pageRepository.findById(pageId)
            .orElseThrow(() -> new BusinessException(40001, "page not found"));
    }

    private String serializeBlocks(List<Object> blocks) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(40000, "invalid blocks payload");
        }
    }

    private List<Object> deserializeBlocks(String blocksJson) {
        try {
            return objectMapper.readValue(blocksJson, OBJECT_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50000, "failed to deserialize page content");
        }
    }

    private List<Object> toBlocks(SavePageContentRequest request) {
        if (request.blocks() != null) {
            return request.blocks();
        }

        List<Object> blocks = new ArrayList<>();
        Map<String, Object> metadata = request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata());
        metadata.put("annotations", request.annotations() == null ? List.of() : request.annotations());

        if (request.document() != null) {
            Map<String, Object> richText = new HashMap<>();
            richText.put("id", "page-content");
            richText.put("type", "richtext");
            richText.put("document", request.document());
            richText.put("content", request.content() == null ? "" : request.content());
            int schemaVersion = request.schemaVersion() == null ? SCHEMA_VERSION_V2 : request.schemaVersion();
            metadata.put("schemaVersion", schemaVersion);
            richText.put("metadata", metadata);
            blocks.add(richText);
            return blocks;
        }

        Map<String, Object> richText = new HashMap<>();
        richText.put("id", "page-content");
        richText.put("type", "richtext");
        richText.put("content", request.content() == null ? "" : request.content());
        richText.put("metadata", metadata);
        blocks.add(richText);

        if (request.embeds() != null) {
            blocks.addAll(request.embeds());
        }
        return blocks;
    }

    @SuppressWarnings("unchecked")
    private PageContentDto toPageContentDto(String pageId, List<Object> blocks) {
        String content = "";
        List<Object> embeds = new ArrayList<>();
        List<Object> annotations = List.of();
        Map<String, Object> metadata = Map.of();
        Object document = null;
        Integer schemaVersion = null;

        for (Object block : blocks) {
            JsonNode node = objectMapper.valueToTree(block);
            String type = node.path("type").asText("");
            if ("richtext".equalsIgnoreCase(type) || "richText".equals(type)) {
                if (content.isEmpty() && document == null) {
                    content = node.path("content").asText("");
                    JsonNode documentNode = node.path("document");
                    if (!documentNode.isMissingNode() && documentNode.isObject()) {
                        document = objectMapper.convertValue(documentNode, Map.class);
                    }
                    JsonNode metadataNode = node.path("metadata");
                    if (metadataNode.isObject()) {
                        metadata = objectMapper.convertValue(metadataNode, Map.class);
                        Object rawAnnotations = metadata.get("annotations");
                        if (rawAnnotations instanceof List<?> list) {
                            annotations = new ArrayList<>(list);
                        }
                        Object rawSchemaVersion = metadata.get("schemaVersion");
                        if (rawSchemaVersion instanceof Number number) {
                            schemaVersion = number.intValue();
                        }
                    }
                    if (document != null && schemaVersion == null) {
                        schemaVersion = SCHEMA_VERSION_V2;
                    }
                }
            } else {
                embeds.add(block);
            }
        }

        return new PageContentDto(
            pageId,
            content,
            embeds,
            annotations,
            metadata,
            blocks,
            document,
            schemaVersion
        );
    }
}
