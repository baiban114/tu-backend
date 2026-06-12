package com.tu.backend.contenttree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_tree_node")
public class ContentTreeNodeEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "scope_type", length = 32, nullable = false)
    private String scopeType;

    @Column(name = "scope_id", length = 64, nullable = false)
    private String scopeId;

    @Column(name = "parent_id", length = 64)
    private String parentId;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "estimated_hours", precision = 10, scale = 2)
    private BigDecimal estimatedHours;

    @Column(length = 255)
    private String locator;

    @Column(length = 1024)
    private String note;

    @Column(name = "source_block_id", length = 64)
    private String sourceBlockId;

    @Column(name = "outline_level")
    private Integer outlineLevel;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "preview_text", length = 512)
    private String previewText;

    @Column(name = "block_type", length = 32)
    private String blockType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.sortOrder == null) {
            this.sortOrder = 0;
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

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(BigDecimal estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSourceBlockId() {
        return sourceBlockId;
    }

    public void setSourceBlockId(String sourceBlockId) {
        this.sourceBlockId = sourceBlockId;
    }

    public Integer getOutlineLevel() {
        return outlineLevel;
    }

    public void setOutlineLevel(Integer outlineLevel) {
        this.outlineLevel = outlineLevel;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
