package com.tu.backend.page.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.page.dto.CreatePageRequest;
import com.tu.backend.page.dto.PageItemDto;
import com.tu.backend.page.dto.UpdatePageRequest;
import com.tu.backend.page.service.PageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/kbs/{kbId}/pages/tree")
    public ApiResponse<List<PageItemDto>> getTree(@PathVariable String kbId) {
        return ApiResponse.success(pageService.getTree(kbId));
    }

    @PostMapping("/pages")
    public ApiResponse<PageItemDto> create(@Valid @RequestBody CreatePageRequest request) {
        return ApiResponse.success(pageService.create(request));
    }

    @PatchMapping("/pages/{id}")
    public ApiResponse<PageItemDto> update(
        @PathVariable String id,
        @RequestBody UpdatePageRequest request
    ) {
        return ApiResponse.success(pageService.update(id, request));
    }

    @DeleteMapping("/pages/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        pageService.delete(id);
        return ApiResponse.success();
    }
}

