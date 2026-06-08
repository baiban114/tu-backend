package com.tu.backend.index;

import com.tu.backend.common.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/index/pages")
public class PageIndexFlushController {

    private final PageIndexCoordinator pageIndexCoordinator;

    public PageIndexFlushController(PageIndexCoordinator pageIndexCoordinator) {
        this.pageIndexCoordinator = pageIndexCoordinator;
    }

    @PostMapping("/{pageId}/flush")
    public ApiResponse<Void> flushPage(@PathVariable String pageId) {
        pageIndexCoordinator.flushPage(pageId);
        return ApiResponse.success();
    }
}
