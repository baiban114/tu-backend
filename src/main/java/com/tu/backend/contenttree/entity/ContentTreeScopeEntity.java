package com.tu.backend.contenttree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_tree_scope")
public class ContentTreeScopeEntity {

    @EmbeddedId
    private ContentTreeScopeId id;

    @Column(name = "kb_id", length = 64)
    private String kbId;

    @Column(name = "content_fingerprint", length = 128)
    private String contentFingerprint;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ContentTreeScopeId getId() {
        return id;
    }

    public void setId(ContentTreeScopeId id) {
        this.id = id;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getContentFingerprint() {
        return contentFingerprint;
    }

    public void setContentFingerprint(String contentFingerprint) {
        this.contentFingerprint = contentFingerprint;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
