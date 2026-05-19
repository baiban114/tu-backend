package com.tu.backend.knowledge.dto;

import java.util.List;

public record RoadmapNodeRequest(
    String title,
    String name,
    String description,
    String content,
    List<RoadmapNodeRequest> children
) {
}
