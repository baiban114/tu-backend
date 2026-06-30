package com.tu.backend.contenttree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tu.backend.content.tiptap.TiptapDocumentWalker;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OutlineExtractor {

    private static final Pattern ATX_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final int PREVIEW_MAX = 200;

    private OutlineExtractor() {
    }

    public record ExtractedOutlineNode(
        String id,
        String parentId,
        String title,
        int sortOrder,
        int level,
        String sourceBlockId,
        String sourceType,
        String previewText,
        String blockType
    ) {
    }

    public static List<ExtractedOutlineNode> extractPageOutline(String pageId, ArrayNode blocks) {
        List<MarkdownHeading> headings = extractHeadingsFromBlocks(blocks, false, 0);
        if (headings.isEmpty()) {
            return extractDocBlockFallback(pageId, blocks);
        }
        return buildHierarchy(pageId, headings, blocks);
    }

    public static List<ExtractedOutlineNode> extractBlockOutline(String pageId, JsonNode rootBlock) {
        ArrayNode blocks = new ObjectMapper().createArrayNode();
        blocks.add(rootBlock);
        List<MarkdownHeading> headings = extractHeadingsFromBlocks(blocks, true, 1);
        if (headings.isEmpty() && isPageRefBlock(rootBlock)) {
            return List.of();
        }
        if (headings.isEmpty()) {
            return extractDocBlockFallback(pageId, blocks);
        }
        return buildHierarchy(pageId, headings, blocks);
    }

    public static String stableNodeId(String scopeId, String sourceBlockId, int level, String title) {
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase();
        String payload = scopeId + "\0" + nullToEmpty(sourceBlockId) + "\0" + level + "\0" + normalizedTitle;
        return "ctn-" + sha256Hex(payload).substring(0, 24);
    }

    private record MarkdownHeading(String text, int level, String blockId) {
    }

    private record StackItem(String nodeId, int level) {
    }

    private static List<ExtractedOutlineNode> buildHierarchy(
        String pageId,
        List<MarkdownHeading> headings,
        ArrayNode blocks
    ) {
        List<ExtractedOutlineNode> result = new ArrayList<>();
        List<StackItem> stack = new ArrayList<>();
        int sortOrder = 0;
        for (MarkdownHeading heading : headings) {
            while (!stack.isEmpty() && stack.get(stack.size() - 1).level >= heading.level) {
                stack.remove(stack.size() - 1);
            }
            String parentId = stack.isEmpty() ? null : stack.get(stack.size() - 1).nodeId;
            String nodeId = stableNodeId(pageId, heading.blockId, heading.level, heading.text);
            JsonNode block = findBlock(blocks, heading.blockId);
            result.add(new ExtractedOutlineNode(
                nodeId,
                parentId,
                heading.text,
                sortOrder++,
                heading.level,
                heading.blockId,
                "ref-child",
                block == null ? null : previewText(block),
                block == null ? null : textValue(block, "type")
            ));
            stack.add(new StackItem(nodeId, heading.level));
        }
        return result;
    }

    private static List<ExtractedOutlineNode> extractDocBlockFallback(String pageId, ArrayNode blocks) {
        List<ExtractedOutlineNode> result = new ArrayList<>();
        int sortOrder = 0;
        for (JsonNode block : blocks) {
            if (!block.isObject()) {
                continue;
            }
            String blockId = textValue(block, "id");
            if (blockId == null) {
                continue;
            }
            String title = blockPreviewLabel(block);
            if (title == null || title.isBlank()) {
                title = "文档内容";
            }
            result.add(new ExtractedOutlineNode(
                stableNodeId(pageId, blockId, 2, title),
                null,
                title,
                sortOrder++,
                2,
                blockId,
                "ref-doc-block",
                previewText(block),
                textValue(block, "type")
            ));
        }
        return result;
    }

    private static List<MarkdownHeading> extractHeadingsFromBlocks(ArrayNode blocks, boolean shift, int contentParentLevel) {
        List<MarkdownHeading> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (shift) {
            int offset = headingShiftOffset(blocks, contentParentLevel);
            collectHeadings(blocks, result, seen, offset);
        } else {
            collectHeadings(blocks, result, seen, 0);
        }
        return result;
    }

    private static void collectHeadings(ArrayNode blocks, List<MarkdownHeading> result, Set<String> seen, int offset) {
        for (JsonNode block : blocks) {
            collectBlockHeadings(block, result, seen, offset);
        }
    }

    private static void collectBlockHeadings(JsonNode block, List<MarkdownHeading> result, Set<String> seen, int offset) {
        if (!block.isObject()) {
            return;
        }
        String type = textValue(block, "type");
        String blockId = textValue(block, "id");
        if ("richtext".equals(type) || "richText".equals(type)) {
            JsonNode document = block.get("document");
            if (TiptapDocumentWalker.isDocument(document) && blockId != null) {
                for (TiptapDocumentWalker.TiptapHeading heading : TiptapDocumentWalker.extractHeadings(document, blockId)) {
                    String key = heading.blockId() + ":" + heading.level() + ":" + heading.text();
                    if (seen.add(key)) {
                        result.add(new MarkdownHeading(heading.text(), heading.level() + offset, heading.blockId()));
                    }
                }
                for (TiptapDocumentWalker.TiptapEmbedOutline embed : TiptapDocumentWalker.extractEmbedOutlines(document, blockId)) {
                    String key = embed.blockId() + ":embed:" + embed.title();
                    if (seen.add(key)) {
                        result.add(new MarkdownHeading(embed.title(), 2 + offset, embed.blockId()));
                    }
                }
            } else {
                String content = textValue(block, "content");
                if (content != null && blockId != null) {
                    result.addAll(parseMarkdownHeadingLines(shiftContentHeadings(content, offset), blockId, seen));
                }
            }
        } else if ("externalResource".equals(type) && blockId != null) {
            JsonNode snapshot = block.path("externalResource").path("snapshot");
            String excerptText = textValue(snapshot, "excerptText");
            if (excerptText != null) {
                result.addAll(parseMarkdownHeadingLines(shiftContentHeadings(excerptText, offset), blockId, seen));
            }
        }
        JsonNode children = block.get("children");
        if (children instanceof ArrayNode childArray) {
            for (JsonNode child : childArray) {
                collectBlockHeadings(child, result, seen, offset);
            }
        }
    }

    private static int headingShiftOffset(ArrayNode blocks, int contentParentLevel) {
        if (contentParentLevel == 0) {
            return 0;
        }
        int minChild = minHeadingLevelInBlocks(blocks);
        if (minChild == Integer.MAX_VALUE) {
            return 0;
        }
        return Math.max(0, contentParentLevel + 1 - minChild);
    }

    private static int minHeadingLevelInBlocks(ArrayNode blocks) {
        int min = Integer.MAX_VALUE;
        for (JsonNode block : blocks) {
            min = Math.min(min, minHeadingLevelInBlock(block));
        }
        return min;
    }

    private static int minHeadingLevelInBlock(JsonNode block) {
        int min = Integer.MAX_VALUE;
        if (!block.isObject()) {
            return min;
        }
        String type = textValue(block, "type");
        if ("richtext".equals(type) || "richText".equals(type)) {
            JsonNode document = block.get("document");
            if (TiptapDocumentWalker.isDocument(document)) {
                for (TiptapDocumentWalker.TiptapHeading heading : TiptapDocumentWalker.extractHeadings(document, textValue(block, "id"))) {
                    min = Math.min(min, heading.level());
                }
            } else {
                String content = textValue(block, "content");
                if (content != null) {
                    min = Math.min(min, minHeadingLevelInContent(content));
                }
            }
        } else if ("externalResource".equals(type)) {
            String excerptText = textValue(block.path("externalResource").path("snapshot"), "excerptText");
            if (excerptText != null) {
                min = Math.min(min, minHeadingLevelInContent(excerptText));
            }
        }
        JsonNode children = block.get("children");
        if (children instanceof ArrayNode childArray) {
            for (JsonNode child : childArray) {
                min = Math.min(min, minHeadingLevelInBlock(child));
            }
        }
        return min;
    }

    private static int minHeadingLevelInContent(String content) {
        int min = Integer.MAX_VALUE;
        for (String line : content.replace("\r\n", "\n").split("\n")) {
            Matcher matcher = ATX_HEADING.matcher(line);
            if (matcher.matches()) {
                min = Math.min(min, matcher.group(1).length());
            }
        }
        return min;
    }

    private static String shiftContentHeadings(String content, int offset) {
        if (offset == 0) {
            return content;
        }
        StringBuilder builder = new StringBuilder();
        for (String line : content.replace("\r\n", "\n").split("\n", -1)) {
            Matcher matcher = ATX_HEADING.matcher(line);
            if (matcher.matches()) {
                int newLevel = Math.min(6, matcher.group(1).length() + offset);
                builder.append("#".repeat(newLevel)).append(' ').append(matcher.group(2));
            } else {
                builder.append(line);
            }
            builder.append('\n');
        }
        if (!content.endsWith("\n") && builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    private static List<MarkdownHeading> parseMarkdownHeadingLines(String content, String blockId, Set<String> seen) {
        List<MarkdownHeading> result = new ArrayList<>();
        for (String line : content.replace("\r\n", "\n").split("\n")) {
            Matcher matcher = ATX_HEADING.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            int level = matcher.group(1).length();
            String text = stripMarkdownHeadingText(matcher.group(2));
            String key = blockId + ":" + text;
            if (!text.isBlank() && !seen.contains(key)) {
                seen.add(key);
                result.add(new MarkdownHeading(text, level, blockId));
            }
        }
        return result;
    }

    private static String stripMarkdownHeadingText(String raw) {
        String text = raw.trim();
        text = text.replaceAll("\\s*#+\\s*$", "").trim();
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        text = text.replaceAll("\\*(.+?)\\*", "$1");
        text = text.replaceAll("`(.+?)`", "$1");
        text = text.replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1");
        return text;
    }

    private static String previewText(JsonNode block) {
        String type = textValue(block, "type");
        if ("richtext".equals(type) || "richText".equals(type)) {
            JsonNode document = block.get("document");
            if (TiptapDocumentWalker.isDocument(document)) {
                String plain = TiptapDocumentWalker.extractPlainText(document);
                if (plain.isBlank()) {
                    String embedSummary = TiptapDocumentWalker.summarizeDocumentEmbeds(document);
                    if (!embedSummary.isBlank()) {
                        if (embedSummary.length() <= PREVIEW_MAX) {
                            return embedSummary;
                        }
                        return embedSummary.substring(0, PREVIEW_MAX) + "…";
                    }
                    return null;
                }
                if (plain.length() <= PREVIEW_MAX) {
                    return plain;
                }
                return plain.substring(0, PREVIEW_MAX) + "…";
            }
            String content = textValue(block, "content");
            if (content == null) {
                return null;
            }
            String plain = content
                .replaceAll("<!--[\\s\\S]*?-->", "")
                .replaceAll("[#*`>\\-_\\[\\]]", "")
                .trim();
            if (plain.length() <= PREVIEW_MAX) {
                return plain.isBlank() ? null : plain;
            }
            return plain.substring(0, PREVIEW_MAX) + "…";
        }
        String label = blockPreviewLabel(block);
        return label.equals(textValue(block, "type")) ? null : label;
    }

    private static String blockPreviewLabel(JsonNode block) {
        String title = textValue(block, "title");
        if (title != null) {
            return title;
        }
        String type = textValue(block, "type");
        if ("x6".equals(type)) {
            return "画板";
        }
        if ("table".equals(type)) {
            return "表格";
        }
        if ("line".equals(type)) {
            return "时间轴";
        }
        if ("externalResource".equals(type)) {
            JsonNode snapshot = block.path("externalResource").path("snapshot");
            String excerptTitle = textValue(snapshot, "excerptTitle");
            if (excerptTitle != null) {
                return excerptTitle;
            }
            String resourceTitle = textValue(snapshot, "resourceTitle");
            if (resourceTitle != null) {
                return resourceTitle;
            }
            return "外部资源";
        }
        if ("richtext".equals(type) || "richText".equals(type)) {
            JsonNode document = block.get("document");
            if (TiptapDocumentWalker.isDocument(document)) {
                String plain = TiptapDocumentWalker.extractPlainText(document);
                if (plain.isBlank()) {
                    String embedSummary = TiptapDocumentWalker.summarizeDocumentEmbeds(document);
                    if (!embedSummary.isBlank()) {
                        return embedSummary.length() <= PREVIEW_MAX
                            ? embedSummary
                            : embedSummary.substring(0, PREVIEW_MAX) + "…";
                    }
                    return "文档内容";
                }
                if (plain.length() <= PREVIEW_MAX) {
                    return plain;
                }
                return plain.substring(0, PREVIEW_MAX) + "…";
            }
            String content = textValue(block, "content");
            if (content != null) {
                String plain = content
                    .replaceAll("<!--[\\s\\S]*?-->", "")
                    .replaceAll("[#*`>\\-_\\[\\]]", "")
                    .trim();
                if (!plain.isBlank()) {
                    return plain.length() > 40 ? plain.substring(0, 40) + "…" : plain;
                }
            }
        }
        return type == null ? "block" : type;
    }

    private static boolean isPageRefBlock(JsonNode block) {
        return "ref".equals(textValue(block, "type")) && "page".equals(textValue(block, "refType"));
    }

    private static JsonNode findBlock(ArrayNode blocks, String blockId) {
        for (JsonNode block : blocks) {
            JsonNode found = findBlockRecursive(block, blockId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static JsonNode findBlockRecursive(JsonNode node, String blockId) {
        if (!node.isObject()) {
            return null;
        }
        if (blockId.equals(textValue(node, "id"))) {
            return node;
        }
        JsonNode children = node.get("children");
        if (children instanceof ArrayNode childArray) {
            for (JsonNode child : childArray) {
                JsonNode found = findBlockRecursive(child, blockId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
