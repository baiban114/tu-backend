package com.tu.backend.knowledge.dto;

import com.tu.backend.page.dto.PageItemDto;

import java.util.List;

public record ImportRoadmapResponse(
    KnowledgeBaseDto knowledgeBase,
    List<PageItemDto> pages,
    int pageCount
) {
}
