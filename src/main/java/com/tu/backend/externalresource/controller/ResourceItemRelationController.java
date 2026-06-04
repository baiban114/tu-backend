package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.externalresource.dto.CreateResourceItemRelationRequest;
import com.tu.backend.externalresource.dto.ResourceItemRelationDto;
import com.tu.backend.externalresource.service.ExternalResourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resource-item-relations")
public class ResourceItemRelationController {

    private final ExternalResourceService externalResourceService;

    public ResourceItemRelationController(ExternalResourceService externalResourceService) {
        this.externalResourceService = externalResourceService;
    }

    @GetMapping("/by-item/{itemId}")
    public ApiResponse<List<ResourceItemRelationDto>> listByItem(@PathVariable String itemId) {
        return ApiResponse.success(externalResourceService.listItemRelations(itemId));
    }

    @PostMapping
    public ApiResponse<ResourceItemRelationDto> create(@Valid @RequestBody CreateResourceItemRelationRequest request) {
        return ApiResponse.success(externalResourceService.createItemRelation(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        externalResourceService.deleteItemRelation(id);
        return ApiResponse.success();
    }
}
