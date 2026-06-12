package com.tu.backend.contenttree.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.contenttree.dto.ContentTreeNodeDto;
import com.tu.backend.contenttree.dto.UpdateContentTreeNodeRequestDto;
import com.tu.backend.contenttree.service.ContentTreeNodeService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content-tree-nodes")
public class ContentTreeNodeController {

    private final ContentTreeNodeService contentTreeNodeService;

    public ContentTreeNodeController(ContentTreeNodeService contentTreeNodeService) {
        this.contentTreeNodeService = contentTreeNodeService;
    }

    @PatchMapping("/{id}")
    public ApiResponse<ContentTreeNodeDto> updateEstimatedHours(
        @PathVariable String id,
        @RequestBody UpdateContentTreeNodeRequestDto request
    ) {
        return ApiResponse.success(contentTreeNodeService.updateEstimatedHours(id, request));
    }
}
