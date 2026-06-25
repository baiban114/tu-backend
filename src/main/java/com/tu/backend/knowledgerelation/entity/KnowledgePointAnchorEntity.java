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
    name = "knowledge_point_anchor",
    indexes = {
        @Index(name = "idx_kpa_point", columnList = "knowledge_point_id"),
        @Index(name = "idx_kpa_locator", columnList = "locator")
    }
)
public class KnowledgePointAnchorEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "knowledge_point_id", length = 64, nullable = false)
    private String knowledgePointId;

    @Column(name = "anchor_kind", length = 32, nullable = false)
    private String anchorKind;

    @Column(length = 512, nullable = false)
    private String locator;

    @Lob
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(length = 32, nullable = false)
    private String role = "primary";

    @Column(name = "is_primary", nullable = false)
    private Boolean primaryAnchor = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.role == null || this.role.isBlank()) {
            this.role = "primary";
        }
        if (this.primaryAnchor == null) {
            this.primaryAnchor = false;
        }
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

    public String getKnowledgePointId() {
        return knowledgePointId;
    }

    public void setKnowledgePointId(String knowledgePointId) {
        this.knowledgePointId = knowledgePointId;
    }

    public String getAnchorKind() {
        return anchorKind;
    }

    public void setAnchorKind(String anchorKind) {
        this.anchorKind = anchorKind;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getPrimaryAnchor() {
        return primaryAnchor;
    }

    public void setPrimaryAnchor(Boolean primaryAnchor) {
        this.primaryAnchor = primaryAnchor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
