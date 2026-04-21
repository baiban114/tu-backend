package com.tu.backend.page.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

public class UpdatePageRequest {

    private String parentId;
    private Integer order;
    private String title;
    private boolean parentIdPresent;
    private boolean orderPresent;
    private boolean titlePresent;

    public String getParentId() {
        return parentId;
    }

    @JsonSetter("parentId")
    public void setParentId(String parentId) {
        this.parentId = parentId;
        this.parentIdPresent = true;
    }

    public Integer getOrder() {
        return order;
    }

    @JsonSetter("order")
    public void setOrder(Integer order) {
        this.order = order;
        this.orderPresent = true;
    }

    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public boolean isParentIdPresent() {
        return parentIdPresent;
    }

    public boolean isOrderPresent() {
        return orderPresent;
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }
}

