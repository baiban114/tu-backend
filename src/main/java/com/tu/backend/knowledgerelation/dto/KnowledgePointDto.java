package com.tu.backend.knowledgerelation.dto;

import java.util.ArrayList;
import java.util.List;

public class KnowledgePointDto {

    private String id;
    private String kbId;
    private String parentId;
    private String title;
    private String summary;
    private String status;
    private Double estimatedHours;
    private int sortOrder;
    private List<String> aliases = new ArrayList<>();
    private List<KnowledgePointDto> children = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases == null ? new ArrayList<>() : aliases;
    }

    public List<KnowledgePointDto> getChildren() {
        return children;
    }

    public void setChildren(List<KnowledgePointDto> children) {
        this.children = children;
    }
}
