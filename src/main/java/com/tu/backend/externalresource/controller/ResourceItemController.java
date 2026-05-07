package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.externalresource.dto.CreateResourceItemRequest;
import com.tu.backend.externalresource.dto.ResourceItemDto;
import com.tu.backend.externalresource.dto.UpdateResourceItemRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api/resource-items")
public class ResourceItemController {

    private final ExternalResourceService externalResourceService;

    public ResourceItemController(ExternalResourceService externalResourceService) {
        this.externalResourceService = externalResourceService;
    }

    @GetMapping
    public ApiResponse<List<ResourceItemDto>> list(
        @RequestParam(required = false) String typeId,
        @RequestParam(required = false) String workId,
        @RequestParam(required = false) String identityValue
    ) {
        return ApiResponse.success(externalResourceService.listItems(typeId, workId, identityValue));
    }

    @PostMapping
    public ApiResponse<ResourceItemDto> create(@Valid @RequestBody CreateResourceItemRequest request) {
        return ApiResponse.success(externalResourceService.createItem(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ResourceItemDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateResourceItemRequest request
    ) {
        return ApiResponse.success(externalResourceService.updateItem(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        externalResourceService.deleteItem(id);
        return ApiResponse.success();
    }
}
