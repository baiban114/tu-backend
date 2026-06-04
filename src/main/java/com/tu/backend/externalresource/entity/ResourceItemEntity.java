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
    name = "external_resource_item",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_external_resource_item_identity", columnNames = {"type_id", "identity_value"})
    }
)
public class ResourceItemEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "type_id", length = 64, nullable = false)
    private String typeId;

    @Column(name = "work_id", length = 64)
    private String workId;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(name = "identity_value", length = 512)
    private String identityValue;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(length = 128)
    private String edition;

    @Column(length = 1024)
    private String note;

    @Column(name = "title_source", length = 16, nullable = false)
    private String titleSource = "auto";

    @Column(name = "work_id_source", length = 16, nullable = false)
    private String workIdSource = "auto";

    @Column(name = "variant_kind", length = 32)
    private String variantKind;

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

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIdentityValue() {
        return identityValue;
    }

    public void setIdentityValue(String identityValue) {
        this.identityValue = identityValue;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTitleSource() {
        return titleSource;
    }

    public void setTitleSource(String titleSource) {
        this.titleSource = titleSource;
    }

    public String getWorkIdSource() {
        return workIdSource;
    }

    public void setWorkIdSource(String workIdSource) {
        this.workIdSource = workIdSource;
    }

    public String getVariantKind() {
        return variantKind;
    }

    public void setVariantKind(String variantKind) {
        this.variantKind = variantKind;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
