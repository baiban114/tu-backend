package com.tu.backend.content.tiptap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walks ProseMirror JSON documents stored in {@code richtext} blocks (schema v2).
 */
@SuppressWarnings("SpellCheckingInspection")
public final class TiptapDocumentWalker {

    private static final Pattern TU_LINK_URL_PATTERN = Pattern.compile(
        "<!--tu:link-display[^>]*url=\"([^\"]+)\"[^>]*-->"
    );

    private TiptapDocumentWalker() {
    }

    public record TiptapHeading(String text, int level, String blockId) {
    }

    public record TiptapEmbedOutline(String title, String blockId) {
    }

    public static boolean isDocument(JsonNode node) {
        return node != null && node.isObject() && "doc".equals(text(node, "type"));
    }

    public static String extractPlainText(JsonNode document) {
        if (!isDocument(document)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        collectPlainText(document.get("content"), sb, false);
        return sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static List<TiptapHeading> extractHeadings(JsonNode document, String defaultBlockId) {
        List<TiptapHeading> headings = new ArrayList<>();
        if (!isDocument(document)) {
            return headings;
        }
        collectHeadings(document.get("content"), headings, defaultBlockId, 0);
        return headings;
    }

    public static List<TiptapEmbedOutline> extractEmbedOutlines(JsonNode document, String defaultBlockId) {
        List<TiptapEmbedOutline> outlines = new ArrayList<>();
        if (!isDocument(document)) {
            return outlines;
        }
        collectEmbedOutlines(document.get("content"), outlines, defaultBlockId);
        return outlines;
    }

    public static String summarizeDocumentEmbeds(JsonNode document) {
        List<TiptapEmbedOutline> embeds = extractEmbedOutlines(document, "");
        if (embeds.isEmpty()) {
            return "";
        }
        if (embeds.size() == 1) {
            return embeds.get(0).title();
        }
        return embeds.get(0).title() + " 等 " + embeds.size() + " 项";
    }

    public static String pdfExcerptBlockLabel(JsonNode attrs) {
        String fileName = text(attrs, "fileName");
        String viewMode = text(attrs, "viewMode");
        int start = attrs.path("startPage").asInt(1);
        int end = attrs.path("endPage").asInt(start);
        String name = fileName.isBlank() ? "PDF" : fileName;
        if ("full".equals(viewMode)) {
            return name + " · 全文";
        }
        if (start == end) {
            return name + " · 第" + start + "页";
        }
        return name + " · 第" + start + "–" + end + "页";
    }

    public static String urlEmbedBlockLabel(JsonNode attrs) {
        String url = text(attrs, "url");
        if (url.isBlank()) {
            return "嵌入链接";
        }
        if (url.length() > 60) {
            return url.substring(0, 57) + "…";
        }
        return url;
    }

    public static void collectReferences(
        String pageId,
        String blockId,
        String blockPath,
        JsonNode document,
        ReferenceSink sink
    ) {
        if (!isDocument(document)) {
            return;
        }
        walkNodes(document.get("content"), pageId, blockId, blockPath, sink);
    }

    public interface ReferenceSink {
        void onRef(String pageId, String blockId, String blockPath, String refId, String refType);

        void onExternalResource(
            String pageId,
            String blockId,
            String blockPath,
            String sourceLocator,
            JsonNode externalResource
        );

        void onHeadingSource(
            String pageId,
            String blockId,
            String headingBlockId,
            String resourceItemId,
            String resourceExcerptId
        );

        void onGraphData(String pageId, String blockId, String blockType, String blockPath, JsonNode graphData);

        void onTableData(String pageId, String blockId, String blockType, String blockPath, JsonNode tableData);

        void onExternalUrl(String pageId, String blockId, String blockPath, String url);
    }

    private static void walkNodes(
        JsonNode nodes,
        String pageId,
        String blockId,
        String blockPath,
        ReferenceSink sink
    ) {
        if (!(nodes instanceof ArrayNode array)) {
            return;
        }
        int index = 0;
        for (JsonNode node : array) {
            if (!node.isObject()) {
                index += 1;
                continue;
            }
            String type = text(node, "type");
            String nodeBlockId = firstNonBlank(text(node.path("attrs"), "blockId"), blockId);
            String nodePath = blockPath + ".document.content[" + index + "]";

            switch (type) {
                case "heading" -> {
                    JsonNode attrs = node.path("attrs");
                    JsonNode binding = attrs.path("sourceBinding");
                    if (binding.isObject()) {
                        String headingBlockId = text(attrs, "blockId");
                        String resourceItemId = text(binding, "resourceItemId");
                        String resourceExcerptId = text(binding, "resourceExcerptId");
                        if (!headingBlockId.isBlank() && !resourceItemId.isBlank() && !resourceExcerptId.isBlank()) {
                            sink.onHeadingSource(pageId, blockId, headingBlockId, resourceItemId, resourceExcerptId);
                        }
                    }
                    walkNodes(node.get("content"), pageId, blockId, nodePath, sink);
                }
                case "refBlock" -> {
                    String refId = text(node.path("attrs"), "refId");
                    String refType = firstNonBlank(text(node.path("attrs"), "refType"), "block");
                    if (!refId.isBlank()) {
                        sink.onRef(pageId, nodeBlockId, nodePath, refId, refType);
                    }
                }
                case "externalResourceBlock" -> {
                    JsonNode data = node.path("attrs").path("externalResource");
                    if (data.isObject()) {
                        sink.onExternalResource(pageId, nodeBlockId, nodePath, nodePath + ".attrs.externalResource", data);
                    }
                }
                case "x6Block", "timelineBlock" -> {
                    JsonNode graphData = node.path("attrs").path("graphData");
                    if (graphData.isObject()) {
                        String embedType = "x6Block".equals(type) ? "x6" : "line";
                        sink.onGraphData(pageId, nodeBlockId, embedType, nodePath, graphData);
                    }
                }
                case "tableBlock" -> {
                    JsonNode tableData = node.path("attrs").path("tableData");
                    if (tableData.isObject()) {
                        sink.onTableData(pageId, nodeBlockId, "table", nodePath, tableData);
                    }
                }
                case "urlEmbedBlock" -> {
                    String url = text(node.path("attrs"), "url");
                    if (!url.isBlank()) {
                        sink.onExternalUrl(pageId, nodeBlockId, nodePath, url);
                    }
                }
                default -> {
                    collectUrlsFromText(node, pageId, nodeBlockId, nodePath, sink);
                    walkNodes(node.get("content"), pageId, blockId, nodePath, sink);
                }
            }
            index += 1;
        }
    }

    private static void collectUrlsFromText(
        JsonNode node,
        String pageId,
        String blockId,
        String blockPath,
        ReferenceSink sink
    ) {
        if (!node.isObject()) {
            return;
        }
        JsonNode content = node.get("content");
        if (!(content instanceof ArrayNode array)) {
            return;
        }
        for (JsonNode child : array) {
            if (!child.isObject()) {
                continue;
            }
            if ("text".equals(text(child, "type"))) {
                String value = text(child, "text");
                if (value != null) {
                    Matcher matcher = TU_LINK_URL_PATTERN.matcher(value);
                    while (matcher.find()) {
                        String url = matcher.group(1);
                        if (url != null && !url.isBlank()) {
                            sink.onExternalUrl(pageId, blockId, blockPath, url);
                        }
                    }
                }
            }
        }
    }

    private static void collectEmbedOutlines(
        JsonNode nodes,
        List<TiptapEmbedOutline> outlines,
        String defaultBlockId
    ) {
        if (!(nodes instanceof ArrayNode array)) {
            return;
        }
        for (JsonNode node : array) {
            if (!node.isObject()) {
                continue;
            }
            String type = text(node, "type");
            JsonNode attrs = node.path("attrs");
            String blockId = firstNonBlank(text(attrs, "blockId"), defaultBlockId);
            String label = embedBlockLabel(type, attrs);
            if (label != null && !label.isBlank() && !blockId.isBlank()) {
                outlines.add(new TiptapEmbedOutline(label, blockId));
            }
            collectEmbedOutlines(node.get("content"), outlines, defaultBlockId);
        }
    }

    private static String embedBlockLabel(String type, JsonNode attrs) {
        return switch (type) {
            case "pdfExcerptBlock" -> pdfExcerptBlockLabel(attrs);
            case "urlEmbedBlock" -> urlEmbedBlockLabel(attrs);
            case "x6Block" -> {
                String title = text(attrs, "title");
                yield title.isBlank() ? "画板" : title;
            }
            case "timelineBlock" -> "时间轴";
            case "tableBlock" -> "表格";
            default -> null;
        };
    }

    private static void collectHeadings(
        JsonNode nodes,
        List<TiptapHeading> headings,
        String defaultBlockId,
        int levelOffset
    ) {
        if (!(nodes instanceof ArrayNode array)) {
            return;
        }
        for (JsonNode node : array) {
            if (!node.isObject()) {
                continue;
            }
            String type = text(node, "type");
            if ("heading".equals(type)) {
                int level = node.path("attrs").path("level").asInt(1) + levelOffset;
                String blockId = firstNonBlank(text(node.path("attrs"), "blockId"), defaultBlockId);
                String title = extractInlineText(node.get("content"));
                if (!title.isBlank() && blockId != null && !blockId.isBlank()) {
                    headings.add(new TiptapHeading(title, level, blockId));
                }
            }
            collectHeadings(node.get("content"), headings, defaultBlockId, levelOffset);
        }
    }

    private static void collectPlainText(JsonNode nodes, StringBuilder sb, boolean blockBreak) {
        if (!(nodes instanceof ArrayNode array)) {
            return;
        }
        boolean first = true;
        for (JsonNode node : array) {
            if (!node.isObject()) {
                continue;
            }
            if (!first && blockBreak) {
                sb.append('\n');
            }
            first = false;
            String type = text(node, "type");
            if ("text".equals(type)) {
                String value = text(node, "text");
                if (value != null) {
                    sb.append(value);
                }
            } else if ("hardBreak".equals(type)) {
                sb.append('\n');
            } else if ("heading".equals(type) || "paragraph".equals(type) || "blockquote".equals(type)
                || "listItem".equals(type) || "taskItem".equals(type)) {
                collectPlainText(node.get("content"), sb, false);
                if ("heading".equals(type) || "paragraph".equals(type)) {
                    sb.append('\n');
                }
            } else if ("bulletList".equals(type) || "orderedList".equals(type) || "taskList".equals(type)) {
                collectPlainText(node.get("content"), sb, true);
            } else if ("tableBlock".equals(type)) {
                JsonNode tableData = node.path("attrs").path("tableData");
                sb.append(extractTableText(tableData)).append('\n');
            } else if ("x6Block".equals(type) || "timelineBlock".equals(type)) {
                String title = text(node.path("attrs"), "title");
                if (title != null && !title.isBlank()) {
                    sb.append(title).append('\n');
                }
            } else if ("pdfExcerptBlock".equals(type)) {
                sb.append(pdfExcerptBlockLabel(node.path("attrs"))).append('\n');
            } else if ("urlEmbedBlock".equals(type)) {
                sb.append(urlEmbedBlockLabel(node.path("attrs"))).append('\n');
            } else {
                collectPlainText(node.get("content"), sb, false);
            }
        }
    }

    private static String extractInlineText(JsonNode nodes) {
        if (!(nodes instanceof ArrayNode array)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : array) {
            if (!node.isObject()) {
                continue;
            }
            if ("text".equals(text(node, "type"))) {
                String value = text(node, "text");
                if (value != null) {
                    sb.append(value);
                }
            } else {
                sb.append(extractInlineText(node.get("content")));
            }
        }
        return sb.toString().trim();
    }

    private static String extractTableText(JsonNode tableData) {
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

    private static String joinTextArray(ArrayNode values) {
        List<String> parts = new ArrayList<>();
        for (JsonNode value : values) {
            String text = value.isTextual() ? value.asText() : value.toString();
            if (!text.isBlank()) {
                parts.add(text.trim());
            }
        }
        return String.join(" | ", parts);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        String text = value.asText();
        return text == null ? "" : text.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
