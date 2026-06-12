package com.tu.backend.contenttree.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.contenttree.dto.BlockOutlineResponseDto;
import com.tu.backend.contenttree.dto.OutlineBatchRequestDto;
import com.tu.backend.contenttree.dto.OutlineBatchResponseDto;
import com.tu.backend.contenttree.dto.PageOutlineResponseDto;
import com.tu.backend.contenttree.service.ContentTreeIndexService;
import com.tu.backend.contenttree.service.ContentTreeNodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OutlineController {

    private final ContentTreeNodeService contentTreeNodeService;
    private final ContentTreeIndexService contentTreeIndexService;

    public OutlineController(
        ContentTreeNodeService contentTreeNodeService,
        ContentTreeIndexService contentTreeIndexService
    ) {
        this.contentTreeNodeService = contentTreeNodeService;
        this.contentTreeIndexService = contentTreeIndexService;
    }

    @GetMapping("/pages/{pageId}/outline")
    public ApiResponse<PageOutlineResponseDto> getPageOutline(@PathVariable String pageId) {
        return ApiResponse.success(contentTreeNodeService.getPageOutline(pageId));
    }

    @GetMapping("/blocks/{blockId}/outline")
    public ApiResponse<BlockOutlineResponseDto> getBlockOutline(@PathVariable String blockId) {
        return ApiResponse.success(contentTreeNodeService.getBlockOutline(blockId));
    }

    @PostMapping("/outlines/batch")
    public ApiResponse<OutlineBatchResponseDto> batchOutline(@RequestBody OutlineBatchRequestDto request) {
        return ApiResponse.success(contentTreeNodeService.batchOutline(request));
    }

    @PostMapping("/content-tree/reindex")
    public ApiResponse<Void> reindex() {
        contentTreeIndexService.reindexAll();
        return ApiResponse.success();
    }
}
