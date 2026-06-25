package com.tu.backend.knowledgerelation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tu.backend.content.tiptap.TiptapDocumentWalker;
import com.tu.backend.knowledgerelation.dto.KnowledgeAnchorDto;
import com.tu.backend.knowledgerelation.entity.KnowledgeRelationEntity;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeRelationRebuildService {

    private final PageRepository pageRepository;
    private final KnowledgeRelationService knowledgeRelationService;
    private final KnowledgePointService knowledgePointService;
    private final ObjectMapper objectMapper;

    public KnowledgeRelationRebuildService(
        PageRepository pageRepository,
        KnowledgeRelationService knowledgeRelationService,
        KnowledgePointService knowledgePointService,
        ObjectMapper objectMapper
    ) {
        this.pageRepository = pageRepository;
        this.knowledgeRelationService = knowledgeRelationService;
        this.knowledgePointService = knowledgePointService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void rebuildPageRelations(String pageId, String blocksJson) {
        PageEntity page = pageRepository.findById(pageId).orElse(null);
        if (page == null) {
            return;
        }
        String kbId = page.getKbId();
        knowledgeRelationService.deleteMigratedForPage(kbId, pageId);

        ArrayNode blocks = deserializeBlocks(blocksJson);
        List<KnowledgeRelationEntity> migrated = new ArrayList<>();
        for (JsonNode block : blocks) {
            collectFromBlock(pageId, kbId, block, migrated);
        }
        knowledgeRelationService.saveMigratedBatch(migrated);
    }

    private void collectFromBlock(String pageId, String kbId, JsonNode block, List<KnowledgeRelationEntity> migrated) {
        if (!block.isObject()) {
            return;
        }
        String blockId = text(block.get("id"));
        String blockType = text(block.get("type"));
        if ("richtext".equalsIgnoreCase(blockType) || "richText".equals(blockType)) {
            JsonNode document = block.get("document");
            if (TiptapDocumentWalker.isDocument(document)) {
                TiptapDocumentWalker.collectReferences(pageId, blockId, "document", document, new TiptapDocumentWalker.ReferenceSink() {
                    @Override
                    public void onRef(String pageId, String blockId, String blockPath, String refId, String refType) {
                    }

                    @Override
                    public void onExternalResource(String pageId, String blockId, String blockPath, String sourceLocator, JsonNode externalResource) {
                    }

                    @Override
                    public void onHeadingSource(String pageId, String parentBlockId, String headingBlockId, String resourceItemId, String resourceExcerptId) {
                        if (resourceExcerptId == null || resourceExcerptId.isBlank()) {
                            return;
                        }
                        KnowledgeAnchorDto from = headingAnchor(pageId, headingBlockId, block);
                        KnowledgeAnchorDto to = resourceExcerptAnchor(resourceItemId, resourceExcerptId, null);
                        addMigratedPointRelation(kbId, "source", from, to, null, migrated);
                    }

                    @Override
                    public void onGraphData(String pageId, String blockId, String blockType, String blockPath, JsonNode graphData) {
                    }

                    @Override
                    public void onTableData(String pageId, String blockId, String blockType, String blockPath, JsonNode tableData) {
                    }

                    @Override
                    public void onExternalUrl(String pageId, String blockId, String blockPath, String url) {
                    }
                });
            }
            extractBasisAnnotations(pageId, kbId, block, migrated);
        }
    }

    private void addMigratedPointRelation(
        String kbId,
        String relationTypeKey,
        KnowledgeAnchorDto from,
        KnowledgeAnchorDto to,
        String note,
        List<KnowledgeRelationEntity> migrated
    ) {
        String fromPointId = knowledgePointService.ensurePointForAnchor(
            kbId,
            from,
            titleFromSnapshot(from.snapshot()),
            null
        );
        String toPointId = knowledgePointService.ensurePointForAnchor(
            kbId,
            to,
            titleFromSnapshot(to.snapshot()),
            null
        );
        migrated.add(knowledgeRelationService.buildMigratedPointEntity(
            kbId,
            relationTypeKey,
            fromPointId,
            toPointId,
            from,
            to,
            note
        ));
    }

    private void extractBasisAnnotations(String pageId, String kbId, JsonNode block, List<KnowledgeRelationEntity> migrated) {
        JsonNode metadata = block.get("metadata");
        if (metadata == null || !metadata.isObject()) {
            return;
        }
        JsonNode annotations = metadata.get("annotations");
        if (!(annotations instanceof ArrayNode annArray)) {
            return;
        }
        for (JsonNode ann : annArray) {
            if (!"basis".equalsIgnoreCase(text(ann.get("kind")))) {
                continue;
            }
            String annId = text(ann.get("id"));
            if (annId.isBlank()) {
                continue;
            }
            JsonNode binding = ann.get("basisBinding");
            if (binding == null || !binding.isObject()) {
                continue;
            }
            String resourceItemId = text(binding.get("resourceItemId"));
            if (resourceItemId.isBlank()) {
                continue;
            }
            String resourceExcerptId = text(binding.get("resourceExcerptId"));
            KnowledgeAnchorDto to = resourceExcerptId.isBlank()
                ? resourceItemAnchor(resourceItemId, binding)
                : resourceExcerptAnchor(resourceItemId, resourceExcerptId, binding);
            Map<String, Object> fromSnapshot = new HashMap<>();
            String selectedText = text(ann.get("selectedText"));
            if (!selectedText.isBlank()) {
                fromSnapshot.put("title", truncate(selectedText, 120));
            }
            KnowledgeAnchorDto from = new KnowledgeAnchorDto(
                "annotation",
                "page:" + pageId + ":annotation:" + annId,
                fromSnapshot
            );
            addMigratedPointRelation(kbId, "basis", from, to, text(ann.get("note")), migrated);
        }
    }

    private KnowledgeAnchorDto headingAnchor(String pageId, String headingBlockId, JsonNode block) {
        Map<String, Object> snapshot = new HashMap<>();
        String title = findHeadingTitle(block, headingBlockId);
        if (!title.isBlank()) {
            snapshot.put("title", title);
        }
        snapshot.put("pageId", pageId);
        return new KnowledgeAnchorDto("heading", "page:" + pageId + ":heading:" + headingBlockId, snapshot);
    }

    private String findHeadingTitle(JsonNode block, String headingBlockId) {
        JsonNode document = block.get("document");
        if (!TiptapDocumentWalker.isDocument(document)) {
            return "";
        }
        return TiptapDocumentWalker.extractHeadings(document, headingBlockId).stream()
            .filter(heading -> headingBlockId.equals(heading.blockId()))
            .map(TiptapDocumentWalker.TiptapHeading::text)
            .findFirst()
            .orElse("");
    }

    private KnowledgeAnchorDto resourceItemAnchor(String resourceItemId, JsonNode binding) {
        Map<String, Object> snapshot = snapshotFromBinding(binding);
        return new KnowledgeAnchorDto("resourceItem", "resource:" + resourceItemId, snapshot);
    }

    private KnowledgeAnchorDto resourceExcerptAnchor(String resourceItemId, String resourceExcerptId, JsonNode binding) {
        Map<String, Object> snapshot = snapshotFromBinding(binding);
        if (binding != null && binding.isObject()) {
            JsonNode snap = binding.get("snapshot");
            if (snap != null && snap.isObject()) {
                putIfPresent(snapshot, "excerptTitle", text(snap.get("excerptTitle")));
            }
        }
        return new KnowledgeAnchorDto(
            "resourceExcerpt",
            "resource:" + resourceItemId + ":excerpt:" + resourceExcerptId,
            snapshot
        );
    }

    private Map<String, Object> snapshotFromBinding(JsonNode binding) {
        Map<String, Object> snapshot = new HashMap<>();
        if (binding == null || !binding.isObject()) {
            return snapshot;
        }
        JsonNode snap = binding.get("snapshot");
        if (snap != null && snap.isObject()) {
            putIfPresent(snapshot, "title", text(snap.get("resourceTitle")));
            putIfPresent(snapshot, "excerptTitle", text(snap.get("excerptTitle")));
            putIfPresent(snapshot, "resourceTypeName", text(snap.get("resourceTypeName")));
        }
        return snapshot;
    }

    private String titleFromSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null) {
            return null;
        }
        Object title = snapshot.get("title");
        if (title == null) {
            title = snapshot.get("excerptTitle");
        }
        return title == null ? null : title.toString();
    }

    private void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private ArrayNode deserializeBlocks(String blocksJson) {
        try {
            JsonNode node = objectMapper.readTree(blocksJson == null ? "[]" : blocksJson);
            if (node instanceof ArrayNode array) {
                return array;
            }
            return objectMapper.createArrayNode();
        } catch (Exception ex) {
            return objectMapper.createArrayNode();
        }
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        return node.asText("").trim();
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
