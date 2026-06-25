package com.tu.backend.knowledgerelation.service;

import com.tu.backend.common.BusinessException;
import com.tu.backend.knowledgerelation.dto.CreateRelationTypeRequest;
import com.tu.backend.knowledgerelation.dto.RelationTypeDefDto;
import com.tu.backend.knowledgerelation.entity.RelationTypeDefEntity;
import com.tu.backend.knowledgerelation.repository.RelationTypeDefRepository;
import com.tu.backend.knowledge.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RelationTypeService {

    private final RelationTypeDefRepository relationTypeDefRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public RelationTypeService(
        RelationTypeDefRepository relationTypeDefRepository,
        KnowledgeBaseRepository knowledgeBaseRepository
    ) {
        this.relationTypeDefRepository = relationTypeDefRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Transactional
    public void ensureSystemTypes() {
        seedSystemType("source", "来源", "#52c41a", false);
        seedSystemType("basis", "依据", "#389e0d", false);
        seedSystemType("case", "案例", "#1677ff", false);
        seedSystemType("cites", "引用", "#722ed1", false);
        seedSystemType("related", "相关", "#8c8c8c", true);
        seedSystemType("prerequisite", "前置", "#fa8c16", false);
    }

    private void seedSystemType(String key, String label, String color, boolean bidirectional) {
        relationTypeDefRepository.findByKbIdIsNullAndTypeKey(key).orElseGet(() -> {
            RelationTypeDefEntity entity = new RelationTypeDefEntity();
            entity.setId("rt-sys-" + key);
            entity.setKbId(null);
            entity.setTypeKey(key);
            entity.setLabel(label);
            entity.setColor(color);
            entity.setBidirectional(bidirectional);
            entity.setSystem(true);
            entity.setEnabled(true);
            return relationTypeDefRepository.save(entity);
        });
    }

    @Transactional(readOnly = true)
    public List<RelationTypeDefDto> listForKb(String kbId) {
        ensureKbExists(kbId);
        Map<String, RelationTypeDefDto> merged = new LinkedHashMap<>();
        relationTypeDefRepository.findByKbIdIsNullOrderByTypeKeyAsc().forEach(entity -> {
            if (Boolean.TRUE.equals(entity.getEnabled())) {
                merged.put(entity.getTypeKey(), toDto(entity));
            }
        });
        relationTypeDefRepository.findByKbIdOrderByTypeKeyAsc(kbId).forEach(entity -> merged.put(entity.getTypeKey(), toDto(entity)));
        return new ArrayList<>(merged.values());
    }

    @Transactional(readOnly = true)
    public RelationTypeDefDto resolveType(String kbId, String typeKey) {
        return relationTypeDefRepository.findByKbIdAndTypeKey(kbId, typeKey)
            .map(this::toDto)
            .or(() -> relationTypeDefRepository.findByKbIdIsNullAndTypeKey(typeKey).map(this::toDto))
            .orElseThrow(() -> new BusinessException(40001, "relation type not found: " + typeKey));
    }

    @Transactional
    public RelationTypeDefDto createCustom(String kbId, CreateRelationTypeRequest request) {
        ensureKbExists(kbId);
        String normalizedKey = normalizeKey(request.typeKey());
        if (normalizedKey.isBlank()) {
            throw new BusinessException(40000, "invalid relation type key");
        }
        if (relationTypeDefRepository.findByKbIdAndTypeKey(kbId, normalizedKey).isPresent()
            || relationTypeDefRepository.findByKbIdIsNullAndTypeKey(normalizedKey).isPresent()) {
            throw new BusinessException(40009, "relation type key already exists");
        }
        RelationTypeDefEntity entity = new RelationTypeDefEntity();
        entity.setId(newId("rt"));
        entity.setKbId(kbId);
        entity.setTypeKey(normalizedKey);
        entity.setLabel(request.label().trim());
        entity.setColor(blankToNull(request.color()));
        entity.setIcon(blankToNull(request.icon()));
        entity.setBidirectional(Boolean.TRUE.equals(request.bidirectional()));
        entity.setSystem(false);
        entity.setEnabled(true);
        return toDto(relationTypeDefRepository.save(entity));
    }

    private void ensureKbExists(String kbId) {
        if (!knowledgeBaseRepository.existsById(kbId)) {
            throw new BusinessException(40001, "knowledge base not found");
        }
    }

    private RelationTypeDefDto toDto(RelationTypeDefEntity entity) {
        return new RelationTypeDefDto(
            entity.getId(),
            entity.getKbId(),
            entity.getTypeKey(),
            entity.getLabel(),
            entity.getColor(),
            entity.getIcon(),
            Boolean.TRUE.equals(entity.getBidirectional()),
            Boolean.TRUE.equals(entity.getSystem()),
            Boolean.TRUE.equals(entity.getEnabled())
        );
    }

    private static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
