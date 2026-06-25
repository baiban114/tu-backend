package com.tu.backend.knowledgerelation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import com.tu.backend.common.PageResponse;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgePointAnchorRequest;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgePointRequest;
import com.tu.backend.knowledgerelation.dto.KnowledgeAnchorDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointAnchorDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointDto;
import com.tu.backend.knowledgerelation.dto.UpdateKnowledgePointRequest;
import com.tu.backend.knowledgerelation.entity.KnowledgePointAnchorEntity;
import com.tu.backend.knowledgerelation.entity.KnowledgePointEntity;
import com.tu.backend.knowledgerelation.repository.KnowledgePointAnchorRepository;
import com.tu.backend.knowledgerelation.repository.KnowledgePointRepository;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class KnowledgePointService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgePointAnchorRepository knowledgePointAnchorRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ObjectMapper objectMapper;

    public KnowledgePointService(
        KnowledgePointRepository knowledgePointRepository,
        KnowledgePointAnchorRepository knowledgePointAnchorRepository,
        KnowledgeBaseRepository knowledgeBaseRepository,
        ObjectMapper objectMapper
    ) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.knowledgePointAnchorRepository = knowledgePointAnchorRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<KnowledgePointDto> listTree(String kbId) {
        ensureKbExists(kbId);
        return buildTree(knowledgePointRepository.findByKbIdOrderBySortOrderAscTitleAsc(kbId));
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgePointDto> listPoints(String kbId, String q, int page, int pageSize) {
        ensureKbExists(kbId);
        String query = normalize(q).toLowerCase(Locale.ROOT);
        int safePage = Math.max(0, page);
        int safePageSize = Math.clamp(pageSize, 1, 200);
        List<KnowledgePointDto> all = knowledgePointRepository.findByKbIdOrderBySortOrderAscTitleAsc(kbId)
            .stream()
            .map(this::toDto)
            .filter(dto -> matchesQuery(dto, query))
            .toList();
        long total = all.size();
        int fromIndex = safePage * safePageSize;
        if (fromIndex >= total) {
            return PageResponse.of(List.of(), total, safePage, safePageSize);
        }
        int toIndex = Math.min(fromIndex + safePageSize, (int) total);
        return PageResponse.of(all.subList(fromIndex, toIndex), total, safePage, safePageSize);
    }

    @Transactional(readOnly = true)
    public KnowledgePointDto getPoint(String id) {
        return toDto(findPoint(id));
    }

    @Transactional
    public KnowledgePointDto createPoint(String kbId, CreateKnowledgePointRequest request) {
        ensureKbExists(kbId);
        String title = request.title().trim();
        if (title.isBlank()) {
            throw new BusinessException(40000, "title is required");
        }
        validateParent(kbId, request.parentId());

        KnowledgePointEntity entity = new KnowledgePointEntity();
        entity.setId(RelationTypeService.newId("kp"));
        entity.setKbId(kbId);
        entity.setParentId(blankToNull(request.parentId()));
        entity.setTitle(title);
        entity.setSummary(blankToNull(request.summary()));
        entity.setStatus("active");
        if (request.estimatedHours() != null) {
            entity.setEstimatedHours(BigDecimal.valueOf(request.estimatedHours()));
        }
        entity.setSortOrder(nextSortOrder(kbId, entity.getParentId()));
        KnowledgePointEntity saved = knowledgePointRepository.save(entity);

        if (request.sourceAnchor() != null) {
            bindAnchorInternal(saved.getId(), request.sourceAnchor(), "primary", true);
        }
        return toDto(saved);
    }

    @Transactional
    public KnowledgePointDto updatePoint(String id, UpdateKnowledgePointRequest request) {
        KnowledgePointEntity entity = findPoint(id);
        if (request.parentId() != null) {
            validateParent(entity.getKbId(), request.parentId());
            entity.setParentId(blankToNull(request.parentId()));
        }
        if (request.title() != null && !request.title().isBlank()) {
            entity.setTitle(request.title().trim());
        }
        if (request.summary() != null) {
            entity.setSummary(blankToNull(request.summary()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim());
        }
        if (request.estimatedHours() != null) {
            entity.setEstimatedHours(BigDecimal.valueOf(request.estimatedHours()));
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        return toDto(knowledgePointRepository.save(entity));
    }

    @Transactional
    public void deletePoint(String id) {
        KnowledgePointEntity entity = findPoint(id);
        knowledgePointAnchorRepository.deleteByKnowledgePointId(id);
        knowledgePointRepository.delete(entity);
    }

    @Transactional
    public void deleteByKbId(String kbId) {
        List<KnowledgePointEntity> points = knowledgePointRepository.findByKbIdOrderBySortOrderAscTitleAsc(kbId);
        for (KnowledgePointEntity point : points) {
            knowledgePointAnchorRepository.deleteByKnowledgePointId(point.getId());
        }
        knowledgePointRepository.deleteByKbId(kbId);
    }

    @Transactional
    public void deleteByPageId(String pageId) {
        String pageLocator = "page:" + pageId;
        knowledgePointAnchorRepository.deleteByPageLocator(pageLocator, pageLocator + ":");
    }

    @Transactional(readOnly = true)
    public List<KnowledgePointAnchorDto> listAnchors(String pointId) {
        findPoint(pointId);
        return knowledgePointAnchorRepository.findByKnowledgePointIdOrderByPrimaryAnchorDescCreatedAtAsc(pointId)
            .stream()
            .map(this::toAnchorDto)
            .toList();
    }

    @Transactional
    public KnowledgePointAnchorDto addAnchor(String pointId, CreateKnowledgePointAnchorRequest request) {
        findPoint(pointId);
        return bindAnchorInternal(pointId, request.anchor(), defaultRole(request.role()), Boolean.TRUE.equals(request.primary()));
    }

    @Transactional
    public void deleteAnchor(String anchorId) {
        KnowledgePointAnchorEntity entity = knowledgePointAnchorRepository.findById(anchorId)
            .orElseThrow(() -> new BusinessException(40001, "knowledge point anchor not found"));
        knowledgePointAnchorRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<KnowledgePointDto> findPointsByLocator(String kbId, String locator) {
        ensureKbExists(kbId);
        if (locator == null || locator.isBlank()) {
            return List.of();
        }
        String trimmed = locator.trim();
        return knowledgePointAnchorRepository.findByLocatorOrderByUpdatedAtDesc(trimmed)
            .stream()
            .map(anchor -> knowledgePointRepository.findById(anchor.getKnowledgePointId()).orElse(null))
            .filter(Objects::nonNull)
            .filter(point -> kbId.equals(point.getKbId()))
            .distinct()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public String ensurePointForAnchor(String kbId, KnowledgeAnchorDto anchor, String title, String parentId) {
        if (anchor == null || anchor.locator() == null || anchor.locator().isBlank()) {
            throw new BusinessException(40000, "invalid knowledge anchor");
        }
        String locator = anchor.locator().trim();
        for (KnowledgePointAnchorEntity existing : knowledgePointAnchorRepository.findByLocatorOrderByUpdatedAtDesc(locator)) {
            KnowledgePointEntity point = knowledgePointRepository.findById(existing.getKnowledgePointId()).orElse(null);
            if (point != null && kbId.equals(point.getKbId())) {
                return point.getId();
            }
        }
        String resolvedTitle = resolveTitle(title, anchor);
        CreateKnowledgePointRequest request = new CreateKnowledgePointRequest(
            parentId,
            resolvedTitle,
            null,
            null,
            anchor
        );
        return createPoint(kbId, request).getId();
    }

    KnowledgePointEntity findPointEntity(String id) {
        return findPoint(id);
    }

    Map<String, String> loadTitles(String kbId, Iterable<String> pointIds) {
        Map<String, String> titles = new HashMap<>();
        for (String pointId : pointIds) {
            if (pointId == null || pointId.isBlank()) {
                continue;
            }
            knowledgePointRepository.findById(pointId)
                .filter(point -> kbId.equals(point.getKbId()))
                .ifPresent(point -> titles.put(point.getId(), point.getTitle()));
        }
        return titles;
    }

    private KnowledgePointAnchorDto bindAnchorInternal(
        String pointId,
        KnowledgeAnchorDto anchor,
        String role,
        boolean primary
    ) {
        validateAnchor(anchor);
        String locator = anchor.locator().trim();
        knowledgePointAnchorRepository.findFirstByKnowledgePointIdAndLocator(pointId, locator).ifPresent(existing -> {
            throw new BusinessException(40009, "anchor already bound to this knowledge point");
        });
        if (primary) {
            clearPrimaryFlag(pointId);
        }
        KnowledgePointAnchorEntity entity = new KnowledgePointAnchorEntity();
        entity.setId(RelationTypeService.newId("kpa"));
        entity.setKnowledgePointId(pointId);
        entity.setAnchorKind(anchor.kind());
        entity.setLocator(locator);
        entity.setSnapshotJson(serializeSnapshot(anchor.snapshot()));
        entity.setRole(role);
        entity.setPrimaryAnchor(primary);
        return toAnchorDto(knowledgePointAnchorRepository.save(entity));
    }

    private void clearPrimaryFlag(String pointId) {
        for (KnowledgePointAnchorEntity anchor : knowledgePointAnchorRepository.findByKnowledgePointIdOrderByPrimaryAnchorDescCreatedAtAsc(pointId)) {
            if (Boolean.TRUE.equals(anchor.getPrimaryAnchor())) {
                anchor.setPrimaryAnchor(false);
                knowledgePointAnchorRepository.save(anchor);
            }
        }
    }

    private KnowledgePointEntity findPoint(String id) {
        return knowledgePointRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "knowledge point not found"));
    }

    private void validateParent(String kbId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return;
        }
        KnowledgePointEntity parent = findPoint(parentId);
        if (!kbId.equals(parent.getKbId())) {
            throw new BusinessException(40000, "parent knowledge point must belong to the same knowledge base");
        }
    }

    private int nextSortOrder(String kbId, String parentId) {
        return knowledgePointRepository.findByKbIdOrderBySortOrderAscTitleAsc(kbId)
            .stream()
            .filter(point -> Objects.equals(parentId, point.getParentId()))
            .map(KnowledgePointEntity::getSortOrder)
            .max(Integer::compareTo)
            .orElse(-1) + 1;
    }

    private List<KnowledgePointDto> buildTree(List<KnowledgePointEntity> points) {
        Map<String, KnowledgePointDto> dtoMap = new HashMap<>();
        List<KnowledgePointDto> roots = new ArrayList<>();
        for (KnowledgePointEntity point : points) {
            dtoMap.put(point.getId(), toDto(point));
        }
        for (KnowledgePointEntity point : points) {
            KnowledgePointDto current = dtoMap.get(point.getId());
            if (point.getParentId() == null) {
                roots.add(current);
                continue;
            }
            KnowledgePointDto parent = dtoMap.get(point.getParentId());
            if (parent != null) {
                parent.getChildren().add(current);
            } else {
                roots.add(current);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<KnowledgePointDto> nodes) {
        nodes.sort(Comparator.comparingInt(KnowledgePointDto::getSortOrder).thenComparing(KnowledgePointDto::getTitle));
        for (KnowledgePointDto node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private KnowledgePointDto toDto(KnowledgePointEntity entity) {
        KnowledgePointDto dto = new KnowledgePointDto();
        dto.setId(entity.getId());
        dto.setKbId(entity.getKbId());
        dto.setParentId(entity.getParentId());
        dto.setTitle(entity.getTitle());
        dto.setSummary(entity.getSummary());
        dto.setStatus(entity.getStatus());
        dto.setEstimatedHours(entity.getEstimatedHours() == null ? null : entity.getEstimatedHours().doubleValue());
        dto.setSortOrder(entity.getSortOrder() == null ? 0 : entity.getSortOrder());
        return dto;
    }

    private KnowledgePointAnchorDto toAnchorDto(KnowledgePointAnchorEntity entity) {
        return new KnowledgePointAnchorDto(
            entity.getId(),
            entity.getKnowledgePointId(),
            entity.getAnchorKind(),
            entity.getLocator(),
            deserializeSnapshot(entity.getSnapshotJson()),
            entity.getRole(),
            Boolean.TRUE.equals(entity.getPrimaryAnchor())
        );
    }

    private boolean matchesQuery(KnowledgePointDto dto, String query) {
        if (query.isBlank()) {
            return true;
        }
        String haystack = String.join(" ",
            safe(dto.getTitle()),
            safe(dto.getSummary()),
            safe(dto.getStatus())
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(query);
    }

    private String resolveTitle(String title, KnowledgeAnchorDto anchor) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        Map<String, Object> snapshot = anchor.snapshot();
        if (snapshot != null) {
            Object snapshotTitle = snapshot.get("title");
            if (snapshotTitle != null && !snapshotTitle.toString().isBlank()) {
                return snapshotTitle.toString().trim();
            }
        }
        return "未命名知识点";
    }

    private void validateAnchor(KnowledgeAnchorDto anchor) {
        if (anchor == null || anchor.kind() == null || anchor.kind().isBlank()
            || anchor.locator() == null || anchor.locator().isBlank()) {
            throw new BusinessException(40000, "invalid knowledge anchor");
        }
    }

    private String serializeSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(40000, "invalid anchor snapshot");
        }
    }

    private Map<String, Object> deserializeSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private void ensureKbExists(String kbId) {
        if (!knowledgeBaseRepository.existsById(kbId)) {
            throw new BusinessException(40001, "knowledge base not found");
        }
    }

    private static String defaultRole(String role) {
        if (role == null || role.isBlank()) {
            return "primary";
        }
        return role.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
