package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resource-types")
public class ResourceTypeController {

    private final ExternalResourceService externalResourceService;

    public ResourceTypeController(ExternalResourceService externalResourceService) {
        this.externalResourceService = externalResourceService;
    }

    @GetMapping
    public ApiResponse<List<ResourceTypeDto>> list() {
        return ApiResponse.success(externalResourceService.listTypes());
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
