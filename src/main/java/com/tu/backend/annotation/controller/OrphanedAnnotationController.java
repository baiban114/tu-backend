package com.tu.backend.annotation.controller;

import com.tu.backend.annotation.dto.OrphanedAnnotationDto;
import com.tu.backend.annotation.service.OrphanedAnnotationService;
import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.PageResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orphaned-annotations")
public class OrphanedAnnotationController {

    private final OrphanedAnnotationService orphanedAnnotationService;

    public OrphanedAnnotationController(OrphanedAnnotationService orphanedAnnotationService) {
        this.orphanedAnnotationService = orphanedAnnotationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrphanedAnnotationDto>> listOrphaned(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(orphanedAnnotationService.listOrphaned(page, pageSize));
    }

    @GetMapping("/count")
    public ApiResponse<Long> countOrphaned() {
        return ApiResponse.success(orphanedAnnotationService.countOrphaned());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrphaned(@PathVariable String id) {
        orphanedAnnotationService.deleteOrphaned(id);
        return ApiResponse.success();
    }

    @PostMapping("/clear")
    public ApiResponse<Integer> clearAllOrphaned() {
        return ApiResponse.success(orphanedAnnotationService.clearAllOrphaned());
    }
}
