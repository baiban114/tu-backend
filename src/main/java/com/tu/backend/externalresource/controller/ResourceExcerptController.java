package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.externalresource.dto.ResourceExcerptDto;
import com.tu.backend.externalresource.dto.UpdateResourceExcerptRequest;
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
@RequestMapping("/api/resource-excerpts")
public class ResourceExcerptController {

    private final ExternalResourceService externalResourceService;

    public ResourceExcerptController(ExternalResourceService externalResourceService) {
        this.externalResourceService = externalResourceService;
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceExcerptDto> get(@PathVariable String id) {
        return ApiResponse.success(externalResourceService.getExcerpt(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ResourceExcerptDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateResourceExcerptRequest request
    ) {
        return ApiResponse.success(externalResourceService.updateExcerpt(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        externalResourceService.deleteExcerpt(id);
        return ApiResponse.success();
    }
}
