package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.externalresource.dto.CreateUrlClusterRuleRequest;
import com.tu.backend.externalresource.dto.UpdateUrlClusterRuleRequest;
import com.tu.backend.externalresource.dto.UrlClusterRuleDto;
import com.tu.backend.externalresource.service.UrlClusterRuleService;
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
@RequestMapping("/api/url-cluster-rules")
public class UrlClusterRuleController {

    private final UrlClusterRuleService urlClusterRuleService;

    public UrlClusterRuleController(UrlClusterRuleService urlClusterRuleService) {
        this.urlClusterRuleService = urlClusterRuleService;
    }

    @GetMapping
    public ApiResponse<List<UrlClusterRuleDto>> list() {
        return ApiResponse.success(urlClusterRuleService.listRules());
    }

    @PostMapping
    public ApiResponse<UrlClusterRuleDto> create(@Valid @RequestBody CreateUrlClusterRuleRequest request) {
        return ApiResponse.success(urlClusterRuleService.createRule(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<UrlClusterRuleDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateUrlClusterRuleRequest request
    ) {
        return ApiResponse.success(urlClusterRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        urlClusterRuleService.deleteRule(id);
        return ApiResponse.success();
    }
}
