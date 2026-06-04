package com.tu.backend.externalresource.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "external_resource_item_relation",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_external_resource_item_relation_pair",
            columnNames = {"from_item_id", "to_item_id", "relation_type"}
        )
    }
)
public class ResourceItemRelationEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "from_item_id", length = 64, nullable = false)
    private String fromItemId;

    @Column(name = "to_item_id", length = 64, nullable = false)
    private String toItemId;

    @Column(name = "relation_type", length = 32, nullable = false)
    private String relationType;

    @Column(length = 1024)
    private String note;

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

    public String getFromItemId() {
        return fromItemId;
    }

    public void setFromItemId(String fromItemId) {
        this.fromItemId = fromItemId;
    }

    public String getToItemId() {
        return toItemId;
    }

    public void setToItemId(String toItemId) {
        this.toItemId = toItemId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
