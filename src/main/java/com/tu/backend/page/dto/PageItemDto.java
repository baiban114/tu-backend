package com.tu.backend.page.dto;

import java.util.ArrayList;
import java.util.List;

public class PageItemDto {

    private String id;
    private String kbId;
    private String parentId;
    private String title;
    private Integer order;
    private List<PageItemDto> children = new ArrayList<>();

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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public List<PageItemDto> getChildren() {
        return children;
    }

    public void setChildren(List<PageItemDto> children) {
        this.children = children;
    }
}

