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
import com.tu.backend.knowledgerelation.entity.KnowledgeRelationEntity;
import com.tu.backend.knowledgerelation.repository.KnowledgeRelationRepository;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class KnowledgeRelationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final KnowledgeRelationRepository knowledgeRelationRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RelationTypeService relationTypeService;
    private final ObjectMapper objectMapper;

    public KnowledgeRelationService(
        KnowledgeRelationRepository knowledgeRelationRepository,
        KnowledgeBaseRepository knowledgeBaseRepository,
        RelationTypeService relationTypeService,
        ObjectMapper objectMapper
    ) {
        this.knowledgeRelationRepository = knowledgeRelationRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.relationTypeService = relationTypeService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeRelationDto> listRelations(
        String kbId,
        String locator,
        String relationTypeKey,
        String q,
        int page,
        int pageSize
    ) {
        ensureKbExists(kbId);
        String normalizedLocator = normalize(locator);
        String normalizedType = normalize(relationTypeKey);
        String normalizedQuery = normalize(q).toLowerCase(Locale.ROOT);
        int safePage = Math.max(0, page);
        int safePageSize = Math.clamp(pageSize, 1, 200);

        List<KnowledgeRelationDto> all = knowledgeRelationRepository.findByKbIdOrderByUpdatedAtDescCreatedAtDesc(kbId)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .filter(dto -> matches(dto, normalizedLocator, normalizedType, normalizedQuery))
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
        List<KnowledgeRelationDto> outgoing = knowledgeRelationRepository
            .findByKbIdAndFromLocatorOrderByUpdatedAtDesc(kbId, trimmed)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .toList();
        List<KnowledgeRelationDto> incoming = knowledgeRelationRepository
            .findByKbIdAndToLocatorOrderByUpdatedAtDesc(kbId, trimmed)
            .stream()
            .map(entity -> toDto(entity, relationTypeService.resolveType(kbId, entity.getRelationTypeKey())))
            .toList();
        return new RelationsByAnchorDto(trimmed, outgoing, incoming);
    }

    @Transactional
    public KnowledgeRelationDto createRelation(String kbId, CreateKnowledgeRelationRequest request) {
        ensureKbExists(kbId);
        validateAnchor(request.from());
        validateAnchor(request.to());
        RelationTypeDefDto typeDef = relationTypeService.resolveType(kbId, request.relationTypeKey());

        KnowledgeRelationEntity entity = new KnowledgeRelationEntity();
        entity.setId(RelationTypeService.newId("kr"));
        entity.setKbId(kbId);
        entity.setRelationTypeKey(typeDef.typeKey());
        applyAnchor(entity, true, request.from());
        applyAnchor(entity, false, request.to());
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
            knowledgeRelationRepository.findAll().stream()
                .filter(entity -> touchesPage(entity, pageId, pageLocator, pagePrefix))
                .forEach(entity -> knowledgeRelationRepository.delete(entity));
        }
    }

    @Transactional
    public void deleteByKbId(String kbId) {
        knowledgeRelationRepository.deleteByKbId(kbId);
    }

    KnowledgeRelationEntity buildMigratedEntity(
        String kbId,
        String relationTypeKey,
        KnowledgeAnchorDto from,
        KnowledgeAnchorDto to,
        String note
    ) {
        KnowledgeRelationEntity entity = new KnowledgeRelationEntity();
        entity.setId(RelationTypeService.newId("kr"));
        entity.setKbId(kbId);
        entity.setRelationTypeKey(relationTypeKey);
        applyAnchor(entity, true, from);
        applyAnchor(entity, false, to);
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

    private boolean touchesPage(KnowledgeRelationEntity entity, String pageId, String pageLocator, String pagePrefix) {
        return entity.getFromLocator().equals(pageLocator)
            || entity.getFromLocator().startsWith(pagePrefix)
            || entity.getToLocator().equals(pageLocator)
            || entity.getToLocator().startsWith(pagePrefix)
            || entity.getToLocator().startsWith("page:" + pageId + ":")
            || entity.getFromLocator().startsWith("page:" + pageId + ":");
    }

    private void validateAnchor(KnowledgeAnchorDto anchor) {
        if (anchor == null || anchor.kind() == null || anchor.kind().isBlank()
            || anchor.locator() == null || anchor.locator().isBlank()) {
            throw new BusinessException(40000, "invalid knowledge anchor");
        }
    }

    private void applyAnchor(KnowledgeRelationEntity entity, boolean from, KnowledgeAnchorDto anchor) {
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
        return new KnowledgeRelationDto(
            entity.getId(),
            entity.getKbId(),
            entity.getRelationTypeKey(),
            typeDef.label(),
            typeDef.color(),
            typeDef.bidirectional(),
            new KnowledgeAnchorDto(entity.getFromAnchorKind(), entity.getFromLocator(), deserializeSnapshot(entity.getFromSnapshotJson())),
            new KnowledgeAnchorDto(entity.getToAnchorKind(), entity.getToLocator(), deserializeSnapshot(entity.getToSnapshotJson())),
            entity.getNote(),
            entity.getSourceProvenance(),
            entity.getStatus()
        );
    }

    private boolean matches(KnowledgeRelationDto dto, String locator, String typeKey, String query) {
        if (!locator.isBlank()
            && !locator.equals(dto.from().locator())
            && !locator.equals(dto.to().locator())) {
            return false;
        }
        if (!typeKey.isBlank() && !typeKey.equals(dto.relationTypeKey())) {
            return false;
        }
        if (query.isBlank()) {
            return true;
        }
        String joined = String.join(" ",
            safe(dto.from().locator()),
            safe(dto.to().locator()),
            safe(dto.note()),
            safe(dto.relationTypeLabel()),
            snapshotText(dto.from().snapshot()),
            snapshotText(dto.to().snapshot())
        ).toLowerCase(Locale.ROOT);
        return joined.contains(query);
    }

    private String snapshotText(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return "";
        }
        return snapshot.values().stream().map(Objects::toString).reduce("", (a, b) -> a + " " + b);
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
