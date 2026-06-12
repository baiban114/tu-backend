package com.tu.backend.contenttree;

import com.tu.backend.contenttree.dto.ContentTreeNodeDto;
import com.tu.backend.contenttree.entity.ContentTreeNodeEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ContentTreeHoursRollup {

    private ContentTreeHoursRollup() {
    }

    public static List<ContentTreeNodeDto> withRollup(List<ContentTreeNodeEntity> nodes) {
        Map<String, List<ContentTreeNodeEntity>> childrenByParent = new HashMap<>();
        for (ContentTreeNodeEntity node : nodes) {
            String parentKey = node.getParentId() == null ? "" : node.getParentId();
            childrenByParent.computeIfAbsent(parentKey, key -> new ArrayList<>()).add(node);
        }
        Map<String, BigDecimal> totals = new HashMap<>();
        for (ContentTreeNodeEntity node : nodes) {
            totals.put(node.getId(), computeTotal(node.getId(), childrenByParent, totals, node.getEstimatedHours()));
        }
        return nodes.stream()
            .map(node -> toDto(node, totals.getOrDefault(node.getId(), ownHours(node.getEstimatedHours()))))
            .toList();
    }

    private static BigDecimal computeTotal(
        String nodeId,
        Map<String, List<ContentTreeNodeEntity>> childrenByParent,
        Map<String, BigDecimal> memo,
        BigDecimal ownHours
    ) {
        if (memo.containsKey(nodeId)) {
            return memo.get(nodeId);
        }
        BigDecimal total = ownHours(ownHours);
        for (ContentTreeNodeEntity child : childrenByParent.getOrDefault(nodeId, List.of())) {
            total = total.add(computeTotal(child.getId(), childrenByParent, memo, child.getEstimatedHours()));
        }
        memo.put(nodeId, total);
        return total;
    }

    private static BigDecimal ownHours(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static ContentTreeNodeDto toDto(ContentTreeNodeEntity node, BigDecimal totalEstimatedHours) {
        return new ContentTreeNodeDto(
            node.getId(),
            node.getScopeType(),
            node.getScopeId(),
            node.getParentId(),
            node.getTitle(),
            node.getSortOrder(),
            node.getEstimatedHours(),
            totalEstimatedHours,
            node.getLocator(),
            node.getNote(),
            node.getSourceBlockId(),
            node.getOutlineLevel(),
            node.getSourceType(),
            node.getPreviewText(),
            node.getBlockType()
        );
    }
}
