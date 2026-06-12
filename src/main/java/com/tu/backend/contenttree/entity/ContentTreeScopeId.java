package com.tu.backend.contenttree.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContentTreeScopeId implements Serializable {

    @Column(name = "scope_type", length = 32, nullable = false)
    private String scopeType;

    @Column(name = "scope_id", length = 64, nullable = false)
    private String scopeId;

    public ContentTreeScopeId() {
    }

    public ContentTreeScopeId(String scopeType, String scopeId) {
        this.scopeType = scopeType;
        this.scopeId = scopeId;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContentTreeScopeId that)) {
            return false;
        }
        return Objects.equals(scopeType, that.scopeType) && Objects.equals(scopeId, that.scopeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopeType, scopeId);
    }
}
