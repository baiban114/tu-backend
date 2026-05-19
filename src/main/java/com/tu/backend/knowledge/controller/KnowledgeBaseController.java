package com.tu.backend.knowledge.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.knowledge.dto.CreateKnowledgeBaseRequest;
import com.tu.backend.knowledge.dto.ImportRoadmapRequest;
import com.tu.backend.knowledge.dto.ImportRoadmapResponse;
import com.tu.backend.knowledge.dto.KnowledgeBaseDto;
import com.tu.backend.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.tu.backend.knowledge.service.KnowledgeBaseService;
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
@RequestMapping("/api/kbs")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBaseDto>> list() {
        return ApiResponse.success(knowledgeBaseService.list());
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseDto> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(request));
    }

    @PostMapping("/import-roadmap")
    public ApiResponse<ImportRoadmapResponse> importRoadmap(@RequestBody ImportRoadmapRequest request) {
        return ApiResponse.success(knowledgeBaseService.importRoadmap(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<KnowledgeBaseDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateKnowledgeBaseRequest request
    ) {
        return ApiResponse.success(knowledgeBaseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return ApiResponse.success();
    }
}
