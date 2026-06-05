package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.PageResponse;
import com.tu.backend.externalresource.dto.CreateResourceTypeRequest;
import com.tu.backend.externalresource.dto.ResourceTypeDto;
import com.tu.backend.externalresource.dto.UpdateResourceTypeRequest;
import com.tu.backend.externalresource.service.ExternalResourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resource-types")
public class ResourceTypeController {

    private final ExternalResourceService externalResourceService;

    public ResourceTypeController(ExternalResourceService externalResourceService) {
        this.externalResourceService = externalResourceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceTypeDto>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(externalResourceService.listTypes(page, pageSize));
    }

    @PostMapping
    public ApiResponse<ResourceTypeDto> create(@Valid @RequestBody CreateResourceTypeRequest request) {
        return ApiResponse.success(externalResourceService.createType(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ResourceTypeDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateResourceTypeRequest request
    ) {
        return ApiResponse.success(externalResourceService.updateType(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        externalResourceService.deleteType(id);
        return ApiResponse.success();
    }
}
