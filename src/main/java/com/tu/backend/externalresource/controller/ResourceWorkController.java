package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.PageResponse;
import com.tu.backend.externalresource.dto.CreateResourceWorkRequest;
import com.tu.backend.externalresource.dto.MergeResourceWorksRequest;
import com.tu.backend.externalresource.dto.ResourceWorkDto;
import com.tu.backend.externalresource.dto.UpdateResourceWorkRequest;
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
@RequestMapping("/api/resource-works")
public class ResourceWorkController {

    private final ExternalResourceService externalResourceService;

    public ResourceWorkController(ExternalResourceService externalResourceService) {
        this.externalResourceService = externalResourceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceWorkDto>> list(
        @RequestParam(required = false) String typeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(externalResourceService.listWorks(typeId, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ResourceWorkDto> create(@Valid @RequestBody CreateResourceWorkRequest request) {
        return ApiResponse.success(externalResourceService.createWork(request));
    }

    @PostMapping("/merge")
    public ApiResponse<ResourceWorkDto> merge(@Valid @RequestBody MergeResourceWorksRequest request) {
        return ApiResponse.success(externalResourceService.mergeWorks(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ResourceWorkDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateResourceWorkRequest request
    ) {
        return ApiResponse.success(externalResourceService.updateWork(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        externalResourceService.deleteWork(id);
        return ApiResponse.success();
    }
}
