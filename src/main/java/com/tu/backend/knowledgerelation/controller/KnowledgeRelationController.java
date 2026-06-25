package com.tu.backend.knowledgerelation.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.PageResponse;
import com.tu.backend.knowledgerelation.dto.CreateKnowledgeRelationRequest;
import com.tu.backend.knowledgerelation.dto.CreateRelationTypeRequest;
import com.tu.backend.knowledgerelation.dto.KnowledgeRelationDto;
import com.tu.backend.knowledgerelation.dto.RelationTypeDefDto;
import com.tu.backend.knowledgerelation.dto.RelationsByAnchorDto;
import com.tu.backend.knowledgerelation.dto.RelationsByPointDto;
import com.tu.backend.knowledgerelation.service.KnowledgeRelationService;
import com.tu.backend.knowledgerelation.service.RelationTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class KnowledgeRelationController {

    private final RelationTypeService relationTypeService;
    private final KnowledgeRelationService knowledgeRelationService;

    public KnowledgeRelationController(
        RelationTypeService relationTypeService,
        KnowledgeRelationService knowledgeRelationService
    ) {
        this.relationTypeService = relationTypeService;
        this.knowledgeRelationService = knowledgeRelationService;
    }

    @GetMapping("/kbs/{kbId}/relation-types")
    public ApiResponse<List<RelationTypeDefDto>> listRelationTypes(@PathVariable String kbId) {
        return ApiResponse.success(relationTypeService.listForKb(kbId));
    }

    @PostMapping("/kbs/{kbId}/relation-types")
    public ApiResponse<RelationTypeDefDto> createRelationType(
        @PathVariable String kbId,
        @Valid @RequestBody CreateRelationTypeRequest request
    ) {
        return ApiResponse.success(relationTypeService.createCustom(kbId, request));
    }

    @GetMapping("/kbs/{kbId}/relations")
    public ApiResponse<PageResponse<KnowledgeRelationDto>> listRelations(
        @PathVariable String kbId,
        @RequestParam(required = false) String locator,
        @RequestParam(required = false) String pointId,
        @RequestParam(required = false) String relationTypeKey,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(knowledgeRelationService.listRelations(kbId, locator, pointId, relationTypeKey, q, page, pageSize));
    }

    @GetMapping("/knowledge-points/{pointId}/relations")
    public ApiResponse<RelationsByPointDto> listByPoint(
        @PathVariable String pointId,
        @RequestParam String kbId
    ) {
        return ApiResponse.success(knowledgeRelationService.listByPoint(kbId, pointId));
    }

    @GetMapping("/kbs/{kbId}/relations/by-anchor")
    public ApiResponse<RelationsByAnchorDto> listByAnchor(
        @PathVariable String kbId,
        @RequestParam String locator
    ) {
        return ApiResponse.success(knowledgeRelationService.listByAnchor(kbId, locator));
    }

    @PostMapping("/kbs/{kbId}/relations")
    public ApiResponse<KnowledgeRelationDto> createRelation(
        @PathVariable String kbId,
        @Valid @RequestBody CreateKnowledgeRelationRequest request
    ) {
        return ApiResponse.success(knowledgeRelationService.createRelation(kbId, request));
    }

    @DeleteMapping("/relations/{id}")
    public ApiResponse<Void> deleteRelation(@PathVariable String id) {
        knowledgeRelationService.deleteRelation(id);
        return ApiResponse.success();
    }
}
