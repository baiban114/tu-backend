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
import com.tu.backend.rag.RagIndexService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            .map(entity -> new PageContentDto(pageId, deserializeBlocks(entity.getBlocksJson())))
            .orElseGet(() -> new PageContentDto(pageId, List.of()));
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
        entity.setBlocksJson(serializeBlocks(request.blocks()));

        PageContentEntity saved = pageContentRepository.save(entity);
        referenceService.rebuildPageReferences(saved.getPageId(), saved.getBlocksJson());
        ragIndexService.indexPageBestEffort(saved.getPageId());
        return new PageContentDto(saved.getPageId(), deserializeBlocks(saved.getBlocksJson()));
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
}
