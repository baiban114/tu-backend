package com.tu.backend.externalresource.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_resource_url_cluster_rule")
public class UrlClusterRuleEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 255, nullable = false)
    private String domain;

    @Column(name = "path_regex", length = 512, nullable = false)
    private String pathRegex;

    @Column(name = "cluster_key_format", length = 512, nullable = false)
    private String clusterKeyFormat;

    @Column(name = "variant_group")
    private Integer variantGroup;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    @Column(length = 512)
    private String description;

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPathRegex() {
        return pathRegex;
    }

    public void setPathRegex(String pathRegex) {
        this.pathRegex = pathRegex;
    }

    public String getClusterKeyFormat() {
        return clusterKeyFormat;
    }

    public void setClusterKeyFormat(String clusterKeyFormat) {
        this.clusterKeyFormat = clusterKeyFormat;
    }

    public Integer getVariantGroup() {
        return variantGroup;
    }

    public void setVariantGroup(Integer variantGroup) {
        this.variantGroup = variantGroup;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
