package com.tu.backend.block.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tu.backend.block.dto.BlockMetaDto;
import com.tu.backend.block.dto.SyncBlocksRequest;
import com.tu.backend.block.dto.UpdateBlockContentRequest;
import com.tu.backend.block.dto.UpdateBlockGraphRequest;
import com.tu.backend.common.BusinessException;
import com.tu.backend.content.entity.PageContentEntity;
import com.tu.backend.content.repository.PageContentRepository;
import com.tu.backend.page.entity.PageEntity;
import com.tu.backend.page.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BlockService {

    private final PageContentRepository pageContentRepository;
    private final PageRepository pageRepository;
    private final ObjectMapper objectMapper;

    public BlockService(
        PageContentRepository pageContentRepository,
        PageRepository pageRepository,
        ObjectMapper objectMapper
    ) {
        this.pageContentRepository = pageContentRepository;
        this.pageRepository = pageRepository;
        this.objectMapper = objectMapper;
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
    }

    @Transactional
    public void updateBlockGraph(String blockId, UpdateBlockGraphRequest request) {
        PageContentEntity contentEntity = getPageContentOrThrow(request.pageId());
        ArrayNode blocks = deserializeBlocksAsArrayNode(contentEntity.getBlocksJson());
        ObjectNode block = findBlock(blocks, blockId);
        if (block == null) {
            throw new BusinessException(40001, "block not found");
        }
        block.set("graphData", request.graphData());
        contentEntity.setBlocksJson(serializeBlocks(blocks));
        pageContentRepository.save(contentEntity);
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
        if (!(node instanceof ObjectNode objectNode)) {
            return null;
        }

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

    private List<JsonNode> deserializeBlocks(String blocksJson) {
        try {
            ArrayNode arrayNode = deserializeBlocksAsArrayNode(blocksJson);
            List<JsonNode> result = new ArrayList<>();
            arrayNode.forEach(result::add);
            return result;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50000, "failed to deserialize page content");
        }
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
