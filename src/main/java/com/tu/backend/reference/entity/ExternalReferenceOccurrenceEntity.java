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
    name = "external_reference_occurrence",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_external_reference_occurrence_source",
            columnNames = {
                "page_id",
                "block_id",
                "source_kind",
                "source_locator",
                "occurrence_index",
                "url"
            }
        )
    }
)
public class ExternalReferenceOccurrenceEntity {

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

    @Column(name = "occurrence_index", nullable = false)
    private Integer occurrenceIndex;

    @Column(length = 350, nullable = false)
    private String url;

    @Column(name = "link_text", length = 255)
    private String linkText;

    @Column(name = "render_mode", length = 16, nullable = false)
    private String renderMode;

    @Column(name = "resource_item_id", length = 64)
    private String resourceItemId;

    @Column(name = "binding_mode", length = 32, nullable = false)
    private String bindingMode;

    @Column(name = "display_text", length = 255)
    private String displayText;

    @Column(name = "citation_locator", length = 255)
    private String citationLocator;

    @Column(name = "citation_note", length = 1024)
    private String citationNote;

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

    public Integer getOccurrenceIndex() {
        return occurrenceIndex;
    }

    public void setOccurrenceIndex(Integer occurrenceIndex) {
        this.occurrenceIndex = occurrenceIndex;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public String getRenderMode() {
        return renderMode;
    }

    public void setRenderMode(String renderMode) {
        this.renderMode = renderMode;
    }

    public String getResourceItemId() {
        return resourceItemId;
    }

    public void setResourceItemId(String resourceItemId) {
        this.resourceItemId = resourceItemId;
    }

    public String getBindingMode() {
        return bindingMode;
    }

    public void setBindingMode(String bindingMode) {
        this.bindingMode = bindingMode;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getCitationLocator() {
        return citationLocator;
    }

    public void setCitationLocator(String citationLocator) {
        this.citationLocator = citationLocator;
    }

    public String getCitationNote() {
        return citationNote;
    }

    public void setCitationNote(String citationNote) {
        this.citationNote = citationNote;
    }
}
