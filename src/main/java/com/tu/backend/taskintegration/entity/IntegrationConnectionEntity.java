package com.tu.backend.taskintegration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "integration_connection")
public class IntegrationConnectionEntity {

    @Id
    @Column(name = "provider", length = 64, nullable = false)
    private String id;

    @Column(name = "provider_code", length = 64)
    private String provider;

    @Column(name = "base_url", length = 1024, nullable = false)
    private String baseUrl;

    @Column(name = "api_key", length = 2048)
    private String apiKey;

    @Column(name = "workspace_id", length = 128)
    private String workspaceId;

    @Lob
    @Column(name = "adapter_profile_json", columnDefinition = "text")
    private String adapterProfileJson;

    @Column(nullable = false)
    private boolean enabled;

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getAdapterProfileJson() {
        return adapterProfileJson;
    }

    public void setAdapterProfileJson(String adapterProfileJson) {
        this.adapterProfileJson = adapterProfileJson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
