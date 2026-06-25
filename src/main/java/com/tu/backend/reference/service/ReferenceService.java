package com.tu.backend.reference.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tu.backend.content.tiptap.TiptapDocumentWalker;
import com.tu.backend.common.BusinessException;
import com.tu.backend.common.PageResponse;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.externalresource.entity.ResourceExcerptEntity;
import com.tu.backend.externalresource.entity.ResourceItemEntity;
import com.tu.backend.externalresource.entity.ResourceTypeEntity;
import com.tu.backend.externalresource.entity.ResourceWorkEntity;
import com.tu.backend.externalresource.util.ExternalUrlNormalizer;
import com.tu.backend.externalresource.repository.ResourceExcerptRepository;
import com.tu.backend.externalresource.repository.ResourceItemRepository;
import com.tu.backend.externalresource.repository.ResourceTypeRepository;
import com.tu.backend.externalresource.repository.ResourceWorkRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.reference.dto.ReferenceCitationDto;
import com.tu.backend.reference.dto.ReferenceItemDto;
import com.tu.backend.reference.dto.ReferenceSourceDto;
import com.tu.backend.reference.dto.ReferenceTargetDto;
import com.tu.backend.reference.dto.UpdateExternalReferenceRequest;
import com.tu.backend.reference.entity.ExternalReferenceOccurrenceEntity;
import com.tu.backend.reference.entity.ExternalResourceReferenceEntity;
import com.tu.backend.reference.entity.InternalReferenceRecordEntity;
import com.tu.backend.reference.repository.ExternalReferenceOccurrenceRepository;
import com.tu.backend.reference.repository.ExternalResourceReferenceRepository;
import com.tu.backend.reference.repository.InternalReferenceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReferenceService {

    private static final String LINK_RESOURCE_TYPE_CODE = "web-link";
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("!?\\[([^\\x5D]*)\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("<img\\b[^>]*?src=\"([^\"]+)\"[^>]*?(?:alt=\"([^\"]*)\")?[^>]*?>", Pattern.CASE_INSENSITIVE);

    private final InternalReferenceRecordRepository internalReferenceRepository;
    private final ExternalReferenceOccurrenceRepository externalReferenceRepository;
    private final ExternalResourceReferenceRepository externalResourceReferenceRepository;
    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final ResourceItemRepository resourceItemRepository;
    private final ResourceExcerptRepository resourceExcerptRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final ResourceWorkRepository resourceWorkRepository;
    private final ObjectMapper objectMapper;

    public ReferenceService(
        InternalReferenceRecordRepository internalReferenceRepository,
        ExternalReferenceOccurrenceRepository externalReferenceRepository,
        ExternalResourceReferenceRepository externalResourceReferenceRepository,
        PageRepository pageRepository,
        PageContentRepository pageContentRepository,
        ResourceItemRepository resourceItemRepository,
        ResourceExcerptRepository resourceExcerptRepository,
        ResourceTypeRepository resourceTypeRepository,
        ResourceWorkRepository resourceWorkRepository,
        ObjectMapper objectMapper
    ) {
        this.internalReferenceRepository = internalReferenceRepository;
        this.externalReferenceRepository = externalReferenceRepository;
        this.externalResourceReferenceRepository = externalResourceReferenceRepository;
        this.pageRepository = pageRepository;
        this.pageContentRepository = pageContentRepository;
        this.resourceItemRepository = resourceItemRepository;
        this.resourceExcerptRepository = resourceExcerptRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.resourceWorkRepository = resourceWorkRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReferenceItemDto> listReferences(
        String category,
        String pageId,
        String resourceItemId,
        String status,
        String q,
        int page,
        int pageSize
    ) {
        String normalizedCategory = normalize(category);
        String normalizedPageId = normalize(pageId);
        String normalizedResourceItemId = normalize(resourceItemId);
        String normalizedStatus = normalize(status);
        String normalizedQuery = normalize(q).toLowerCase(Locale.ROOT);

        int safePage = Math.max(0, page);
        int safePageSize = Math.clamp(pageSize, 1, 200);

        Map<String, PageEntity> pageMap = loadPageMap();
        Map<String, PageContentContext> pageContentMap = loadPageContentContextMap();
        Map<String, ResourceItemEntity> resourceItemMap = loadResourceItemMap();
        Map<String, ResourceExcerptEntity> resourceExcerptMap = loadResourceExcerptMap();
        Map<String, ResourceTypeEntity> resourceTypeMap = loadResourceTypeMap();

        List<ReferenceItemDto> allItems = new ArrayList<>();
        boolean includeInternal = !"external".equals(normalizedCategory) && !"annotation".equals(normalizedCategory);
        boolean includeExternal = !"internal".equals(normalizedCategory) && !"annotation".equals(normalizedCategory);
        boolean includeAnnotation = !"internal".equals(normalizedCategory) && !"external".equals(normalizedCategory);

        if (includeInternal) {
            internalReferenceRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc().forEach(entity -> {
                ReferenceItemDto dto = toInternalDto(entity, pageMap, pageContentMap);
                if (matches(dto, normalizedPageId, normalizedResourceItemId, normalizedStatus, normalizedQuery)) {
                    allItems.add(dto);
                }
            });
        }
        if (includeExternal) {
            externalReferenceRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc().forEach(entity -> {
                ReferenceItemDto dto = toExternalDto(entity, pageMap, pageContentMap, resourceItemMap, resourceTypeMap);
                if (matches(dto, normalizedPageId, normalizedResourceItemId, normalizedStatus, normalizedQuery)) {
                    allItems.add(dto);
                }
            });
            externalResourceReferenceRepository.findAllByOrderByUpdatedAtDescCreatedAtDesc().forEach(entity -> {
                ReferenceItemDto dto = toExternalResourceDto(
                    entity,
                    pageMap,
                    pageContentMap,
                    resourceItemMap,
                    resourceExcerptMap,
                    resourceTypeMap
                );
                if (matches(dto, normalizedPageId, normalizedResourceItemId, normalizedStatus, normalizedQuery)) {
                    allItems.add(dto);
                }
            });
        }
        if (includeAnnotation) {
            for (Map.Entry<String, PageContentContext> entry : pageContentMap.entrySet()) {
                String pid = entry.getKey();
                PageEntity pageEntity = pageMap.get(pid);
                String pageTitle = pageEntity == null ? pid : pageEntity.getTitle();
                for (PageBlockContext block : entry.getValue().blocks()) {
                    collectAnnotationReferences(block, pid, pageTitle, allItems,
                        normalizedPageId, normalizedResourceItemId, normalizedStatus, normalizedQuery);
                }
            }
        }

        allItems.sort(Comparator.comparing(ReferenceItemDto::id).reversed());
        long total = allItems.size();
        int fromIndex = safePage * safePageSize;
        if (fromIndex >= total) {
            return PageResponse.of(List.of(), total, safePage, safePageSize);
        }
        int toIndex = Math.min(fromIndex + safePageSize, (int) total);
        return PageResponse.of(allItems.subList(fromIndex, toIndex), total, safePage, safePageSize);
    }

    @Transactional
    public ReferenceItemDto updateExternalReference(String id, UpdateExternalReferenceRequest request) {
        ExternalReferenceOccurrenceEntity entity = externalReferenceRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "external reference not found"));
        String bindingMode = normalize(request.bindingMode());
        if (!List.of("auto", "manual_bound", "manual_unbound").contains(bindingMode)) {
            throw new BusinessException(40000, "invalid binding mode");
        }

        if ("manual_bound".equals(bindingMode)) {
            String resourceItemId = normalize(request.resourceItemId());
            if (resourceItemId.isBlank()) {
                throw new BusinessException(40000, "resourceItemId is required for manual_bound");
            }
            ensureResourceItemExists(resourceItemId);
            entity.setResourceItemId(resourceItemId);
        } else if ("manual_unbound".equals(bindingMode)) {
            entity.setResourceItemId(null);
        } else {
            entity.setResourceItemId(resolveResourceItemIdByUrl(entity.getUrl()));
        }

        entity.setBindingMode(bindingMode);
        entity.setDisplayText(blankToNull(request.displayText()));
        entity.setCitationLocator(blankToNull(request.citationLocator()));
        entity.setCitationNote(blankToNull(request.citationNote()));
        entity = externalReferenceRepository.save(entity);

        return toExternalDto(
            entity,
            loadPageMap(),
            loadPageContentContextMap(),
            loadResourceItemMap(),
            loadResourceTypeMap()
        );
    }

    @Transactional
    public void rebuildAll() {
        internalReferenceRepository.deleteAllInBatch();
        externalReferenceRepository.deleteAllInBatch();
        externalResourceReferenceRepository.deleteAllInBatch();
        pageContentRepository.findAll().forEach(content -> rebuildPageReferences(content.getPageId(), content.getBlocksJson()));
    }

    @Transactional
    public void rebuildPageReferences(String pageId, String blocksJson) {
        ArrayNode blocks = deserializeBlocksAsArrayNode(blocksJson);
        Map<ExternalReferenceKey, ExternalReferenceOccurrenceEntity> existingExternalMap = new LinkedHashMap<>();
        externalReferenceRepository.findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(List.of(pageId))
            .forEach(entity -> existingExternalMap.put(ExternalReferenceKey.from(entity), entity));
        Map<ExternalResourceReferenceKey, ExternalResourceReferenceEntity> existingResourceMap = new LinkedHashMap<>();
        externalResourceReferenceRepository.findByPageIdInOrderByUpdatedAtDescCreatedAtDesc(List.of(pageId))
            .forEach(entity -> existingResourceMap.put(ExternalResourceReferenceKey.from(entity), entity));

        internalReferenceRepository.deleteByPageId(pageId);
        externalReferenceRepository.deleteByPageId(pageId);
        externalResourceReferenceRepository.deleteByPageId(pageId);

        List<InternalReferenceRecordEntity> internalRecords = new ArrayList<>();
        List<ExternalReferenceOccurrenceEntity> externalRecords = new ArrayList<>();
        List<ExternalResourceReferenceEntity> externalResourceRecords = new ArrayList<>();
        int rootIndex = 0;
        for (JsonNode block : blocks) {
            collectBlockReferences(
                pageId,
                block,
                "blocks[" + rootIndex + "]",
                internalRecords,
                externalRecords,
                externalResourceRecords,
                existingExternalMap,
                existingResourceMap
            );
            rootIndex += 1;
        }

        if (!internalRecords.isEmpty()) {
            internalReferenceRepository.saveAll(internalRecords);
        }
        if (!externalRecords.isEmpty()) {
            externalReferenceRepository.saveAll(externalRecords);
        }
        if (!externalResourceRecords.isEmpty()) {
            externalResourceReferenceRepository.saveAll(externalResourceRecords);
        }
    }

    @Transactional
    public void deleteByPageIds(Collection<String> pageIds) {
        if (pageIds.isEmpty()) {
            return;
        }
        internalReferenceRepository.deleteByPageIdIn(pageIds);
        externalReferenceRepository.deleteByPageIdIn(pageIds);
        externalResourceReferenceRepository.deleteByPageIdIn(pageIds);
    }

    @Transactional(readOnly = true)
    public boolean shouldRunBootstrapRebuild() {
        return internalReferenceRepository.count() == 0
            && externalReferenceRepository.count() == 0
            && externalResourceReferenceRepository.count() == 0
            && pageContentRepository.count() > 0;
    }


    private void collectBlockReferences(
        String pageId,
        JsonNode block,
        String blockPath,
        List<InternalReferenceRecordEntity> internalRecords,
        List<ExternalReferenceOccurrenceEntity> externalRecords,
        List<ExternalResourceReferenceEntity> externalResourceRecords,
        Map<ExternalReferenceKey, ExternalReferenceOccurrenceEntity> existingExternalMap,
        Map<ExternalResourceReferenceKey, ExternalResourceReferenceEntity> existingResourceMap
    ) {
        if (!block.isObject()) {
            return;
        }

        String blockId = text(block.get("id"));
        if (blockId.isBlank()) {
            return;
        }
        String blockType = text(block.get("type"));

        if ("richtext".equalsIgnoreCase(blockType) || "richText".equals(blockType)) {
            JsonNode document = block.get("document");
            if (TiptapDocumentWalker.isDocument(document)) {
                collectTiptapDocumentReferences(
                    pageId,
                    blockId,
                    blockPath,
                    document,
                    internalRecords,
                    externalRecords,
                    externalResourceRecords,
                    existingExternalMap,
                    existingResourceMap
                );
                return;
            }
        }

        if ("ref".equals(blockType)) {
            String refId = text(block.get("refId"));
            String refType = normalize(text(block.get("refType")));
            if (!refId.isBlank()) {
                InternalReferenceRecordEntity entity = new InternalReferenceRecordEntity();
                entity.setId(newId("irr"));
                entity.setPageId(pageId);
                entity.setBlockId(blockId);
                entity.setSourceKind("block_ref");
                entity.setSourceLocator(blockPath);
                entity.setTargetKind("page".equals(refType) ? "page" : "block");
                if ("page".equals(refType)) {
                    entity.setTargetPageId(refId);
                } else {
                    entity.setTargetBlockId(refId);
                    entity.setTargetPageId(findPageIdByBlockId(refId));
                }
                entity.setRefKind("page".equals(refType) ? "page" : "block");
                internalRecords.add(entity);
            }
        }

        extractExternalResourceReference(
            pageId,
            blockId,
            blockType,
            blockPath,
            block,
            externalResourceRecords,
            existingResourceMap
        );
        extractExternalReferences(pageId, blockId, blockType, blockPath, text(block.get("content")), externalRecords, existingExternalMap);
        extractHeadingSourceReferences(
            pageId,
            blockId,
            text(block.get("content")),
            externalResourceRecords,
            existingResourceMap
        );
        extractTableReferences(pageId, blockId, blockType, blockPath, block.get("tableData"), externalRecords, existingExternalMap);
        extractGraphReferences(pageId, blockId, blockType, blockPath, block.get("graphData"), internalRecords, externalRecords, existingExternalMap);

        JsonNode childrenNode = block.get("children");
        if (childrenNode instanceof ArrayNode children) {
            int index = 0;
            for (JsonNode child : children) {
                collectBlockReferences(
                    pageId,
                    child,
                    blockPath + ".children[" + index + "]",
                    internalRecords,
                    externalRecords,
                    externalResourceRecords,
                    existingExternalMap,
                    existingResourceMap
                );
                index += 1;
            }
        }
    }

    private void collectTiptapDocumentReferences(
        String pageId,
        String blockId,
        String blockPath,
        JsonNode document,
        List<InternalReferenceRecordEntity> internalRecords,
        List<ExternalReferenceOccurrenceEntity> externalRecords,
        List<ExternalResourceReferenceEntity> externalResourceRecords,
        Map<ExternalReferenceKey, ExternalReferenceOccurrenceEntity> existingExternalMap,
        Map<ExternalResourceReferenceKey, ExternalResourceReferenceEntity> existingResourceMap
    ) {
        TiptapDocumentWalker.collectReferences(pageId, blockId, blockPath, document, new TiptapDocumentWalker.ReferenceSink() {
            @Override
            public void onRef(String pageId, String embedBlockId, String embedPath, String refId, String refType) {
                String normalizedRefType = normalize(refType);
                if (refId.isBlank()) {
                    return;
                }
                InternalReferenceRecordEntity entity = new InternalReferenceRecordEntity();
                entity.setId(newId("irr"));
                entity.setPageId(pageId);
                entity.setBlockId(embedBlockId);
                entity.setSourceKind("block_ref");
                entity.setSourceLocator(embedPath);
                entity.setTargetKind("page".equals(normalizedRefType) ? "page" : "block");
                if ("page".equals(normalizedRefType)) {
                    entity.setTargetPageId(refId);
                } else {
                    entity.setTargetBlockId(refId);
                    entity.setTargetPageId(findPageIdByBlockId(refId));
                }
                entity.setRefKind("page".equals(normalizedRefType) ? "page" : "block");
                internalRecords.add(entity);
            }

            @Override
            public void onExternalResource(
                String pageId,
                String embedBlockId,
                String embedPath,
                String sourceLocator,
                JsonNode externalResource
            ) {
                var fakeBlock = objectMapper.createObjectNode();
                fakeBlock.set("externalResource", externalResource);
                extractExternalResourceReference(
                    pageId,
                    embedBlockId,
                    "externalResource",
                    embedPath,
                    fakeBlock,
                    externalResourceRecords,
                    existingResourceMap
                );
            }

            @Override
            public void onHeadingSource(
                String pageId,
                String parentBlockId,
                String headingBlockId,
                String resourceItemId,
                String resourceExcerptId
            ) {
                String sourceLocator = "content:heading:" + headingBlockId;
                ExternalResourceReferenceKey key = new ExternalResourceReferenceKey(
                    pageId,
                    parentBlockId,
                    "headingSource",
                    sourceLocator
                );
                ExternalResourceReferenceEntity existing = existingResourceMap.get(key);
                ExternalResourceReferenceEntity entity = new ExternalResourceReferenceEntity();
                entity.setId(existing == null ? newId("err") : existing.getId());
                entity.setPageId(pageId);
                entity.setBlockId(parentBlockId);
                entity.setSourceKind("headingSource");
                entity.setSourceLocator(sourceLocator);
                entity.setResourceItemId(resourceItemId);
                entity.setResourceExcerptId(resourceExcerptId);
                externalResourceRecords.add(entity);
            }

            @Override
            public void onGraphData(
                String pageId,
                String embedBlockId,
                String embedType,
                String embedPath,
                JsonNode graphData
            ) {
                extractGraphReferences(
                    pageId,
                    embedBlockId,
                    embedType,
                    embedPath,
                    graphData,
                    internalRecords,
                    externalRecords,
                    existingExternalMap
                );
            }

            @Override
            public void onTableData(
                String pageId,
                String embedBlockId,
                String embedType,
                String embedPath,
                JsonNode tableData
            ) {
                extractTableReferences(
                    pageId,
                    embedBlockId,
                    embedType,
                    embedPath,
                    tableData,
                    externalRecords,
                    existingExternalMap
                );
            }

            @Override
            public void onExternalUrl(String pageId, String embedBlockId, String embedPath, String url) {
                String normalized = normalizeUrl(url);
                if (normalized == null) {
                    return;
                }
                extractExternalReferences(
                    pageId,
                    embedBlockId,
                    "richtext",
                    embedPath,
                    "[" + normalized + "](" + normalized + ")",
                    externalRecords,
                    existingExternalMap
                );
            }
        });
    }

    private void extractExternalResourceReference(
        String pageId,
        String blockId,
        String blockType,
        String blockPath,
        JsonNode block,
        List<ExternalResourceReferenceEntity> externalResourceRecords,
        Map<ExternalResourceReferenceKey, ExternalResourceReferenceEntity> existingResourceMap
    ) {
        if (!"externalResource".equals(blockType)) {
            return;
        }
        JsonNode data = block.get("externalResource");
        if (data == null || data.isNull()) {
            data = block.path("metadata").path("externalResource");
        }
        if (data == null || data.isMissingNode() || !data.isObject()) {
            return;
        }

        String resourceItemId = text(data.get("resourceItemId"));
        if (resourceItemId.isBlank()) {
            return;
        }
        String sourceLocator = blockPath + ".externalResource";
        ExternalResourceReferenceKey key = new ExternalResourceReferenceKey(pageId, blockId, blockType, sourceLocator);
        ExternalResourceReferenceEntity existing = existingResourceMap.get(key);
        ExternalResourceReferenceEntity entity = new ExternalResourceReferenceEntity();
        entity.setId(existing == null ? newId("err") : existing.getId());
        entity.setPageId(pageId);
        entity.setBlockId(blockId);
        entity.setSourceKind(blockType);
        entity.setSourceLocator(sourceLocator);
        entity.setResourceItemId(resourceItemId);
        entity.setResourceExcerptId(blankToNull(text(data.get("resourceExcerptId"))));
        externalResourceRecords.add(entity);
    }

    private static final Pattern HEADING_SOURCE_PATTERN = Pattern.compile(
        "<!--tu:heading-source\\s+([^>]+)-->",
        Pattern.MULTILINE
    );
    private static final Pattern HEADING_SOURCE_ATTR_PATTERN = Pattern.compile("(\\w[\\w-]*)=\"([^\"]*)\"");

    private void extractHeadingSourceReferences(
        String pageId,
        String blockId,
        String content,
        List<ExternalResourceReferenceEntity> externalResourceRecords,
        Map<ExternalResourceReferenceKey, ExternalResourceReferenceEntity> existingResourceMap
    ) {
        if (content == null || content.isBlank()) {
            return;
        }
        Matcher matcher = HEADING_SOURCE_PATTERN.matcher(content);
        while (matcher.find()) {
            Map<String, String> attrs = parseHeadingSourceAttrs(matcher.group(1));
            String headingBlockId = attrs.get("id");
            String resourceItemId = attrs.get("item");
            String resourceExcerptId = attrs.get("excerpt");
            if (headingBlockId == null || headingBlockId.isBlank()
                || resourceItemId == null || resourceItemId.isBlank()
                || resourceExcerptId == null || resourceExcerptId.isBlank()) {
                continue;
            }
            String sourceLocator = "content:heading:" + headingBlockId;
            ExternalResourceReferenceKey key = new ExternalResourceReferenceKey(
                pageId,
                blockId,
                "headingSource",
                sourceLocator
            );
            ExternalResourceReferenceEntity existing = existingResourceMap.get(key);
            ExternalResourceReferenceEntity entity = new ExternalResourceReferenceEntity();
            entity.setId(existing == null ? newId("err") : existing.getId());
            entity.setPageId(pageId);
            entity.setBlockId(blockId);
            entity.setSourceKind("headingSource");
            entity.setSourceLocator(sourceLocator);
            entity.setResourceItemId(resourceItemId);
            entity.setResourceExcerptId(resourceExcerptId);
            externalResourceRecords.add(entity);
        }
    }

    private Map<String, String> parseHeadingSourceAttrs(String attrsStr) {
        Map<String, String> attrs = new LinkedHashMap<>();
        Matcher matcher = HEADING_SOURCE_ATTR_PATTERN.matcher(attrsStr);
        while (matcher.find()) {
            attrs.put(matcher.group(1), matcher.group(2));
        }
        return attrs;
    }

    private void extractGraphReferences(
        String pageId,
        String blockId,
        String blockType,
        String blockPath,
        JsonNode graphData,
        List<InternalReferenceRecordEntity> internalRecords,
        List<ExternalReferenceOccurrenceEntity> externalRecords,
        Map<ExternalReferenceKey, ExternalReferenceOccurrenceEntity> existingExternalMap
    ) {
        if (graphData == null || !graphData.isObject()) {
            return;
        }
        JsonNode nodes = graphData.get("nodes");
        if (nodes instanceof ArrayNode nodeArray) {
            for (int index = 0; index < nodeArray.size(); index += 1) {
                JsonNode graphNode = nodeArray.get(index);
                JsonNode dataNode = graphNode.get("data");
                String refBlockId = text(dataNode == null ? null : dataNode.get("refBlockId"));
                String refKind = text(dataNode == null ? null : dataNode.get("refKind"));
                if (!refBlockId.isBlank()) {
                    InternalReferenceRecordEntity entity = new InternalReferenceRecordEntity();
                    entity.setId(newId("irr"));
                    entity.setPageId(pageId);
                    entity.setBlockId(blockId);
                    entity.setSourceKind("x6_node");
                    entity.setSourceLocator(blockPath + ".graphData.nodes[" + index + "]");
                    entity.setTargetKind("block");
                    entity.setTargetBlockId(refBlockId);
                    entity.setTargetPageId(findPageIdByBlockId(refBlockId));
                    entity.setRefKind(refKind.isBlank() ? "block" : refKind);
                    internalRecords.add(entity);
                }

                String richContent = text(dataNode == null ? null : dataNode.get("richContent"));
                extractExternalReferences(
                    pageId,
                    blockId,
                    blockType,
                    blockPath + ".graphData.nodes[" + index + "].data.richContent",
                    richContent,
                    externalRecords,
                    existingExternalMap
                );
            }
        }

        JsonNode edges = graphData.get("edges");
        if (edges instanceof ArrayNode edgeArray) {
            for (int index = 0; index < edgeArray.size(); index += 1) {
                JsonNode edgeNode = edgeArray.get(index);
                JsonNode dataNode = edgeNode.get("data");
                String refBlockId = text(dataNode == null ? null : dataNode.get("refBlockId"));
                String refKind = text(dataNode == null ? null : dataNode.get("refKind"));
                if (!refBlockId.isBlank()) {
                    InternalReferenceRecordEntity entity = new InternalReferenceRecordEntity();
                    entity.setId(newId("irr"));
                    entity.setPageId(pageId);
                    entity.setBlockId(blockId);
                    entity.setSourceKind("x6_edge");
                    entity.setSourceLocator(blockPath + ".graphData.edges[" + index + "]");
                    entity.setTargetKind("block");
                    entity.setTargetBlockId(refBlockId);
                    entity.setTargetPageId(findPageIdByBlockId(refBlockId));
                    entity.setRefKind(refKind.isBlank() ? "graph_selection" : refKind);
                    internalRecords.add(entity);
                }
            }
        }
    }

    private void extractTableReferences(
        String pageId,
        String blockId,
        String blockType,
        String blockPath,
        JsonNode tableData,
        List<ExternalReferenceOccurrenceEntity> externalRecords,
        Map<ExternalReferenceKey, ExternalReferenceOccurrenceEntity> existingExternalMap
    ) {
        if (tableData == null || tableData.isNull()) {
            return;
        }
        String textMode = text(tableData.get("textMode"));
        if (!"rich".equals(textMode)) {
            return;
        }
        JsonNode rows = tableData.get("rows");
        if (!(rows instanceof ArrayNode rowArray)) {
            return;
        }
        for (int row = 0; row < rowArray.size(); row += 1) {
            JsonNode rowNode = rowArray.get(row);
            if (!(rowNode instanceof ArrayNode cellArray)) {
                continue;
            }
            for (int column = 0; column < cellArray.size(); column += 1) {
                extractExternalReferences(
                    pageId,
                    blockId,
                    blockType,
                    blockPath + ".tableData.rows[" + row + "][" + column + "]",
                    text(cellArray.get(column)),
                    externalRecords,
                    existingExternalMap
                );
            }
        }
    }

    private void extractExternalReferences(
        String pageId,
        String blockId,
        String blockType,
        String sourceLocator,
        String content,
        List<ExternalReferenceOccurrenceEntity> externalRecords,
        Map<ExternalReferenceKey, ExternalReferenceOccurrenceEntity> existingExternalMap
    ) {
        if (content.isBlank()) {
            return;
        }
        int occurrenceIndex = 0;

        Matcher markdownMatcher = MARKDOWN_LINK_PATTERN.matcher(content);
        while (markdownMatcher.find()) {
            String raw = markdownMatcher.group(0);
            String url = normalizeUrl(markdownMatcher.group(2));
            if (url == null) {
                continue;
            }
            boolean image = raw.startsWith("![");
            String label = blankToNull(markdownMatcher.group(1));
            externalRecords.add(buildExternalReference(
                pageId,
                blockId,
                blockType,
                sourceLocator,
                occurrenceIndex,
                url,
                label,
                image ? "image" : "link",
                existingExternalMap
            ));
            occurrenceIndex += 1;
        }

        Matcher imageMatcher = HTML_IMAGE_PATTERN.matcher(content);
        while (imageMatcher.find()) {
            String url = normalizeUrl(imageMatcher.group(1));
            if (url == null) {
                continue;
            }
            String label = blankToNull(imageMatcher.group(2));
            externalRecords.add(buildExternalReference(
                pageId,
                blockId,
                blockType,
                sourceLocator,
                occurrenceIndex,
                url,
                label,
                "image",
                existingExternalMap
            ));
            occurrenceIndex += 1;
        }
    }

    private ExternalReferenceOccurrenceEntity buildExternalReference(
        String pageId,
        String blockId,
        String blockType,
        String sourceLocator,
        int occurrenceIndex,
        String url,
        String linkText,
        String renderMode,
        Map<ExternalReferenceKey, ExternalReferenceOccurrenceEntity> existingExternalMap
    ) {
        ExternalReferenceKey key = new ExternalReferenceKey(pageId, blockId, blockType, sourceLocator, occurrenceIndex, url);
        ExternalReferenceOccurrenceEntity existing = existingExternalMap.get(key);
        ExternalReferenceOccurrenceEntity entity = new ExternalReferenceOccurrenceEntity();
        entity.setId(existing == null ? newId("ero") : existing.getId());
        entity.setPageId(pageId);
        entity.setBlockId(blockId);
        entity.setSourceKind(blockType);
        entity.setSourceLocator(sourceLocator);
        entity.setOccurrenceIndex(occurrenceIndex);
        entity.setUrl(url);
        entity.setLinkText(linkText);
        entity.setRenderMode(renderMode);

        if (existing != null && "manual_bound".equals(existing.getBindingMode())) {
            entity.setBindingMode(existing.getBindingMode());
            entity.setResourceItemId(existing.getResourceItemId());
        } else if (existing != null && "manual_unbound".equals(existing.getBindingMode())) {
            entity.setBindingMode(existing.getBindingMode());
            entity.setResourceItemId(null);
        } else {
            entity.setBindingMode("auto");
            entity.setResourceItemId(resolveOrCreateResourceItemIdByUrl(url, linkText));
        }

        entity.setDisplayText(existing == null ? null : existing.getDisplayText());
        entity.setCitationLocator(existing == null ? null : existing.getCitationLocator());
        entity.setCitationNote(existing == null ? null : existing.getCitationNote());
        return entity;
    }

    private ReferenceItemDto toInternalDto(
        InternalReferenceRecordEntity entity,
        Map<String, PageEntity> pageMap,
        Map<String, PageContentContext> pageContentMap
    ) {
        PageEntity sourcePage = pageMap.get(entity.getPageId());
        PageEntity targetPage = pageMap.get(entity.getTargetPageId());
        PageBlockContext sourceBlock = pageContentMap.values().stream()
            .flatMap(context -> context.blocks().stream())
            .filter(block -> block.id().equals(entity.getBlockId()))
            .findFirst()
            .orElse(null);
        PageBlockContext targetBlock = pageContentMap.values().stream()
            .flatMap(context -> context.blocks().stream())
            .filter(block -> block.id().equals(entity.getTargetBlockId()))
            .findFirst()
            .orElse(null);
        String status = "ok";
        if ("page".equals(entity.getTargetKind())) {
            if (targetPage == null) {
                status = "broken";
            }
        } else if (targetBlock == null) {
            status = "broken";
        }

        return new ReferenceItemDto(
            entity.getId(),
            "internal",
            false,
            new ReferenceSourceDto(
                entity.getPageId(),
                sourcePage == null ? entity.getPageId() : sourcePage.getTitle(),
                entity.getBlockId(),
                sourceBlock == null ? "" : sourceBlock.type(),
                entity.getSourceKind(),
                entity.getSourceLocator()
            ),
            new ReferenceTargetDto(
                entity.getTargetKind(),
                entity.getTargetPageId(),
                targetPage == null ? "" : targetPage.getTitle(),
                entity.getTargetBlockId(),
                targetBlock == null ? "" : previewBlock(targetBlock.node()),
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            status,
            new ReferenceCitationDto(null, null, null)
        );
    }

    private ReferenceItemDto toExternalDto(
        ExternalReferenceOccurrenceEntity entity,
        Map<String, PageEntity> pageMap,
        Map<String, PageContentContext> pageContentMap,
        Map<String, ResourceItemEntity> resourceItemMap,
        Map<String, ResourceTypeEntity> resourceTypeMap
    ) {
        PageEntity sourcePage = pageMap.get(entity.getPageId());
        PageBlockContext sourceBlock = pageContentMap.values().stream()
            .flatMap(context -> context.blocks().stream())
            .filter(block -> block.id().equals(entity.getBlockId()))
            .findFirst()
            .orElse(null);
        ResourceItemEntity resourceItem = resourceItemMap.get(entity.getResourceItemId());
        ResourceTypeEntity resourceType = resourceItem == null ? null : resourceTypeMap.get(resourceItem.getTypeId());
        String status;
        if ("manual_unbound".equals(entity.getBindingMode()) || entity.getResourceItemId() == null) {
            status = "unbound";
        } else {
            status = resourceItem == null ? "broken" : "bound";
        }

        return new ReferenceItemDto(
            entity.getId(),
            "external",
            true,
            new ReferenceSourceDto(
                entity.getPageId(),
                sourcePage == null ? entity.getPageId() : sourcePage.getTitle(),
                entity.getBlockId(),
                sourceBlock == null ? entity.getSourceKind() : sourceBlock.type(),
                entity.getSourceKind(),
                entity.getSourceLocator()
            ),
            new ReferenceTargetDto(
                "resource",
                null,
                null,
                null,
                null,
                entity.getResourceItemId(),
                resourceItem == null ? null : resourceItem.getTitle(),
                resourceType == null ? null : resourceType.getName(),
                null,
                null,
                null,
                entity.getUrl()
            ),
            status,
            new ReferenceCitationDto(
                entity.getDisplayText(),
                entity.getCitationLocator(),
                entity.getCitationNote()
            )
        );
    }

    private ReferenceItemDto toExternalResourceDto(
        ExternalResourceReferenceEntity entity,
        Map<String, PageEntity> pageMap,
        Map<String, PageContentContext> pageContentMap,
        Map<String, ResourceItemEntity> resourceItemMap,
        Map<String, ResourceExcerptEntity> resourceExcerptMap,
        Map<String, ResourceTypeEntity> resourceTypeMap
    ) {
        PageEntity sourcePage = pageMap.get(entity.getPageId());
        PageBlockContext sourceBlock = pageContentMap.values().stream()
            .flatMap(context -> context.blocks().stream())
            .filter(block -> block.id().equals(entity.getBlockId()))
            .findFirst()
            .orElse(null);
        ResourceItemEntity resourceItem = resourceItemMap.get(entity.getResourceItemId());
        ResourceExcerptEntity excerpt = entity.getResourceExcerptId() == null ? null : resourceExcerptMap.get(entity.getResourceExcerptId());
        ResourceTypeEntity resourceType = resourceItem == null ? null : resourceTypeMap.get(resourceItem.getTypeId());
        JsonNode snapshot = sourceBlock == null ? null : sourceBlock.node().path("externalResource").path("snapshot");

        String resourceTitle = resourceItem == null ? text(snapshot == null ? null : snapshot.get("resourceTitle")) : resourceItem.getTitle();
        String resourceTypeName = resourceType == null ? text(snapshot == null ? null : snapshot.get("resourceTypeName")) : resourceType.getName();
        String excerptTitle = excerpt == null ? text(snapshot == null ? null : snapshot.get("excerptTitle")) : excerpt.getTitle();
        String excerptLocator = excerpt == null ? text(snapshot == null ? null : snapshot.get("excerptLocator")) : excerpt.getLocator();
        String excerptNote = excerpt == null ? text(snapshot == null ? null : snapshot.get("excerptNote")) : excerpt.getNote();
        String url = resourceItem == null ? text(snapshot == null ? null : snapshot.get("sourceUrl")) : resourceItem.getSourceUrl();
        String status = resourceItem == null || (entity.getResourceExcerptId() != null && excerpt == null) ? "broken" : "bound";
        String sourceBlockType = "headingSource".equals(entity.getSourceKind())
            ? "heading"
            : (sourceBlock == null ? entity.getSourceKind() : sourceBlock.type());

        return new ReferenceItemDto(
            entity.getId(),
            "external",
            false,
            new ReferenceSourceDto(
                entity.getPageId(),
                sourcePage == null ? entity.getPageId() : sourcePage.getTitle(),
                entity.getBlockId(),
                sourceBlockType,
                entity.getSourceKind(),
                entity.getSourceLocator()
            ),
            new ReferenceTargetDto(
                entity.getResourceExcerptId() == null ? "resource" : "resource_excerpt",
                null,
                null,
                null,
                null,
                entity.getResourceItemId(),
                blankToNull(resourceTitle),
                blankToNull(resourceTypeName),
                entity.getResourceExcerptId(),
                blankToNull(excerptTitle),
                blankToNull(excerptLocator),
                blankToNull(url)
            ),
            status,
            new ReferenceCitationDto(
                blankToNull(excerptTitle) != null ? excerptTitle : blankToNull(resourceTitle),
                blankToNull(excerptLocator),
                blankToNull(excerptNote)
            )
        );
    }

    private void collectAnnotationReferences(
        PageBlockContext block,
        String pageId,
        String pageTitle,
        List<ReferenceItemDto> items,
        String normalizedPageId,
        String normalizedResourceItemId,
        String normalizedStatus,
        String normalizedQuery
    ) {
        JsonNode metadata = block.node().get("metadata");
        if (metadata == null || !metadata.isObject()) {
            return;
        }
        JsonNode annotations = metadata.get("annotations");
        if (!(annotations instanceof ArrayNode annArray)) {
            return;
        }
        for (JsonNode ann : annArray) {
            ReferenceItemDto dto = toAnnotationDto(ann, pageId, pageTitle, block);
            if (matches(dto, normalizedPageId, normalizedResourceItemId, normalizedStatus, normalizedQuery)) {
                items.add(dto);
            }
        }
    }

    private ReferenceItemDto toAnnotationDto(JsonNode ann, String pageId, String pageTitle, PageBlockContext block) {
        String id = text(ann.get("id"));
        String selectedText = text(ann.get("selectedText"));
        String note = text(ann.get("note"));

        return new ReferenceItemDto(
            id,
            "annotation",
            true,
            new ReferenceSourceDto(
                pageId,
                pageTitle,
                block.id(),
                block.type(),
                "annotation",
                id
            ),
            new ReferenceTargetDto(
                "annotation",
                null,
                null,
                null,
                selectedText,
                null,
                note.isBlank() ? "(无备注)" : note,
                null,
                null,
                null,
                null,
                null
            ),
            "ok",
            new ReferenceCitationDto(
                selectedText,
                null,
                note
            )
        );
    }

    private boolean matches(
        ReferenceItemDto dto,
        String pageId,
        String resourceItemId,
        String status,
        String query
    ) {
        if (!pageId.isBlank() && !pageId.equals(dto.source().pageId())) {
            return false;
        }
        if (!resourceItemId.isBlank() && !resourceItemId.equals(dto.target().resourceItemId())) {
            return false;
        }
        if (!status.isBlank() && !status.equals(dto.status())) {
            return false;
        }
        if (query.isBlank()) {
            return true;
        }
        String joined = String.join(
            " ",
            safe(dto.source().pageTitle()),
            safe(dto.source().blockId()),
            safe(dto.target().pageTitle()),
            safe(dto.target().blockId()),
            safe(dto.target().resourceItemTitle()),
            safe(dto.target().resourceTypeName()),
            safe(dto.target().resourceExcerptTitle()),
            safe(dto.target().resourceExcerptLocator()),
            safe(dto.target().url()),
            safe(dto.citation().displayText()),
            safe(dto.citation().locator()),
            safe(dto.citation().note())
        ).toLowerCase(Locale.ROOT);
        return joined.contains(query);
    }

    private void ensureResourceItemExists(String resourceItemId) {
        if (!resourceItemRepository.existsById(resourceItemId)) {
            throw new BusinessException(40001, "resource item not found");
        }
    }

    private String resolveOrCreateResourceItemIdByUrl(String url, String label) {
        String resolved = resolveResourceItemIdByUrl(url);
        if (resolved != null) {
            return resolved;
        }
        ResourceTypeEntity type = ensureLinkResourceType();
        String title = deriveLinkTitle(label, url);
        ResourceWorkEntity work = new ResourceWorkEntity();
        work.setId("rw-" + UUID.randomUUID().toString().replace("-", ""));
        work.setTypeId(type.getId());
        work.setTitle(title);
        work.setDescription("Auto-created for referenced external link: " + truncate(url, 950));
        work = resourceWorkRepository.save(work);

        ResourceItemEntity item = new ResourceItemEntity();
        item.setId("ri-" + UUID.randomUUID().toString().replace("-", ""));
        item.setTypeId(type.getId());
        item.setWorkId(work.getId());
        item.setTitle(title);
        String baseUrl = ExternalUrlNormalizer.toBasePageUrl(url);
        String identity = baseUrl == null ? url : baseUrl;
        item.setIdentityValue(identity);
        item.setSourceUrl(identity);
        item.setNote("Auto-created from saved external reference");
        return resourceItemRepository.save(item).getId();
    }

    private String resolveResourceItemIdByUrl(String url) {
        ResourceTypeEntity linkType = resourceTypeRepository.findAll().stream()
            .filter(type -> LINK_RESOURCE_TYPE_CODE.equals(type.getCode()))
            .findFirst()
            .orElse(null);
        if (linkType == null) {
            return null;
        }
        String typeId = linkType.getId();
        ExternalUrlNormalizer.ParsedExternalUrl parsed = ExternalUrlNormalizer.parse(url);
        if (parsed != null) {
            return resourceItemRepository.findByTypeIdAndIdentityValue(typeId, parsed.baseUrl())
                .map(ResourceItemEntity::getId)
                .orElseGet(() -> resourceItemRepository.findByTypeIdAndIdentityValue(typeId, parsed.href())
                    .map(ResourceItemEntity::getId)
                    .orElse(null));
        }
        return resourceItemRepository.findByTypeIdAndIdentityValue(typeId, url)
            .map(ResourceItemEntity::getId)
            .orElse(null);
    }

    private ResourceTypeEntity ensureLinkResourceType() {
        return resourceTypeRepository.findAll().stream()
            .filter(type -> LINK_RESOURCE_TYPE_CODE.equals(type.getCode()))
            .findFirst()
            .orElseGet(() -> {
                ResourceTypeEntity entity = new ResourceTypeEntity();
                entity.setId("rt-" + UUID.randomUUID().toString().replace("-", ""));
                entity.setCode(LINK_RESOURCE_TYPE_CODE);
                entity.setName("网络链接");
                entity.setIcon("link");
                entity.setDescription("由引用扫描自动登记的外部网络链接");
                entity.setIdentityFieldKey("sourceUrl");
                entity.setIdentityFieldLabel("源 URL");
                return resourceTypeRepository.save(entity);
            });
    }

    private String deriveLinkTitle(String label, String url) {
        if (label != null && !label.isBlank()) {
            return truncate(label.trim(), 255);
        }
        try {
            return truncate(new URI(url).getHost(), 255);
        } catch (URISyntaxException ex) {
            return truncate(url, 255);
        }
    }

    private Map<String, PageEntity> loadPageMap() {
        Map<String, PageEntity> map = new HashMap<>();
        pageRepository.findAll().forEach(page -> map.put(page.getId(), page));
        return map;
    }

    private Map<String, PageContentContext> loadPageContentContextMap() {
        Map<String, PageContentContext> map = new HashMap<>();
        pageContentRepository.findAll().forEach(content ->
            map.put(content.getPageId(), new PageContentContext(content.getPageId(), flattenBlocks(deserializeBlocksAsArrayNode(content.getBlocksJson()))))
        );
        return map;
    }

    private List<PageBlockContext> flattenBlocks(ArrayNode blocks) {
        List<PageBlockContext> result = new ArrayList<>();
        for (JsonNode block : blocks) {
            flattenBlockRecursive(block, result);
        }
        return result;
    }

    private void flattenBlockRecursive(JsonNode block, List<PageBlockContext> result) {
        if (!block.isObject()) {
            return;
        }
        String id = text(block.get("id"));
        if (!id.isBlank()) {
            result.add(new PageBlockContext(id, text(block.get("type")), block));
        }
        JsonNode children = block.get("children");
        if (children instanceof ArrayNode childArray) {
            for (JsonNode child : childArray) {
                flattenBlockRecursive(child, result);
            }
        }
    }

    private Map<String, ResourceItemEntity> loadResourceItemMap() {
        Map<String, ResourceItemEntity> map = new HashMap<>();
        resourceItemRepository.findAll().forEach(item -> map.put(item.getId(), item));
        return map;
    }

    private Map<String, ResourceExcerptEntity> loadResourceExcerptMap() {
        Map<String, ResourceExcerptEntity> map = new HashMap<>();
        resourceExcerptRepository.findAll().forEach(excerpt -> map.put(excerpt.getId(), excerpt));
        return map;
    }

    private Map<String, ResourceTypeEntity> loadResourceTypeMap() {
        Map<String, ResourceTypeEntity> map = new HashMap<>();
        resourceTypeRepository.findAll().forEach(type -> map.put(type.getId(), type));
        return map;
    }

    private String findPageIdByBlockId(String blockId) {
        return pageContentRepository.findAll().stream()
            .filter(content -> containsBlockId(deserializeBlocksAsArrayNode(content.getBlocksJson()), blockId))
            .map(PageContentEntity::getPageId)
            .findFirst()
            .orElse(null);
    }

    private boolean containsBlockId(ArrayNode blocks, String blockId) {
        for (JsonNode block : blocks) {
            if (containsBlockIdRecursive(block, blockId)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBlockIdRecursive(JsonNode block, String blockId) {
        if (!block.isObject()) {
            return false;
        }
        if (blockId.equals(text(block.get("id")))) {
            return true;
        }
        JsonNode children = block.get("children");
        if (children instanceof ArrayNode childArray) {
            for (JsonNode child : childArray) {
                if (containsBlockIdRecursive(child, blockId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String previewBlock(JsonNode block) {
        String content = text(block.get("content"));
        if (!content.isBlank()) {
            return truncate(content.replaceAll("\\s+", " "), 120);
        }
        String title = text(block.get("title"));
        if (!title.isBlank()) {
            return truncate(title, 120);
        }
        return text(block.get("type"));
    }

    private ArrayNode deserializeBlocksAsArrayNode(String blocksJson) {
        try {
            JsonNode node = objectMapper.readTree(blocksJson);
            if (node instanceof ArrayNode arrayNode) {
                return arrayNode;
            }
            throw new BusinessException(50000, "invalid page content structure");
        } catch (Exception ex) {
            throw ex instanceof BusinessException businessException
                ? businessException
                : new BusinessException(50000, "failed to deserialize page content");
        }
    }

    private String normalizeUrl(String url) {
        String value = normalize(url);
        if (value.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(value);
            if (!Objects.equals(uri.getScheme(), "http") && !Objects.equals(uri.getScheme(), "https")) {
                return null;
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("");
    }

    private String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record PageBlockContext(String id, String type, JsonNode node) {
    }

    private record PageContentContext(String pageId, List<PageBlockContext> blocks) {
    }

    private record ExternalReferenceKey(
        String pageId,
        String blockId,
        String sourceKind,
        String sourceLocator,
        int occurrenceIndex,
        String url
    ) {
        static ExternalReferenceKey from(ExternalReferenceOccurrenceEntity entity) {
            return new ExternalReferenceKey(
                entity.getPageId(),
                entity.getBlockId(),
                entity.getSourceKind(),
                entity.getSourceLocator(),
                entity.getOccurrenceIndex(),
                entity.getUrl()
            );
        }
    }

    private record ExternalResourceReferenceKey(
        String pageId,
        String blockId,
        String sourceKind,
        String sourceLocator
    ) {
        static ExternalResourceReferenceKey from(ExternalResourceReferenceEntity entity) {
            return new ExternalResourceReferenceKey(
                entity.getPageId(),
                entity.getBlockId(),
                entity.getSourceKind(),
                entity.getSourceLocator()
            );
        }
    }
}
