package com.tu.backend.reference.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.reference.dto.ReferenceItemDto;
import com.tu.backend.reference.dto.UpdateExternalReferenceRequest;
import com.tu.backend.reference.service.ReferenceService;
import jakarta.validation.Valid;
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
@RequestMapping("/api")
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/references")
    public ApiResponse<List<ReferenceItemDto>> listReferences(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String pageId,
        @RequestParam(required = false) String resourceItemId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String q
    ) {
        return ApiResponse.success(referenceService.listReferences(category, pageId, resourceItemId, status, q));
    }

    @PatchMapping("/external-references/{id}")
    public ApiResponse<ReferenceItemDto> updateExternalReference(
        @PathVariable String id,
        @Valid @RequestBody UpdateExternalReferenceRequest request
    ) {
        return ApiResponse.success(referenceService.updateExternalReference(id, request));
    }

    @PostMapping("/references/rebuild")
    public ApiResponse<Void> rebuildReferences() {
        referenceService.rebuildAll();
        return ApiResponse.success();
    }
}
