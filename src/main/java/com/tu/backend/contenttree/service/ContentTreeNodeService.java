package com.tu.backend.contenttree.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tu.backend.common.BusinessException;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.contenttree.ContentTreeHoursRollup;
import com.tu.backend.contenttree.OutlineExtractor;
import com.tu.backend.contenttree.dto.BlockOutlineResponseDto;
import com.tu.backend.contenttree.dto.ContentTreeNodeDto;
import com.tu.backend.contenttree.dto.OutlineBatchRequestDto;
import com.tu.backend.contenttree.dto.OutlineBatchResponseDto;
import com.tu.backend.contenttree.dto.PageOutlineResponseDto;
import com.tu.backend.contenttree.dto.UpdateContentTreeNodeRequestDto;
import com.tu.backend.contenttree.entity.ContentTreeNodeEntity;
import com.tu.backend.contenttree.entity.ContentTreeScopeEntity;
import com.tu.backend.contenttree.entity.ContentTreeScopeId;
import com.tu.backend.contenttree.entity.ScopeType;
import com.tu.backend.contenttree.repository.ContentTreeNodeRepository;
import com.tu.backend.contenttree.repository.ContentTreeScopeRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ContentTreeNodeService {

    private final ContentTreeNodeRepository nodeRepository;
    private final ContentTreeScopeRepository scopeRepository;
    private final PageRepository pageRepository;
    private final PageContentRepository pageContentRepository;
    private final ObjectMapper objectMapper;

    public ContentTreeNodeService(
        ContentTreeNodeRepository nodeRepository,
        ContentTreeScopeRepository scopeRepository,
        PageRepository pageRepository,
        PageContentRepository pageContentRepository,
        ObjectMapper objectMapper
    ) {
        this.nodeRepository = nodeRepository;
        this.scopeRepository = scopeRepository;
        this.pageRepository = pageRepository;
        this.pageContentRepository = pageContentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageOutlineResponseDto getPageOutline(String pageId) {
        PageEntity page = findPage(pageId);
        List<ContentTreeNodeEntity> nodes = nodeRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscCreatedAtAsc(
            ScopeType.PAGE,
            pageId
        );
        if (nodes.isEmpty()) {
            nodes = extractEphemeralPageOutline(pageId);
        }
        return new PageOutlineResponseDto(
            pageId,
            page.getKbId(),
            page.getTitle(),
            ContentTreeHoursRollup.withRollup(nodes)
        );
    }

    @Transactional(readOnly = true)
    public BlockOutlineResponseDto getBlockOutline(String blockId) {
        BlockLocation location = findBlockLocation(blockId);
        List<ContentTreeNodeEntity> pageNodes = nodeRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscCreatedAtAsc(
            ScopeType.PAGE,
            location.pageId()
        );
        Set<String> subtreeIds = collectSubtreeIds(pageNodes, blockId);
        List<ContentTreeNodeEntity> filtered = pageNodes.stream()
            .filter(node -> subtreeIds.contains(node.getId())
                || blockId.equals(node.getSourceBlockId())
                || isDescendantOfSourceBlock(pageNodes, node, blockId))
            .toList();
        if (filtered.isEmpty()) {
            filtered = extractEphemeralBlockOutline(location.pageId(), location.block());
        }
        return new BlockOutlineResponseDto(
            blockId,
            location.pageId(),
            ContentTreeHoursRollup.withRollup(filtered)
        );
    }

    @Transactional(readOnly = true)
    public OutlineBatchResponseDto batchOutline(OutlineBatchRequestDto request) {
        List<String> pageIds = request.pageIds() == null ? List.of() : request.pageIds().stream().distinct().toList();
        List<String> blockIds = request.blockIds() == null ? List.of() : request.blockIds().stream().distinct().toList();

        List<PageOutlineResponseDto> pages = pageIds.stream()
            .map(this::getPageOutline)
            .toList();

        List<BlockOutlineResponseDto> blocks = blockIds.stream()
            .map(this::getBlockOutline)
            .toList();

        return new OutlineBatchResponseDto(pages, blocks);
    }

    @Transactional
    public ContentTreeNodeDto updateEstimatedHours(String nodeId, UpdateContentTreeNodeRequestDto request) {
        ContentTreeNodeEntity entity = findNode(nodeId);
        entity.setEstimatedHours(request.estimatedHours());
        ContentTreeNodeEntity saved = nodeRepository.save(entity);
        List<ContentTreeNodeEntity> siblings = nodeRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscCreatedAtAsc(
            saved.getScopeType(),
            saved.getScopeId()
        );
        return ContentTreeHoursRollup.withRollup(siblings).stream()
            .filter(node -> node.id().equals(saved.getId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(50000, "failed to compute node rollup"));
    }

    @Transactional
    public void rebuildPageOutline(String pageId, String fingerprint) {
        PageEntity page = findPage(pageId);
        PageContentEntity content = pageContentRepository.findById(pageId).orElse(null);
        ArrayNode blocks = deserializeBlocks(content == null ? "[]" : content.getBlocksJson());

        List<OutlineExtractor.ExtractedOutlineNode> extracted = OutlineExtractor.extractPageOutline(pageId, blocks);
        List<ContentTreeNodeEntity> existing = nodeRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscCreatedAtAsc(
            ScopeType.PAGE,
            pageId
        );
        Map<String, ContentTreeNodeEntity> existingById = new HashMap<>();
        for (ContentTreeNodeEntity node : existing) {
            existingById.put(node.getId(), node);
        }

        Set<String> newIds = new HashSet<>();
        List<ContentTreeNodeEntity> toSave = new ArrayList<>();
        for (OutlineExtractor.ExtractedOutlineNode extractedNode : extracted) {
            newIds.add(extractedNode.id());
            ContentTreeNodeEntity entity = existingById.get(extractedNode.id());
            if (entity == null) {
                entity = new ContentTreeNodeEntity();
                entity.setId(extractedNode.id());
                entity.setScopeType(ScopeType.PAGE);
                entity.setScopeId(pageId);
            }
            entity.setParentId(extractedNode.parentId());
            entity.setTitle(extractedNode.title());
            entity.setSortOrder(extractedNode.sortOrder());
            entity.setSourceBlockId(extractedNode.sourceBlockId());
            entity.setOutlineLevel(extractedNode.level());
            entity.setSourceType(extractedNode.sourceType());
            entity.setPreviewText(extractedNode.previewText());
            entity.setBlockType(extractedNode.blockType());
            toSave.add(entity);
        }

        for (ContentTreeNodeEntity orphan : existing) {
            if (!newIds.contains(orphan.getId())) {
                nodeRepository.delete(orphan);
            }
        }
        nodeRepository.saveAll(toSave);

        ContentTreeScopeEntity scope = scopeRepository.findById(new ContentTreeScopeId(ScopeType.PAGE, pageId))
            .orElseGet(() -> {
                ContentTreeScopeEntity created = new ContentTreeScopeEntity();
                created.setId(new ContentTreeScopeId(ScopeType.PAGE, pageId));
                return created;
            });
        scope.setKbId(page.getKbId());
        scope.setContentFingerprint(fingerprint);
        scopeRepository.save(scope);
    }

    @Transactional
    public void deletePageOutline(String pageId) {
        nodeRepository.deleteByScopeTypeAndScopeId(ScopeType.PAGE, pageId);
        scopeRepository.deleteById(new ContentTreeScopeId(ScopeType.PAGE, pageId));
    }

    @Transactional(readOnly = true)
    public String getStoredFingerprint(String pageId) {
        return scopeRepository.findById(new ContentTreeScopeId(ScopeType.PAGE, pageId))
            .map(ContentTreeScopeEntity::getContentFingerprint)
            .orElse(null);
    }

    @Transactional
    public void deletePageOutlines(List<String> pageIds) {
        if (pageIds == null || pageIds.isEmpty()) {
            return;
        }
        nodeRepository.deleteByScopeTypeAndScopeIdIn(ScopeType.PAGE, pageIds);
        for (String pageId : pageIds) {
            scopeRepository.deleteById(new ContentTreeScopeId(ScopeType.PAGE, pageId));
        }
    }

    @Transactional(readOnly = true)
    public List<ContentTreeNodeEntity> listResourceItemNodes(String resourceItemId) {
        return nodeRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscCreatedAtAsc(
            ScopeType.RESOURCE_ITEM,
            resourceItemId
        );
    }

    @Transactional(readOnly = true)
    public ContentTreeNodeEntity findResourceNode(String nodeId) {
        ContentTreeNodeEntity node = findNode(nodeId);
        if (!ScopeType.RESOURCE_ITEM.equals(node.getScopeType())) {
            throw new BusinessException(40001, "content tree node not found");
        }
        return node;
    }

    @Transactional
    public ContentTreeNodeEntity saveResourceNode(ContentTreeNodeEntity entity) {
        entity.setScopeType(ScopeType.RESOURCE_ITEM);
        return nodeRepository.save(entity);
    }

    @Transactional
    public void deleteResourceNodes(Set<String> nodeIds) {
        nodeRepository.deleteAllById(nodeIds);
    }

    @Transactional
    public void deleteResourceScope(String resourceItemId) {
        nodeRepository.deleteByScopeTypeAndScopeId(ScopeType.RESOURCE_ITEM, resourceItemId);
        scopeRepository.deleteById(new ContentTreeScopeId(ScopeType.RESOURCE_ITEM, resourceItemId));
    }

    private List<ContentTreeNodeEntity> extractEphemeralPageOutline(String pageId) {
        PageContentEntity content = pageContentRepository.findById(pageId).orElse(null);
        ArrayNode blocks = deserializeBlocks(content == null ? "[]" : content.getBlocksJson());
        List<OutlineExtractor.ExtractedOutlineNode> extracted = OutlineExtractor.extractPageOutline(pageId, blocks);
        List<ContentTreeNodeEntity> entities = new ArrayList<>();
        for (OutlineExtractor.ExtractedOutlineNode node : extracted) {
            ContentTreeNodeEntity entity = new ContentTreeNodeEntity();
            entity.setId(node.id());
            entity.setScopeType(ScopeType.PAGE);
            entity.setScopeId(pageId);
            entity.setParentId(node.parentId());
            entity.setTitle(node.title());
            entity.setSortOrder(node.sortOrder());
            entity.setSourceBlockId(node.sourceBlockId());
            entity.setOutlineLevel(node.level());
            entity.setSourceType(node.sourceType());
            entity.setPreviewText(node.previewText());
            entity.setBlockType(node.blockType());
            entities.add(entity);
        }
        return entities;
    }

    private List<ContentTreeNodeEntity> extractEphemeralBlockOutline(String pageId, com.fasterxml.jackson.databind.JsonNode block) {
        List<OutlineExtractor.ExtractedOutlineNode> extracted = OutlineExtractor.extractBlockOutline(pageId, block);
        List<ContentTreeNodeEntity> entities = new ArrayList<>();
        for (OutlineExtractor.ExtractedOutlineNode node : extracted) {
            ContentTreeNodeEntity entity = new ContentTreeNodeEntity();
            entity.setId(node.id());
            entity.setScopeType(ScopeType.PAGE);
            entity.setScopeId(pageId);
            entity.setParentId(node.parentId());
            entity.setTitle(node.title());
            entity.setSortOrder(node.sortOrder());
            entity.setSourceBlockId(node.sourceBlockId());
            entity.setOutlineLevel(node.level());
            entity.setSourceType(node.sourceType());
            entity.setPreviewText(node.previewText());
            entity.setBlockType(node.blockType());
            entities.add(entity);
        }
        return entities;
    }

    private boolean isDescendantOfSourceBlock(
        List<ContentTreeNodeEntity> nodes,
        ContentTreeNodeEntity node,
        String blockId
    ) {
        String currentParentId = node.getParentId();
        Map<String, ContentTreeNodeEntity> byId = new HashMap<>();
        for (ContentTreeNodeEntity item : nodes) {
            byId.put(item.getId(), item);
        }
        while (currentParentId != null) {
            ContentTreeNodeEntity parent = byId.get(currentParentId);
            if (parent == null) {
                return false;
            }
            if (blockId.equals(parent.getSourceBlockId())) {
                return true;
            }
            currentParentId = parent.getParentId();
        }
        return false;
    }

    private Set<String> collectSubtreeIds(List<ContentTreeNodeEntity> nodes, String rootSourceBlockId) {
        Set<String> rootNodeIds = new HashSet<>();
        for (ContentTreeNodeEntity node : nodes) {
            if (rootSourceBlockId.equals(node.getSourceBlockId())) {
                rootNodeIds.add(node.getId());
            }
        }
        Map<String, List<ContentTreeNodeEntity>> childrenByParent = new HashMap<>();
        for (ContentTreeNodeEntity node : nodes) {
            String parentKey = node.getParentId() == null ? "" : node.getParentId();
            childrenByParent.computeIfAbsent(parentKey, key -> new ArrayList<>()).add(node);
        }
        Set<String> result = new HashSet<>();
        for (String rootId : rootNodeIds) {
            collectDescendantNodeIds(rootId, childrenByParent, result);
        }
        return result;
    }

    private void collectDescendantNodeIds(
        String nodeId,
        Map<String, List<ContentTreeNodeEntity>> childrenByParent,
        Set<String> result
    ) {
        result.add(nodeId);
        for (ContentTreeNodeEntity child : childrenByParent.getOrDefault(nodeId, List.of())) {
            collectDescendantNodeIds(child.getId(), childrenByParent, result);
        }
    }

    private BlockLocation findBlockLocation(String blockId) {
        for (PageContentEntity content : pageContentRepository.findAll()) {
            ArrayNode blocks = deserializeBlocks(content.getBlocksJson());
            com.fasterxml.jackson.databind.JsonNode block = findBlockRecursive(blocks, blockId);
            if (block != null) {
                return new BlockLocation(content.getPageId(), block);
            }
        }
        throw new BusinessException(40001, "block not found");
    }

    private com.fasterxml.jackson.databind.JsonNode findBlockRecursive(ArrayNode blocks, String blockId) {
        for (com.fasterxml.jackson.databind.JsonNode block : blocks) {
            com.fasterxml.jackson.databind.JsonNode found = findBlockInNode(block, blockId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private com.fasterxml.jackson.databind.JsonNode findBlockInNode(com.fasterxml.jackson.databind.JsonNode node, String blockId) {
        if (!node.isObject()) {
            return null;
        }
        com.fasterxml.jackson.databind.JsonNode id = node.get("id");
        if (id != null && blockId.equals(id.asText())) {
            return node;
        }
        com.fasterxml.jackson.databind.JsonNode children = node.get("children");
        if (children instanceof ArrayNode childArray) {
            for (com.fasterxml.jackson.databind.JsonNode child : childArray) {
                com.fasterxml.jackson.databind.JsonNode found = findBlockInNode(child, blockId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private ArrayNode deserializeBlocks(String blocksJson) {
        try {
            if (blocksJson == null || blocksJson.isBlank()) {
                return objectMapper.createArrayNode();
            }
            return (ArrayNode) objectMapper.readTree(blocksJson);
        } catch (Exception ex) {
            return objectMapper.createArrayNode();
        }
    }

    private PageEntity findPage(String pageId) {
        if (pageId == null || pageId.isBlank()) {
            throw new BusinessException(40000, "page id required");
        }
        return pageRepository.findById(pageId.trim())
            .orElseThrow(() -> new BusinessException(40001, "page not found"));
    }

    private ContentTreeNodeEntity findNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new BusinessException(40000, "content tree node id required");
        }
        return nodeRepository.findById(nodeId.trim())
            .orElseThrow(() -> new BusinessException(40001, "content tree node not found"));
    }

    private record BlockLocation(String pageId, com.fasterxml.jackson.databind.JsonNode block) {
    }
}
