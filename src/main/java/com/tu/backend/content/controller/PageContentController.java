package com.tu.backend.content.controller;

import com.tu.backend.block.service.BlockService;
import com.tu.backend.common.ApiResponse;
import com.tu.backend.content.dto.PageContentDto;
import com.tu.backend.content.dto.SavePageContentRequest;
import com.tu.backend.content.service.PageContentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pages")
public class PageContentController {

    private final PageContentService pageContentService;
    private final BlockService blockService;

    public PageContentController(PageContentService pageContentService, BlockService blockService) {
        this.pageContentService = pageContentService;
        this.blockService = blockService;
    }

    @GetMapping("/{id}/content")
    public ApiResponse<PageContentDto> getContent(@PathVariable("id") String pageId) {
        return ApiResponse.success(pageContentService.getContent(pageId));
    }

    @PutMapping("/{id}/content")
    public ApiResponse<PageContentDto> saveContent(
        @PathVariable("id") String pageId,
        @Valid @RequestBody SavePageContentRequest request
    ) {
        return ApiResponse.success(pageContentService.saveContent(pageId, request));
    }

    @DeleteMapping("/{pageId}/blocks/{blockId}/annotations/{annotationId}")
    public ApiResponse<Void> deleteAnnotation(
        @PathVariable String pageId,
        @PathVariable String blockId,
        @PathVariable String annotationId
    ) {
        blockService.deleteAnnotation(pageId, blockId, annotationId);
        return ApiResponse.success();
    }
}

