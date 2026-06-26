package com.tu.backend.knowledgerelation.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.PageResponse;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgePointAliasRequest;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgePointAnchorRequest;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgePointRequest;
import com.tu.backend.knowledgerelation.dto.GenerateKnowledgePointsRequest;
import com.tu.backend.knowledgerelation.dto.KnowledgePointAliasDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointAnchorDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointDto;
import com.tu.backend.knowledgerelation.dto.KnowledgePointGenerationResultDto;
import com.tu.backend.knowledgerelation.dto.UpdateKnowledgePointRequest;
import com.tu.backend.knowledgerelation.service.KnowledgePointGenerationService;
import com.tu.backend.knowledgerelation.service.KnowledgePointService;
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
@RequestMapping("/api")
public class KnowledgePointController {

    private final KnowledgePointService knowledgePointService;
    private final KnowledgePointGenerationService knowledgePointGenerationService;

    public KnowledgePointController(
        KnowledgePointService knowledgePointService,
        KnowledgePointGenerationService knowledgePointGenerationService
    ) {
        this.knowledgePointService = knowledgePointService;
        this.knowledgePointGenerationService = knowledgePointGenerationService;
    }

    @GetMapping("/kbs/{kbId}/knowledge-points/tree")
    public ApiResponse<List<KnowledgePointDto>> listTree(@PathVariable String kbId) {
        return ApiResponse.success(knowledgePointService.listTree(kbId));
    }

    @GetMapping("/kbs/{kbId}/knowledge-points")
    public ApiResponse<PageResponse<KnowledgePointDto>> listPoints(
        @PathVariable String kbId,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(knowledgePointService.listPoints(kbId, q, page, pageSize));
    }

    @GetMapping("/kbs/{kbId}/knowledge-points/by-locator")
    public ApiResponse<List<KnowledgePointDto>> listByLocator(
        @PathVariable String kbId,
        @RequestParam String locator
    ) {
        return ApiResponse.success(knowledgePointService.findPointsByLocator(kbId, locator));
    }

    @PostMapping("/kbs/{kbId}/knowledge-points/generate")
    public ApiResponse<KnowledgePointGenerationResultDto> generatePoints(
        @PathVariable String kbId,
        @Valid @RequestBody GenerateKnowledgePointsRequest request
    ) {
        return ApiResponse.success(knowledgePointGenerationService.generate(kbId, request));
    }

    @GetMapping("/knowledge-points/{id}")
    public ApiResponse<KnowledgePointDto> getPoint(@PathVariable String id) {
        return ApiResponse.success(knowledgePointService.getPoint(id));
    }

    @PostMapping("/kbs/{kbId}/knowledge-points")
    public ApiResponse<KnowledgePointDto> createPoint(
        @PathVariable String kbId,
        @Valid @RequestBody CreateKnowledgePointRequest request
    ) {
        return ApiResponse.success(knowledgePointService.createPoint(kbId, request));
    }

    @PatchMapping("/knowledge-points/{id}")
    public ApiResponse<KnowledgePointDto> updatePoint(
        @PathVariable String id,
        @Valid @RequestBody UpdateKnowledgePointRequest request
    ) {
        return ApiResponse.success(knowledgePointService.updatePoint(id, request));
    }

    @DeleteMapping("/knowledge-points/{id}")
    public ApiResponse<Void> deletePoint(@PathVariable String id) {
        knowledgePointService.deletePoint(id);
        return ApiResponse.success();
    }

    @GetMapping("/knowledge-points/{id}/anchors")
    public ApiResponse<List<KnowledgePointAnchorDto>> listAnchors(@PathVariable String id) {
        return ApiResponse.success(knowledgePointService.listAnchors(id));
    }

    @PostMapping("/knowledge-points/{id}/anchors")
    public ApiResponse<KnowledgePointAnchorDto> addAnchor(
        @PathVariable String id,
        @Valid @RequestBody CreateKnowledgePointAnchorRequest request
    ) {
        return ApiResponse.success(knowledgePointService.addAnchor(id, request));
    }

    @DeleteMapping("/knowledge-point-anchors/{anchorId}")
    public ApiResponse<Void> deleteAnchor(@PathVariable String anchorId) {
        knowledgePointService.deleteAnchor(anchorId);
        return ApiResponse.success();
    }

    @GetMapping("/knowledge-points/{id}/aliases")
    public ApiResponse<List<KnowledgePointAliasDto>> listAliases(@PathVariable String id) {
        return ApiResponse.success(knowledgePointService.listAliases(id));
    }

    @PostMapping("/knowledge-points/{id}/aliases")
    public ApiResponse<KnowledgePointAliasDto> addAlias(
        @PathVariable String id,
        @Valid @RequestBody CreateKnowledgePointAliasRequest request
    ) {
        return ApiResponse.success(knowledgePointService.addAlias(id, request));
    }

    @DeleteMapping("/knowledge-point-aliases/{aliasId}")
    public ApiResponse<Void> deleteAlias(@PathVariable String aliasId) {
        knowledgePointService.deleteAlias(aliasId);
        return ApiResponse.success();
    }
}
