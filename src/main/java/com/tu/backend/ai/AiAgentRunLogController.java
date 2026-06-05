package com.tu.backend.ai;

import com.tu.backend.ai.dto.AiAgentRunLogDetailDto;
import com.tu.backend.ai.dto.AiAgentRunLogSummaryDto;
import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/runs")
public class AiAgentRunLogController {

    private final AiAgentRunLogService service;

    public AiAgentRunLogController(AiAgentRunLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AiAgentRunLogSummaryDto>> list(
        @RequestParam(required = false) String taskType,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(service.list(taskType, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiAgentRunLogDetailDto> detail(@PathVariable String id) {
        return ApiResponse.success(service.detail(id));
    }
}
