package com.tu.backend.content.dto;

import java.util.List;
import java.util.Map;

public record PageContentDto(
    String pageId,
    String content,
    List<Object> embeds,
    List<Object> annotations,
    Map<String, Object> metadata,
    List<Object> blocks,
    Object document,
    Integer schemaVersion
) {
}
