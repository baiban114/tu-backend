package com.tu.backend.rag;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.index.PageIndexCoordinator;
import com.tu.backend.rag.dto.RagQueryRequest;
import com.tu.backend.rag.dto.RagQueryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagIndexService ragIndexService;
    private final PageIndexCoordinator pageIndexCoordinator;

    public RagController(RagIndexService ragIndexService, PageIndexCoordinator pageIndexCoordinator) {
        this.ragIndexService = ragIndexService;
        this.pageIndexCoordinator = pageIndexCoordinator;
    }

    @PostMapping("/query")
    public ApiResponse<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request) {
        return ApiResponse.success(ragIndexService.query(request));
    }

    @PostMapping("/reindex/page/{pageId}")
    public ApiResponse<Void> reindexPage(@PathVariable String pageId) {
        pageIndexCoordinator.indexPageNow(pageId);
        return ApiResponse.success();
    }

    @PostMapping("/reindex/kb/{kbId}")
    public ApiResponse<Void> reindexKnowledgeBase(@PathVariable String kbId) {
        pageIndexCoordinator.reindexKnowledgeBaseNow(kbId);
        return ApiResponse.success();
    }
}
