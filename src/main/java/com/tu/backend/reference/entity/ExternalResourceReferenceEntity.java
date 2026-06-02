package com.tu.backend.reference.entity;

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
    name = "external_resource_reference_occurrence",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_external_resource_reference_source",
            columnNames = {
                "page_id",
                "block_id",
                "source_kind",
                "source_locator"
            }
        )
    }
)
public class ExternalResourceReferenceEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "page_id", length = 64, nullable = false)
    private String pageId;

    @Column(name = "block_id", length = 128, nullable = false)
    private String blockId;

    @Column(name = "source_kind", length = 64, nullable = false)
    private String sourceKind;

    @Column(name = "source_locator", length = 128, nullable = false)
    private String sourceLocator;

    @Column(name = "resource_item_id", length = 64, nullable = false)
    private String resourceItemId;

    @Column(name = "resource_excerpt_id", length = 64)
    private String resourceExcerptId;

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

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public String getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(String sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getSourceLocator() {
        return sourceLocator;
    }

    public void setSourceLocator(String sourceLocator) {
        this.sourceLocator = sourceLocator;
    }

    public String getResourceItemId() {
        return resourceItemId;
    }

    public void setResourceItemId(String resourceItemId) {
        this.resourceItemId = resourceItemId;
    }

    public String getResourceExcerptId() {
        return resourceExcerptId;
    }

    public void setResourceExcerptId(String resourceExcerptId) {
        this.resourceExcerptId = resourceExcerptId;
    }
}
