package com.tu.backend.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tu.backend.common.BusinessException;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.knowledge.entity.KnowledgeBaseEntity;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.rag.RagDocumentExtractor;
import com.tu.backend.rag.dto.RagIndexDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);

    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ObjectMapper objectMapper;
    private final RagDocumentExtractor documentExtractor;
    private final SearchDocumentMapper searchDocumentMapper;
    private final SearchElasticsearchClient searchClient;
    private final SearchProperties searchProperties;

    public SearchIndexService(
        PageRepository pageRepository,
        PageContentRepository pageContentRepository,
        KnowledgeBaseRepository knowledgeBaseRepository,
        ObjectMapper objectMapper,
        RagDocumentExtractor documentExtractor,
        SearchDocumentMapper searchDocumentMapper,
        @Autowired(required = false) SearchElasticsearchClient searchClient,
        SearchProperties searchProperties
    ) {
        this.pageRepository = pageRepository;
        this.pageContentRepository = pageContentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.objectMapper = objectMapper;
        this.documentExtractor = documentExtractor;
        this.searchDocumentMapper = searchDocumentMapper;
        this.searchClient = searchClient;
        this.searchProperties = searchProperties;
    }

    public void ensureIndexBestEffort() {
        if (!isActive()) {
            return;
        }
        try {
            searchClient.ensureIndex();
        } catch (IOException | RuntimeException ex) {
            log.warn("Failed to ensure search index: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void reindexAll() {
        if (!isActive()) {
            return;
        }
        ensureIndexBestEffort();
        for (KnowledgeBaseEntity kb : knowledgeBaseRepository.findAll()) {
            for (PageEntity page : pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(kb.getId())) {
                indexPage(page.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public void indexPage(String pageId) {
        if (!isActive()) {
            return;
        }

        PageEntity page = pageRepository.findById(pageId)
            .orElseThrow(() -> new BusinessException(40001, "page not found"));
        KnowledgeBaseEntity kb = knowledgeBaseRepository.findById(page.getKbId())
            .orElseThrow(() -> new BusinessException(40001, "knowledge base not found"));

        try {
            searchClient.deleteByPageId(pageId);
        } catch (IOException ex) {
            throw new BusinessException(50000, "failed to clear search index for page");
        }

        PageContentEntity content = pageContentRepository.findById(pageId).orElse(null);
        if (content == null) {
            try {
                searchClient.bulkIndex(List.of(pageTitleDocument(kb, page, null)));
            } catch (IOException ex) {
                throw new BusinessException(50000, "failed to index page title");
            }
            return;
        }

        ArrayNode blocks = deserializeBlocks(content.getBlocksJson());
        LocalDateTime updatedAt = content.getUpdatedAt() == null ? page.getUpdatedAt() : content.getUpdatedAt();
        List<RagIndexDocument> ragDocuments = documentExtractor.extract(
            page.getKbId(),
            page.getId(),
            page.getTitle(),
            blocks,
            updatedAt
        );

        List<SearchDocument> documents = new ArrayList<>();
        for (RagIndexDocument ragDocument : ragDocuments) {
            documents.add(searchDocumentMapper.from(ragDocument, kb.getName(), page.getTitle()));
        }
        if (documents.isEmpty()) {
            documents.add(pageTitleDocument(kb, page, updatedAt));
        }

        try {
            searchClient.bulkIndex(documents);
        } catch (IOException ex) {
            throw new BusinessException(50000, "failed to index page for search");
        }
    }

    public void indexPageBestEffort(String pageId) {
        if (!isActive()) {
            return;
        }
        try {
            indexPage(pageId);
        } catch (RuntimeException ex) {
            log.warn("Failed to index page {} for search: {}", pageId, ex.getMessage());
        }
    }

    public void deletePagesBestEffort(String kbId, List<String> pageIds) {
        if (!isActive()) {
            return;
        }
        for (String pageId : pageIds) {
            try {
                searchClient.deleteByPageId(pageId);
            } catch (IOException | RuntimeException ex) {
                log.warn("Failed to delete page {} from search index: {}", pageId, ex.getMessage());
            }
        }
    }

    public void deleteKnowledgeBaseBestEffort(String kbId) {
        if (!isActive()) {
            return;
        }
        try {
            searchClient.deleteByKbId(kbId);
        } catch (IOException | RuntimeException ex) {
            log.warn("Failed to delete knowledge base {} from search index: {}", kbId, ex.getMessage());
        }
    }

    private SearchDocument pageTitleDocument(KnowledgeBaseEntity kb, PageEntity page, LocalDateTime updatedAt) {
        String updated = updatedAt == null
            ? (page.getUpdatedAt() == null ? null : page.getUpdatedAt().toString())
            : updatedAt.toString();
        return searchDocumentMapper.pageTitleOnly(
            kb.getId(),
            kb.getName(),
            page.getId(),
            page.getTitle(),
            updated
        );
    }

    private ArrayNode deserializeBlocks(String blocksJson) {
        try {
            JsonNode node = objectMapper.readTree(blocksJson);
            if (node instanceof ArrayNode arrayNode) {
                return arrayNode;
            }
            throw new BusinessException(50000, "invalid page content structure");
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50000, "failed to deserialize page content");
        }
    }

    private boolean isActive() {
        return searchProperties.isEnabled() && searchClient != null;
    }
}
