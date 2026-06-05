package com.tu.backend.externalresource.controller;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.PageResponse;
import com.tu.backend.externalresource.dto.CreateResourceChapterRequest;
import com.tu.backend.externalresource.dto.CreateResourceExcerptRequest;
import com.tu.backend.externalresource.dto.CreateResourceItemRequest;
import com.tu.backend.externalresource.dto.RegisterResourceUrlRequest;
import com.tu.backend.externalresource.dto.RegisterResourceUrlResult;
import com.tu.backend.externalresource.dto.ResourceChapterDto;
import com.tu.backend.externalresource.dto.ResourceExcerptDto;
import com.tu.backend.externalresource.dto.ResourceItemDto;
import com.tu.backend.externalresource.dto.UpdateResourceItemRequest;
import com.tu.backend.externalresource.service.ExternalResourceService;
import com.tu.backend.externalresource.service.WebLinkRegistrationService;
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
    private final WebLinkRegistrationService webLinkRegistrationService;

    public ResourceItemController(
        ExternalResourceService externalResourceService,
        WebLinkRegistrationService webLinkRegistrationService
    ) {
        this.externalResourceService = externalResourceService;
        this.webLinkRegistrationService = webLinkRegistrationService;
    }

    @PostMapping("/register-from-url")
    public ApiResponse<RegisterResourceUrlResult> registerFromUrl(@Valid @RequestBody RegisterResourceUrlRequest request) {
        return ApiResponse.success(webLinkRegistrationService.registerFromUrl(request));
    }

    @GetMapping("/fetch-page-title")
    public ApiResponse<String> fetchPageTitle(@RequestParam String url) {
        return ApiResponse.success(
            webLinkRegistrationService.fetchPageTitle(url).orElse(null)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceItemDto>> list(
        @RequestParam(required = false) String typeId,
        @RequestParam(required = false) String workId,
        @RequestParam(required = false) String identityValue,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(externalResourceService.listItems(typeId, workId, identityValue, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceItemDto> get(@PathVariable String id) {
        return ApiResponse.success(externalResourceService.getItem(id));
    }

    @PostMapping
    public ApiResponse<ResourceItemDto> create(@Valid @RequestBody CreateResourceItemRequest request) {
        return ApiResponse.success(externalResourceService.createItem(request));
    }

    @GetMapping("/{id}/chapters")
    public ApiResponse<List<ResourceChapterDto>> listChapters(@PathVariable String id) {
        return ApiResponse.success(externalResourceService.listChapters(id));
    }

    @PostMapping("/{id}/chapters")
    public ApiResponse<ResourceChapterDto> createChapter(
        @PathVariable String id,
        @Valid @RequestBody CreateResourceChapterRequest request
    ) {
        return ApiResponse.success(externalResourceService.createChapter(id, request));
    }

    @GetMapping("/{id}/excerpts")
    public ApiResponse<PageResponse<ResourceExcerptDto>> listExcerpts(
        @PathVariable String id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(externalResourceService.listExcerpts(id, page, pageSize));
    }

    @PostMapping("/{id}/excerpts")
    public ApiResponse<ResourceExcerptDto> createExcerpt(
        @PathVariable String id,
        @Valid @RequestBody CreateResourceExcerptRequest request
    ) {
        return ApiResponse.success(externalResourceService.createExcerpt(id, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ResourceItemDto> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateResourceItemRequest request
    ) {
        return ApiResponse.success(externalResourceService.updateItem(id, request));
    }

    @PostMapping("/{id}/split-work")
    public ApiResponse<ResourceItemDto> splitToNewWork(@PathVariable String id) {
        return ApiResponse.success(externalResourceService.splitItemToNewWork(id));
    }

    @PostMapping("/{id}/reset-auto")
    public ApiResponse<ResourceItemDto> resetAuto(@PathVariable String id) {
        return ApiResponse.success(externalResourceService.resetItemAutoFields(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable String id) {
        externalResourceService.removeItem(id);
        return ApiResponse.success();
    }
}
