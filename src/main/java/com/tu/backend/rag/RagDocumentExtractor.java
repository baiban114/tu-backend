package com.tu.backend.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tu.backend.content.tiptap.TiptapDocumentWalker;
import com.tu.backend.rag.dto.RagIndexDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RagDocumentExtractor {

    private final ObjectMapper objectMapper;

    public RagDocumentExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<RagIndexDocument> extract(
        String kbId,
        String pageId,
        String pageTitle,
        ArrayNode blocks,
        LocalDateTime updatedAt
    ) {
        List<RagIndexDocument> documents = new ArrayList<>();
        for (JsonNode block : blocks) {
            collectBlock(kbId, pageId, pageTitle, block, updatedAt, documents);
        }
        return documents;
    }

    private void collectBlock(
        String kbId,
        String pageId,
        String pageTitle,
        JsonNode block,
        LocalDateTime updatedAt,
        List<RagIndexDocument> documents
    ) {
        if (!block.isObject()) {
            return;
        }

        String type = textValue(block, "type");
        String blockId = textValue(block, "id");
        String title = firstNonBlank(textValue(block, "title"), pageTitle);
        String content = extractContent(block, type, title);

        if (blockId != null && !content.isBlank() && isIndexableType(type)) {
            documents.add(new RagIndexDocument(
                kbId,
                pageId,
                blockId,
                title,
                content,
                type,
                updatedAt == null ? null : updatedAt.toString(),
                metadata(block)
            ));
        }

        JsonNode children = block.get("children");
        if (children instanceof ArrayNode arrayNode) {
            for (JsonNode child : arrayNode) {
                collectBlock(kbId, pageId, pageTitle, child, updatedAt, documents);
            }
        }
    }

    private boolean isIndexableType(String type) {
        return "richtext".equals(type)
            || "richText".equals(type)
            || "table".equals(type)
            || "container".equals(type)
            || "x6".equals(type)
            || "line".equals(type);
    }

    private String extractContent(JsonNode block, String type, String title) {
        if ("richtext".equals(type) || "richText".equals(type)) {
            JsonNode document = block.get("document");
            if (TiptapDocumentWalker.isDocument(document)) {
                return firstNonBlank(TiptapDocumentWalker.extractPlainText(document), title);
            }
            return firstNonBlank(textValue(block, "content"), title);
        }
        if ("table".equals(type)) {
            return firstNonBlank(extractTableText(block.get("tableData")), title);
        }
        if ("container".equals(type)) {
            return firstNonBlank(title, "");
        }
        if ("x6".equals(type) || "line".equals(type)) {
            return firstNonBlank(title, textValue(block, "content"));
        }
        return "";
    }

    private String extractTableText(JsonNode tableData) {
        if (tableData == null || tableData.isNull()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        JsonNode headers = tableData.get("headers");
        if (headers instanceof ArrayNode headerArray) {
            lines.add(joinTextArray(headerArray));
        }
        JsonNode rows = tableData.get("rows");
        if (rows instanceof ArrayNode rowArray) {
            for (JsonNode row : rowArray) {
                if (row instanceof ArrayNode cellArray) {
                    lines.add(joinTextArray(cellArray));
                }
            }
        }
        return String.join("\n", lines).trim();
    }

    private String joinTextArray(ArrayNode values) {
        List<String> parts = new ArrayList<>();
        for (JsonNode value : values) {
            String text = value.isTextual() ? value.asText() : value.toString();
            if (!text.isBlank()) {
                parts.add(text.trim());
            }
        }
        return String.join(" | ", parts);
    }

    private Map<String, Object> metadata(JsonNode block) {
        JsonNode metadata = block.get("metadata");
        if (metadata == null || metadata.isNull()) {
            return Map.of();
        }
        return objectMapper.convertValue(metadata, HashMap.class);
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
