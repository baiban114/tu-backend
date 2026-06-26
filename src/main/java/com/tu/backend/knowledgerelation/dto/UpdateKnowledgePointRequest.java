package com.tu.backend.knowledgerelation.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Size;

public class UpdateKnowledgePointRequest {

    @Size(max = 64)
    private String parentId;

    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String summary;

    @Size(max = 16)
    private String status;

    private Double estimatedHours;
    private Integer sortOrder;

    private boolean parentIdPresent;
    private boolean titlePresent;
    private boolean summaryPresent;
    private boolean statusPresent;
    private boolean estimatedHoursPresent;
    private boolean sortOrderPresent;

    public String getParentId() {
        return parentId;
    }

    @JsonSetter("parentId")
    public void setParentId(String parentId) {
        this.parentId = parentId;
        this.parentIdPresent = true;
    }

    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public String getSummary() {
        return summary;
    }

    @JsonSetter("summary")
    public void setSummary(String summary) {
        this.summary = summary;
        this.summaryPresent = true;
    }

    public String getStatus() {
        return status;
    }

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = status;
        this.statusPresent = true;
    }

    public Double getEstimatedHours() {
        return estimatedHours;
    }

    @JsonSetter("estimatedHours")
    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
        this.estimatedHoursPresent = true;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    @JsonSetter("sortOrder")
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        this.sortOrderPresent = true;
    }

    public boolean isParentIdPresent() {
        return parentIdPresent;
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }

    public boolean isSummaryPresent() {
        return summaryPresent;
    }

    public boolean isStatusPresent() {
        return statusPresent;
    }

    public boolean isEstimatedHoursPresent() {
        return estimatedHoursPresent;
    }

    public boolean isSortOrderPresent() {
        return sortOrderPresent;
    }
}
