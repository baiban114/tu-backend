package com.tu.backend.knowledgerelation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "knowledge_relation",
    indexes = {
        @Index(name = "idx_kr_kb_from", columnList = "kb_id, from_locator"),
        @Index(name = "idx_kr_kb_to", columnList = "kb_id, to_locator"),
        @Index(name = "idx_kr_kb_type", columnList = "kb_id, relation_type_key"),
        @Index(name = "idx_kr_kb_from_point", columnList = "kb_id, from_point_id"),
        @Index(name = "idx_kr_kb_to_point", columnList = "kb_id, to_point_id")
    }
)
public class KnowledgeRelationEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "kb_id", length = 64, nullable = false)
    private String kbId;

    @Column(name = "relation_type_key", length = 64, nullable = false)
    private String relationTypeKey;

    @Column(name = "from_point_id", length = 64)
    private String fromPointId;

    @Column(name = "to_point_id", length = 64)
    private String toPointId;

    @Column(name = "from_anchor_kind", length = 32)
    private String fromAnchorKind;

    @Column(name = "from_locator", length = 512)
    private String fromLocator;

    @Lob
    @Column(name = "from_snapshot_json", columnDefinition = "TEXT")
    private String fromSnapshotJson;

    @Column(name = "to_anchor_kind", length = 32)
    private String toAnchorKind;

    @Column(name = "to_locator", length = 512)
    private String toLocator;

    @Lob
    @Column(name = "to_snapshot_json", columnDefinition = "TEXT")
    private String toSnapshotJson;

    @Column(length = 1024)
    private String note;

    @Lob
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "source_provenance", length = 32, nullable = false)
    private String sourceProvenance;

    @Column(length = 16, nullable = false)
    private String status = "ok";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getRelationTypeKey() {
        return relationTypeKey;
    }

    public void setRelationTypeKey(String relationTypeKey) {
        this.relationTypeKey = relationTypeKey;
    }

    public String getFromPointId() {
        return fromPointId;
    }

    public void setFromPointId(String fromPointId) {
        this.fromPointId = fromPointId;
    }

    public String getToPointId() {
        return toPointId;
    }

    public void setToPointId(String toPointId) {
        this.toPointId = toPointId;
    }

    public String getFromAnchorKind() {
        return fromAnchorKind;
    }

    public void setFromAnchorKind(String fromAnchorKind) {
        this.fromAnchorKind = fromAnchorKind;
    }

    public String getFromLocator() {
        return fromLocator;
    }

    public void setFromLocator(String fromLocator) {
        this.fromLocator = fromLocator;
    }

    public String getFromSnapshotJson() {
        return fromSnapshotJson;
    }

    public void setFromSnapshotJson(String fromSnapshotJson) {
        this.fromSnapshotJson = fromSnapshotJson;
    }

    public String getToAnchorKind() {
        return toAnchorKind;
    }

    public void setToAnchorKind(String toAnchorKind) {
        this.toAnchorKind = toAnchorKind;
    }

    public String getToLocator() {
        return toLocator;
    }

    public void setToLocator(String toLocator) {
        this.toLocator = toLocator;
    }

    public String getToSnapshotJson() {
        return toSnapshotJson;
    }

    public void setToSnapshotJson(String toSnapshotJson) {
        this.toSnapshotJson = toSnapshotJson;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getSourceProvenance() {
        return sourceProvenance;
    }

    public void setSourceProvenance(String sourceProvenance) {
        this.sourceProvenance = sourceProvenance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
