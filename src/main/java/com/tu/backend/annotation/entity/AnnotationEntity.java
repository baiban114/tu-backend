package com.tu.backend.annotation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "annotation")
public class AnnotationEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "page_id", length = 64)
    private String pageId;

    @Column(name = "block_id", length = 128)
    private String blockId;

    @Column(name = "selected_text", columnDefinition = "text")
    private String selectedText;

    @Column(name = "context_before", columnDefinition = "text")
    private String contextBefore;

    @Column(name = "context_after", columnDefinition = "text")
    private String contextAfter;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "color", length = 32)
    private String color;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "orphaned_at")
    private LocalDateTime orphanedAt;

    @Column(name = "scope", length = 16)
    private String scope;

    @Column(name = "from_pos")
    private Integer from;

    @Column(name = "to_pos")
    private Integer to;

    @Column(name = "page_title", length = 255)
    private String pageTitle;

    @Column(name = "block_type", length = 64)
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

    public String getSelectedText() {
        return selectedText;
    }

    public void setSelectedText(String selectedText) {
        this.selectedText = selectedText;
    }

    public String getContextBefore() {
        return contextBefore;
    }

    public void setContextBefore(String contextBefore) {
        this.contextBefore = contextBefore;
    }

    public String getContextAfter() {
        return contextAfter;
    }

    public void setContextAfter(String contextAfter) {
        this.contextAfter = contextAfter;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOrphanedAt() {
        return orphanedAt;
    }

    public void setOrphanedAt(LocalDateTime orphanedAt) {
        this.orphanedAt = orphanedAt;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
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
