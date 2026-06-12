package com.tu.backend.contenttree.search;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.contenttree.search.dto.HeadingSearchResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class HeadingSearchController {

    private final HeadingSearchService headingSearchService;

    public HeadingSearchController(HeadingSearchService headingSearchService) {
        this.headingSearchService = headingSearchService;
    }

    @GetMapping("/headings")
    public ApiResponse<HeadingSearchResponseDto> searchHeadings(
        @RequestParam("q") String query,
        @RequestParam(value = "kbId", required = false) String kbId,
        @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        return ApiResponse.success(headingSearchService.search(query, kbId, limit));
    }
}
