package com.tu.backend.knowledgerelation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.content.tiptap.TiptapDocumentWalker;
import com.tu.backend.knowledgerelation.dto.GenerateKnowledgePointsRequest;
import com.tu.backend.knowledgerelation.dto.KnowledgeAnchorDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointGenerationItemDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointGenerationResultDto;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgePointGenerationService {

    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final KnowledgePointService knowledgePointService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ObjectMapper objectMapper;

    public KnowledgePointGenerationService(
        PageRepository pageRepository,
        PageContentRepository pageContentRepository,
        KnowledgePointService knowledgePointService,
        KnowledgeBaseRepository knowledgeBaseRepository,
        ObjectMapper objectMapper
    ) {
        this.pageRepository = pageRepository;
        this.pageContentRepository = pageContentRepository;
        this.knowledgePointService = knowledgePointService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public KnowledgePointGenerationResultDto generate(String kbId, GenerateKnowledgePointsRequest request) {
        ensureKbExists(kbId);
        Set<String> sources = new HashSet<>(request.sources());
        Set<String> pageFilter = request.pageIds() == null
            ? Set.of()
            : new HashSet<>(request.pageIds().stream().filter(id -> id != null && !id.isBlank()).map(String::trim).toList());

        List<KnowledgePointGenerationItemDto> items = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        List<PageEntity> pages = pageRepository.findByKbIdOrderBySortOrderAscCreatedAtAsc(kbId)
            .stream()
            .filter(page -> pageFilter.isEmpty() || pageFilter.contains(page.getId()))
            .toList();

        if (sources.contains("pageTree")) {
            for (PageEntity page : pages) {
                KnowledgePointGenerationItemDto item = generateForPageAnchor(kbId, page);
                items.add(item);
                switch (item.status()) {
                    case "created" -> created++;
                    case "skipped" -> skipped++;
                    default -> failed++;
                }
            }
        }

        if (sources.contains("documentHeadings")) {
            for (PageEntity page : pages) {
                if (!isDocumentPage(page)) {
                    continue;
                }
                List<KnowledgePointGenerationItemDto> headingItems = generateForDocumentHeadings(kbId, page);
                for (KnowledgePointGenerationItemDto item : headingItems) {
                    items.add(item);
                    switch (item.status()) {
                        case "created" -> created++;
                        case "skipped" -> skipped++;
                        default -> failed++;
                    }
                }
            }
        }

        return new KnowledgePointGenerationResultDto(created, skipped, failed, items);
    }

    private KnowledgePointGenerationItemDto generateForPageAnchor(String kbId, PageEntity page) {
        String locator = "page:" + page.getId();
        try {
            KnowledgeAnchorDto anchor = pageAnchor(page);
            return ensureAnchorPoint(kbId, anchor, page.getTitle(), locator);
        } catch (Exception ex) {
            return new KnowledgePointGenerationItemDto(locator, null, page.getTitle(), "failed");
        }
    }

    private List<KnowledgePointGenerationItemDto> generateForDocumentHeadings(String kbId, PageEntity page) {
        List<KnowledgePointGenerationItemDto> items = new ArrayList<>();
        PageContentEntity content = pageContentRepository.findById(page.getId()).orElse(null);
        if (content == null || content.getBlocksJson() == null || content.getBlocksJson().isBlank()) {
            return items;
        }
        ArrayNode blocks = deserializeBlocks(content.getBlocksJson());
        for (JsonNode block : blocks) {
            if (!block.isObject()) {
                continue;
            }
            String blockType = text(block.get("type"));
            if (!"richtext".equalsIgnoreCase(blockType) && !"richText".equals(blockType)) {
                continue;
            }
            JsonNode document = block.get("document");
            if (!TiptapDocumentWalker.isDocument(document)) {
                continue;
            }
            String defaultBlockId = text(block.get("id"));
            for (TiptapDocumentWalker.TiptapHeading heading : TiptapDocumentWalker.extractHeadings(document, defaultBlockId)) {
                String headingBlockId = heading.blockId();
                String title = heading.text() == null ? "" : heading.text().trim();
                if (headingBlockId == null || headingBlockId.isBlank() || title.isBlank()) {
                    continue;
                }
                String locator = "page:" + page.getId() + ":heading:" + headingBlockId;
                try {
                    KnowledgeAnchorDto anchor = headingAnchor(page.getId(), headingBlockId, title);
                    items.add(ensureAnchorPoint(kbId, anchor, title, locator));
                } catch (Exception ex) {
                    items.add(new KnowledgePointGenerationItemDto(locator, null, title, "failed"));
                }
            }
        }
        return items;
    }

    private KnowledgePointGenerationItemDto ensureAnchorPoint(
        String kbId,
        KnowledgeAnchorDto anchor,
        String title,
        String locator
    ) {
        boolean existed = !knowledgePointService.findPointsByLocator(kbId, locator).isEmpty();
        String pointId = knowledgePointService.ensurePointForAnchor(kbId, anchor, title, null);
        String resolvedTitle = title == null || title.isBlank()
            ? knowledgePointService.getPoint(pointId).getTitle()
            : title.trim();
        return new KnowledgePointGenerationItemDto(locator, pointId, resolvedTitle, existed ? "skipped" : "created");
    }

    private KnowledgeAnchorDto pageAnchor(PageEntity page) {
        Map<String, Object> snapshot = new HashMap<>();
        if (page.getTitle() != null && !page.getTitle().isBlank()) {
            snapshot.put("title", page.getTitle().trim());
        }
        snapshot.put("pageId", page.getId());
        return new KnowledgeAnchorDto("page", "page:" + page.getId(), snapshot);
    }

    private KnowledgeAnchorDto headingAnchor(String pageId, String headingBlockId, String title) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("title", title);
        snapshot.put("pageId", pageId);
        return new KnowledgeAnchorDto("heading", "page:" + pageId + ":heading:" + headingBlockId, snapshot);
    }

    private boolean isDocumentPage(PageEntity page) {
        String pageType = page.getPageType();
        return pageType == null || pageType.isBlank() || "document".equalsIgnoreCase(pageType);
    }

    private ArrayNode deserializeBlocks(String blocksJson) {
        try {
            JsonNode root = objectMapper.readTree(blocksJson);
            if (root instanceof ArrayNode arrayNode) {
                return arrayNode;
            }
            return objectMapper.createArrayNode();
        } catch (Exception ex) {
            return objectMapper.createArrayNode();
        }
    }

    private void ensureKbExists(String kbId) {
        if (!knowledgeBaseRepository.existsById(kbId)) {
            throw new BusinessException(40001, "knowledge base not found");
        }
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }
}
