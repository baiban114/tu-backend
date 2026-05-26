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
    name = "internal_reference_record",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_internal_reference_record_source_target",
            columnNames = {
                "page_id",
                "block_id",
                "source_kind",
                "source_locator",
                "target_kind",
                "target_page_id",
                "target_block_id"
            }
        )
    }
)
public class InternalReferenceRecordEntity {

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

    @Column(name = "target_kind", length = 32, nullable = false)
    private String targetKind;

    @Column(name = "target_page_id", length = 64)
    private String targetPageId;

    @Column(name = "target_block_id", length = 128)
    private String targetBlockId;

    @Column(name = "ref_kind", length = 64, nullable = false)
    private String refKind;

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

    public String getTargetKind() {
        return targetKind;
    }

    public void setTargetKind(String targetKind) {
        this.targetKind = targetKind;
    }

    public String getTargetPageId() {
        return targetPageId;
    }

    public void setTargetPageId(String targetPageId) {
        this.targetPageId = targetPageId;
    }

    public String getTargetBlockId() {
        return targetBlockId;
    }

    public void setTargetBlockId(String targetBlockId) {
        this.targetBlockId = targetBlockId;
    }

    public String getRefKind() {
        return refKind;
    }

    public void setRefKind(String refKind) {
        this.refKind = refKind;
    }
}
