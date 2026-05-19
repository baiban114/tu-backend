package com.tu.backend.knowledge.dto;

import jakarta.validation.Valid;
import java.util.List;

public record ImportRoadmapRequest(
    String name,
    String icon,
    String description,
    @Valid RoadmapNodeRequest root,
    List<@Valid RoadmapNodeRequest> pages,
    Object roadmap
) {
}
