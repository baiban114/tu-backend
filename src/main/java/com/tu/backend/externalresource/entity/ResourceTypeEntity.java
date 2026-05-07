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
    name = "external_resource_type",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_external_resource_type_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_external_resource_type_name", columnNames = "name")
    }
)
public class ResourceTypeEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 64, nullable = false)
    private String code;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 32)
    private String icon;

    @Column(length = 255)
    private String description;

    @Column(name = "identity_field_key", length = 64, nullable = false)
    private String identityFieldKey;

    @Column(name = "identity_field_label", length = 128, nullable = false)
    private String identityFieldLabel;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdentityFieldKey() {
        return identityFieldKey;
    }

    public void setIdentityFieldKey(String identityFieldKey) {
        this.identityFieldKey = identityFieldKey;
    }

    public String getIdentityFieldLabel() {
        return identityFieldLabel;
    }

    public void setIdentityFieldLabel(String identityFieldLabel) {
        this.identityFieldLabel = identityFieldLabel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
