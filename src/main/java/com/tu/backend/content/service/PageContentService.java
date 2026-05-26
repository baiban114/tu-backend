package com.tu.backend.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tu.backend.common.BusinessException;
import com.tu.backend.content.dto.PageContentDto;
import com.tu.backend.content.dto.SavePageContentRequest;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.reference.service.ReferenceService;
import com.tu.backend.rag.RagIndexService;
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

    private final PageContentRepository pageContentRepository;
    private final PageRepository pageRepository;
    private final ObjectMapper objectMapper;
    private final RagIndexService ragIndexService;
    private final ReferenceService referenceService;

    public PageContentService(
        PageContentRepository pageContentRepository,
        PageRepository pageRepository,
        ObjectMapper objectMapper,
        RagIndexService ragIndexService,
        ReferenceService referenceService
    ) {
        this.pageContentRepository = pageContentRepository;
        this.pageRepository = pageRepository;
        this.objectMapper = objectMapper;
        this.ragIndexService = ragIndexService;
        this.referenceService = referenceService;
    }

    @Transactional(readOnly = true)
    public PageContentDto getContent(String pageId) {
        ensurePageExists(pageId);
        return pageContentRepository.findById(pageId)
            .map(entity -> toPageContentDto(pageId, deserializeBlocks(entity.getBlocksJson())))
            .orElseGet(() -> new PageContentDto(pageId, "", List.of(), List.of(), Map.of(), List.of()));
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
        ragIndexService.indexPageBestEffort(saved.getPageId());
        return toPageContentDto(saved.getPageId(), deserializeBlocks(saved.getBlocksJson()));
    }

    @Transactional
    public void deleteByPageIds(List<String> pageIds) {
        if (pageIds.isEmpty()) {
            return;
        }
        referenceService.deleteByPageIds(pageIds);
        pageContentRepository.deleteByPageIdIn(pageIds);
    }

    private void ensurePageExists(String pageId) {
        PageEntity page = pageRepository.findById(pageId)
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
        Map<String, Object> richText = new HashMap<>();
        richText.put("id", "page-content");
        richText.put("type", "richtext");
        richText.put("content", request.content() == null ? "" : request.content());

        Map<String, Object> metadata = request.metadata() == null ? new HashMap<>() : new HashMap<>(request.metadata());
        metadata.put("annotations", request.annotations() == null ? List.of() : request.annotations());
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

        for (Object block : blocks) {
            JsonNode node = objectMapper.valueToTree(block);
            String type = node.path("type").asText("");
            if ("richtext".equalsIgnoreCase(type) || "richText".equals(type)) {
                if (content.isEmpty()) {
                    content = node.path("content").asText("");
                    JsonNode metadataNode = node.path("metadata");
                    if (metadataNode.isObject()) {
                        metadata = objectMapper.convertValue(metadataNode, Map.class);
                        Object rawAnnotations = metadata.get("annotations");
                        if (rawAnnotations instanceof List<?> list) {
                            annotations = new ArrayList<>(list);
                        }
                    }
                }
            } else {
                embeds.add(block);
            }
        }

        return new PageContentDto(pageId, content, embeds, annotations, metadata, blocks);
    }
}
