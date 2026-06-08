package com.tu.backend.block.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tu.backend.block.dto.BlockMetaDto;
import com.tu.backend.block.dto.SyncBlocksRequest;
import com.tu.backend.block.dto.UpdateBlockRequest;
import com.tu.backend.block.dto.UpdateBlockContentRequest;
import com.tu.backend.block.dto.UpdateBlockGraphRequest;
import com.tu.backend.common.BusinessException;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import com.tu.backend.reference.service.ReferenceService;
import com.tu.backend.index.PageIndexCoordinator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BlockService {

    private final PageContentRepository pageContentRepository;
    private final PageRepository pageRepository;
    private final ObjectMapper objectMapper;
    private final PageIndexCoordinator pageIndexCoordinator;
    private final ReferenceService referenceService;

    public BlockService(
        PageContentRepository pageContentRepository,
        PageRepository pageRepository,
        ObjectMapper objectMapper,
        PageIndexCoordinator pageIndexCoordinator,
        ReferenceService referenceService
    ) {
        this.pageContentRepository = pageContentRepository;
        this.pageRepository = pageRepository;
        this.objectMapper = objectMapper;
        this.pageIndexCoordinator = pageIndexCoordinator;
        this.referenceService = referenceService;
    }

    @Transactional(readOnly = true)
    public List<BlockMetaDto> listBlocks() {
        List<PageEntity> pages = pageRepository.findAll();
        List<BlockMetaDto> result = new ArrayList<>();

        for (PageEntity page : pages) {
            pageContentRepository.findById(page.getId()).ifPresent(content -> {
                List<JsonNode> blocks = deserializeBlocks(content.getBlocksJson());
                for (JsonNode block : blocks) {
                    collectBlocks(block, page.getId(), page.getTitle(), result);
                }
            });
        }

        return result;
    }

    @Transactional
    public void updateBlockContent(String blockId, UpdateBlockContentRequest request) {
        PageContentEntity contentEntity = getPageContentOrThrow(request.pageId());
        ArrayNode blocks = deserializeBlocksAsArrayNode(contentEntity.getBlocksJson());
        ObjectNode block = findBlock(blocks, blockId);
        if (block == null) {
            throw new BusinessException(40001, "block not found");
        }
        block.put("content", request.content() == null ? "" : request.content());
        contentEntity.setBlocksJson(serializeBlocks(blocks));
        pageContentRepository.save(contentEntity);
        referenceService.rebuildPageReferences(request.pageId(), contentEntity.getBlocksJson());
        pageIndexCoordinator.onPageContentChanged(request.pageId());
    }

    @Transactional
    public void updateBlockGraph(String blockId, UpdateBlockGraphRequest request) {
        PageContentEntity contentEntity = getPageContentOrThrow(request.pageId());
        ArrayNode blocks = deserializeBlocksAsArrayNode(contentEntity.getBlocksJson());
        ObjectNode block = findBlock(blocks, blockId);
        if (block == null) {
            throw new BusinessException(40001, "block not found");
        }
        block.set("graphData", objectMapper.valueToTree(request.graphData()));
        contentEntity.setBlocksJson(serializeBlocks(blocks));
        pageContentRepository.save(contentEntity);
        referenceService.rebuildPageReferences(request.pageId(), contentEntity.getBlocksJson());
        pageIndexCoordinator.onPageContentChanged(request.pageId());
    }

    @Transactional
    public void updateBlock(String blockId, UpdateBlockRequest request) {
        PageContentEntity contentEntity = getPageContentOrThrow(request.pageId());
        ArrayNode blocks = deserializeBlocksAsArrayNode(contentEntity.getBlocksJson());
        ObjectNode replacement = objectMapper.valueToTree(request.block());
        replacement.put("id", blockId);
        if (!replaceBlock(blocks, blockId, replacement)) {
            throw new BusinessException(40001, "block not found");
        }
        contentEntity.setBlocksJson(serializeBlocks(blocks));
        pageContentRepository.save(contentEntity);
        referenceService.rebuildPageReferences(request.pageId(), contentEntity.getBlocksJson());
        pageIndexCoordinator.onPageContentChanged(request.pageId());
    }

    @Transactional
    public void deleteAnnotation(String pageId, String blockId, String annotationId) {
        PageContentEntity contentEntity = getPageContentOrThrow(pageId);
        ArrayNode blocks = deserializeBlocksAsArrayNode(contentEntity.getBlocksJson());
        ObjectNode block = findBlock(blocks, blockId);
        if (block == null) {
            throw new BusinessException(40001, "block not found");
        }
        JsonNode metadata = block.get("metadata");
        if (metadata instanceof ObjectNode metaNode) {
            JsonNode annotations = metaNode.get("annotations");
            if (annotations instanceof ArrayNode annArray) {
                for (int i = 0; i < annArray.size(); i += 1) {
                    JsonNode ann = annArray.get(i);
                    JsonNode idNode = ann.get("id");
                    String id = idNode == null || idNode.isNull() ? "" : idNode.asText("");
                    if (annotationId.equals(id)) {
                        annArray.remove(i);
                        break;
                    }
                }
                metaNode.set("annotations", annArray);
                block.set("metadata", metaNode);
            }
        }
        contentEntity.setBlocksJson(serializeBlocks(blocks));
        pageContentRepository.save(contentEntity);
        referenceService.rebuildPageReferences(pageId, contentEntity.getBlocksJson());
        pageIndexCoordinator.onPageContentChanged(pageId);
    }

    @Transactional
    public void syncBlocks(SyncBlocksRequest request) {
        PageEntity page = pageRepository.findById(request.pageId())
            .orElseThrow(() -> new BusinessException(40001, "page not found"));

        PageContentEntity entity = pageContentRepository.findById(page.getId())
            .orElseGet(() -> {
                PageContentEntity created = new PageContentEntity();
                created.setPageId(page.getId());
                return created;
        });
        entity.setBlocksJson(serializeBlocks(request.blocks()));
        pageContentRepository.save(entity);
        referenceService.rebuildPageReferences(page.getId(), entity.getBlocksJson());
        pageIndexCoordinator.onPageContentChanged(page.getId());
    }

    private PageContentEntity getPageContentOrThrow(String pageId) {
        pageRepository.findById(pageId)
            .orElseThrow(() -> new BusinessException(40001, "page not found"));
        return pageContentRepository.findById(pageId)
            .orElseThrow(() -> new BusinessException(40001, "page content not found"));
    }

    private void collectBlocks(JsonNode block, String pageId, String pageTitle, List<BlockMetaDto> result) {
        if (!block.isObject()) {
            return;
        }

        JsonNode typeNode = block.get("type");
        String type = typeNode != null && !typeNode.isNull() ? typeNode.asText() : null;
        if (!"ref".equals(type) && !"spacer".equals(type)) {
            result.add(new BlockMetaDto(objectMapper.convertValue(block.deepCopy(), Object.class), pageId, pageTitle));
        }

        JsonNode childrenNode = block.get("children");
        if (childrenNode instanceof ArrayNode children) {
            for (JsonNode child : children) {
                collectBlocks(child, pageId, pageTitle, result);
            }
        }
    }

    private ObjectNode findBlock(ArrayNode blocks, String blockId) {
        for (JsonNode block : blocks) {
            ObjectNode found = findBlockRecursive(block, blockId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private ObjectNode findBlockRecursive(JsonNode node, String blockId) {
        if (!(node instanceof ObjectNode)) {
            return null;
        }
        ObjectNode objectNode = (ObjectNode) node;
        JsonNode idNode = objectNode.get("id");
        if (idNode != null && blockId.equals(idNode.asText())) {
            return objectNode;
        }

        JsonNode childrenNode = objectNode.get("children");
        if (childrenNode instanceof ArrayNode children) {
            for (JsonNode child : children) {
                ObjectNode found = findBlockRecursive(child, blockId);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean replaceBlock(ArrayNode blocks, String blockId, ObjectNode replacement) {
        for (int i = 0; i < blocks.size(); i += 1) {
            JsonNode block = blocks.get(i);
            if (block instanceof ObjectNode objectNode) {
                JsonNode idNode = objectNode.get("id");
                if (idNode != null && blockId.equals(idNode.asText())) {
                    blocks.set(i, replacement);
                    return true;
                }

                JsonNode childrenNode = objectNode.get("children");
                if (childrenNode instanceof ArrayNode children && replaceBlock(children, blockId, replacement)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<JsonNode> deserializeBlocks(String blocksJson) {
        ArrayNode arrayNode = deserializeBlocksAsArrayNode(blocksJson);
        List<JsonNode> result = new ArrayList<>();
        arrayNode.forEach(result::add);
        return result;
    }

    private ArrayNode deserializeBlocksAsArrayNode(String blocksJson) {
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

    private String serializeBlocks(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(40000, "invalid blocks payload");
        }
    }
}
