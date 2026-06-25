package com.tu.backend.knowledgerelation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import com.tu.backend.common.PageResponse;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgeRelationRequest;
import com.tu.backend.knowledgerelation.dto.KnowledgeAnchorDto;
import com.tu.backend.knowledgerelation.dto.KnowledgeRelationDto;
import com.tu.backend.knowledgerelation.dto.RelationTypeDefDto;
import com.tu.backend.knowledgerelation.dto.RelationsByAnchorDto;
import com.tu.backend.knowledgerelation.dto.RelationsByPointDto;
import com.tu.backend.knowledgerelation.entity.KnowledgeRelationEntity;
import com.tu.backend.knowledgerelation.repository.KnowledgeRelationRepository;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class KnowledgeRelationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final KnowledgeRelationRepository knowledgeRelationRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RelationTypeService relationTypeService;
    private final KnowledgePointService knowledgePointService;
    private final ObjectMapper objectMapper;

    public KnowledgeRelationService(
        KnowledgeRelationRepository knowledgeRelationRepository,
        KnowledgeBaseRepository knowledgeBaseRepository,
        RelationTypeService relationTypeService,
        KnowledgePointService knowledgePointService,
        ObjectMapper objectMapper
    ) {
        this.knowledgeRelationRepository = knowledgeRelationRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.relationTypeService = relationTypeService;
        this.knowledgePointService = knowledgePointService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeRelationDto> listRelations(
        String kbId,
        String locator,
        String pointId,
        String relationTypeKey,
        String q,
        int page,
        int pageSize
    ) {
        ensureKbExists(kbId);
        String normalizedLocator = normalize(locator);
        String normalizedPointId = normalize(pointId);
        String normalizedType = normalize(relationTypeKey);
        String normalizedQuery = normalize(q).toLowerCase(Locale.ROOT);
        int safePage = Math.max(0, page);
        int safePageSize = Math.clamp(pageSize, 1, 200);

        List<KnowledgeRelationDto> all = knowledgeRelationRepository.findByKbIdOrderByUpdatedAtDescCreatedAtDesc(kbId)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .filter(dto -> matches(dto, normalizedLocator, normalizedPointId, normalizedType, normalizedQuery))
            .sorted(Comparator.comparing(KnowledgeRelationDto::id).reversed())
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
    public RelationsByAnchorDto listByAnchor(String kbId, String locator) {
        ensureKbExists(kbId);
        if (locator == null || locator.isBlank()) {
            throw new BusinessException(40000, "locator is required");
        }
        String trimmed = locator.trim();
        Set<String> pointIds = new HashSet<>();
        knowledgePointService.findPointsByLocator(kbId, trimmed).forEach(point -> pointIds.add(point.getId()));

        List<KnowledgeRelationDto> outgoing = new ArrayList<>();
        List<KnowledgeRelationDto> incoming = new ArrayList<>();
        for (String pointId : pointIds) {
            RelationsByPointDto byPoint = listByPoint(kbId, pointId);
            outgoing.addAll(byPoint.outgoing());
            incoming.addAll(byPoint.incoming());
        }

        knowledgeRelationRepository.findByKbIdAndFromLocatorOrderByUpdatedAtDesc(kbId, trimmed)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .forEach(outgoing::add);
        knowledgeRelationRepository.findByKbIdAndToLocatorOrderByUpdatedAtDesc(kbId, trimmed)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .forEach(incoming::add);

        return new RelationsByAnchorDto(trimmed, dedupe(outgoing), dedupe(incoming));
    }

    @Transactional(readOnly = true)
    public RelationsByPointDto listByPoint(String kbId, String pointId) {
        ensureKbExists(kbId);
        knowledgePointService.findPointEntity(pointId);
        List<KnowledgeRelationDto> outgoing = knowledgeRelationRepository
            .findByKbIdAndFromPointIdOrderByUpdatedAtDesc(kbId, pointId)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .toList();
        List<KnowledgeRelationDto> incoming = knowledgeRelationRepository
            .findByKbIdAndToPointIdOrderByUpdatedAtDesc(kbId, pointId)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .toList();
        return new RelationsByPointDto(pointId, outgoing, incoming);
    }

    @Transactional
    public KnowledgeRelationDto createRelation(String kbId, CreateKnowledgeRelationRequest request) {
        ensureKbExists(kbId);
        RelationTypeDefDto typeDef = relationTypeService.resolveType(kbId, request.relationTypeKey());

        String fromPointId = normalize(request.fromPointId());
        String toPointId = normalize(request.toPointId());
        if (fromPointId.isBlank() || toPointId.isBlank()) {
            throw new BusinessException(40000, "fromPointId and toPointId are required");
        }
        knowledgePointService.findPointEntity(fromPointId);
        knowledgePointService.findPointEntity(toPointId);

        KnowledgeRelationEntity entity = new KnowledgeRelationEntity();
        entity.setId(RelationTypeService.newId("kr"));
        entity.setKbId(kbId);
        entity.setRelationTypeKey(typeDef.typeKey());
        entity.setFromPointId(fromPointId);
        entity.setToPointId(toPointId);
        if (request.from() != null) {
            applyAnchor(entity, true, request.from());
        }
        if (request.to() != null) {
            applyAnchor(entity, false, request.to());
        }
        entity.setNote(blankToNull(request.note()));
        entity.setSourceProvenance("user");
        entity.setStatus("ok");
        return toDto(knowledgeRelationRepository.save(entity), typeDef);
    }

    @Transactional
    public void deleteRelation(String id) {
        KnowledgeRelationEntity entity = knowledgeRelationRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "knowledge relation not found"));
        knowledgeRelationRepository.delete(entity);
    }

    @Transactional
    public void deleteByPageIds(Collection<String> pageIds) {
        if (pageIds.isEmpty()) {
            return;
        }
        for (String pageId : pageIds) {
            String pageLocator = "page:" + pageId;
            String pagePrefix = pageLocator + ":";
            knowledgePointService.deleteByPageId(pageId);
            knowledgeRelationRepository.findAll().stream()
                .filter(entity -> touchesPage(entity, pageId, pageLocator, pagePrefix))
                .forEach(entity -> knowledgeRelationRepository.delete(entity));
        }
    }

    @Transactional
    public void deleteByKbId(String kbId) {
        knowledgeRelationRepository.deleteByKbId(kbId);
    }

    KnowledgeRelationEntity buildMigratedPointEntity(
        String kbId,
        String relationTypeKey,
        String fromPointId,
        String toPointId,
        KnowledgeAnchorDto from,
        KnowledgeAnchorDto to,
        String note
    ) {
        KnowledgeRelationEntity entity = new KnowledgeRelationEntity();
        entity.setId(RelationTypeService.newId("kr"));
        entity.setKbId(kbId);
        entity.setRelationTypeKey(relationTypeKey);
        entity.setFromPointId(fromPointId);
        entity.setToPointId(toPointId);
        if (from != null) {
            applyAnchor(entity, true, from);
        }
        if (to != null) {
            applyAnchor(entity, false, to);
        }
        entity.setNote(blankToNull(note));
        entity.setSourceProvenance("migrated");
        entity.setStatus("ok");
        return entity;
    }

    void saveMigratedBatch(List<KnowledgeRelationEntity> entities) {
        if (!entities.isEmpty()) {
            knowledgeRelationRepository.saveAll(entities);
        }
    }

    void deleteMigratedForPage(String kbId, String pageId) {
        knowledgeRelationRepository.deleteMigratedByPage(kbId, "page:" + pageId, "page:" + pageId + ":");
    }

    @Transactional
    public void migrateLegacyAnchorRelations(String kbId) {
        ensureKbExists(kbId);
        List<KnowledgeRelationEntity> legacy = knowledgeRelationRepository.findByKbIdOrderByUpdatedAtDescCreatedAtDesc(kbId)
            .stream()
            .filter(entity -> entity.getFromPointId() == null || entity.getToPointId() == null)
            .toList();
        for (KnowledgeRelationEntity entity : legacy) {
            KnowledgeAnchorDto from = anchorFromEntity(entity, true);
            KnowledgeAnchorDto to = anchorFromEntity(entity, false);
            if (from == null || to == null) {
                continue;
            }
            String fromTitle = titleFromSnapshot(from.snapshot());
            String toTitle = titleFromSnapshot(to.snapshot());
            entity.setFromPointId(knowledgePointService.ensurePointForAnchor(kbId, from, fromTitle, null));
            entity.setToPointId(knowledgePointService.ensurePointForAnchor(kbId, to, toTitle, null));
            knowledgeRelationRepository.save(entity);
        }
    }

    private KnowledgeAnchorDto anchorFromEntity(KnowledgeRelationEntity entity, boolean from) {
        String kind = from ? entity.getFromAnchorKind() : entity.getToAnchorKind();
        String locator = from ? entity.getFromLocator() : entity.getToLocator();
        if (kind == null || kind.isBlank() || locator == null || locator.isBlank()) {
            return null;
        }
        return new KnowledgeAnchorDto(kind, locator, deserializeSnapshot(from ? entity.getFromSnapshotJson() : entity.getToSnapshotJson()));
    }

    private boolean touchesPage(KnowledgeRelationEntity entity, String pageId, String pageLocator, String pagePrefix) {
        return locatorTouchesPage(entity.getFromLocator(), pageId, pageLocator, pagePrefix)
            || locatorTouchesPage(entity.getToLocator(), pageId, pageLocator, pagePrefix);
    }

    private boolean locatorTouchesPage(String locator, String pageId, String pageLocator, String pagePrefix) {
        if (locator == null || locator.isBlank()) {
            return false;
        }
        return locator.equals(pageLocator)
            || locator.startsWith(pagePrefix)
            || locator.startsWith("page:" + pageId + ":");
    }

    private void applyAnchor(KnowledgeRelationEntity entity, boolean from, KnowledgeAnchorDto anchor) {
        validateAnchor(anchor);
        if (from) {
            entity.setFromAnchorKind(anchor.kind());
            entity.setFromLocator(anchor.locator().trim());
            entity.setFromSnapshotJson(serializeSnapshot(anchor.snapshot()));
        } else {
            entity.setToAnchorKind(anchor.kind());
            entity.setToLocator(anchor.locator().trim());
            entity.setToSnapshotJson(serializeSnapshot(anchor.snapshot()));
        }
    }

    private KnowledgeRelationDto toDto(KnowledgeRelationEntity entity, RelationTypeDefDto typeDef) {
        Map<String, String> titles = knowledgePointService.loadTitles(
            entity.getKbId(),
            List.of(entity.getFromPointId(), entity.getToPointId())
        );
        KnowledgeAnchorDto fromAnchor = anchorDtoFromEntity(entity, true);
        KnowledgeAnchorDto toAnchor = anchorDtoFromEntity(entity, false);
        return new KnowledgeRelationDto(
            entity.getId(),
            entity.getKbId(),
            entity.getRelationTypeKey(),
            typeDef.label(),
            typeDef.color(),
            typeDef.bidirectional(),
            entity.getFromPointId(),
            entity.getToPointId(),
            titles.get(entity.getFromPointId()),
            titles.get(entity.getToPointId()),
            fromAnchor,
            toAnchor,
            entity.getNote(),
            entity.getSourceProvenance(),
            entity.getStatus()
        );
    }

    private KnowledgeAnchorDto anchorDtoFromEntity(KnowledgeRelationEntity entity, boolean from) {
        String kind = from ? entity.getFromAnchorKind() : entity.getToAnchorKind();
        String locator = from ? entity.getFromLocator() : entity.getToLocator();
        if (kind == null || locator == null) {
            return null;
        }
        return new KnowledgeAnchorDto(kind, locator, deserializeSnapshot(from ? entity.getFromSnapshotJson() : entity.getToSnapshotJson()));
    }

    private boolean matches(
        KnowledgeRelationDto dto,
        String locator,
        String pointId,
        String typeKey,
        String query
    ) {
        if (!pointId.isBlank()
            && !pointId.equals(dto.fromPointId())
            && !pointId.equals(dto.toPointId())) {
            return false;
        }
        if (!locator.isBlank()) {
            boolean anchorMatch = dto.from() != null && locator.equals(dto.from().locator())
                || dto.to() != null && locator.equals(dto.to().locator());
            if (!anchorMatch && pointId.isBlank()) {
                return false;
            }
        }
        if (!typeKey.isBlank() && !typeKey.equals(dto.relationTypeKey())) {
            return false;
        }
        if (query.isBlank()) {
            return true;
        }
        String joined = String.join(" ",
            safe(dto.fromPointTitle()),
            safe(dto.toPointTitle()),
            dto.from() != null ? safe(dto.from().locator()) : "",
            dto.to() != null ? safe(dto.to().locator()) : "",
            safe(dto.note()),
            safe(dto.relationTypeLabel()),
            dto.from() != null ? snapshotText(dto.from().snapshot()) : "",
            dto.to() != null ? snapshotText(dto.to().snapshot()) : ""
        ).toLowerCase(Locale.ROOT);
        return joined.contains(query);
    }

    private List<KnowledgeRelationDto> dedupe(List<KnowledgeRelationDto> relations) {
        return relations.stream()
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toMap(KnowledgeRelationDto::id, dto -> dto, (a, b) -> a, java.util.LinkedHashMap::new),
                map -> new ArrayList<>(map.values())
            ));
    }

    private String titleFromSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null) {
            return null;
        }
        Object title = snapshot.get("title");
        return title == null ? null : title.toString();
    }

    private String snapshotText(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return "";
        }
        return snapshot.values().stream().map(Objects::toString).reduce("", (a, b) -> a + " " + b);
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
