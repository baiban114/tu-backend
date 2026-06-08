package com.tu.backend.search;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.search.dto.SearchResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;
    private final SearchIndexService searchIndexService;

    public SearchController(SearchService searchService, SearchIndexService searchIndexService) {
        this.searchService = searchService;
        this.searchIndexService = searchIndexService;
    }

    @GetMapping
    public ApiResponse<SearchResponseDto> search(
        @RequestParam("q") String query,
        @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.search(query, limit));
    }

    @PostMapping("/reindex")
    public ApiResponse<Void> reindex() {
        searchIndexService.reindexAll();
        return ApiResponse.success();
    }
}
