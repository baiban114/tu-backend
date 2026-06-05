package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.externalresource.dto.ResourceChapterDto;
import com.tu.backend.externalresource.dto.UpdateResourceChapterRequest;
import com.tu.backend.externalresource.service.ExternalResourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resource-chapters")
public class ResourceChapterController {

    private final ExternalResourceService externalResourceService;

    public ResourceChapterController(ExternalResourceService externalResourceService) {
        this.externalResourceService = externalResourceService;
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceChapterDto> get(@PathVariable String id) {
        return ApiResponse.success(externalResourceService.getChapter(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ResourceChapterDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateResourceChapterRequest request
    ) {
        return ApiResponse.success(externalResourceService.updateChapter(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        externalResourceService.deleteChapter(id);
        return ApiResponse.success();
    }
}
