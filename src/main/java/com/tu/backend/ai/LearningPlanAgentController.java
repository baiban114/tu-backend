package com.tu.backend.ai;

import com.tu.backend.ai.dto.GenerateLearningPlanRequest;
import com.tu.backend.ai.dto.LearningPlanResponseDto;
import com.tu.backend.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/learning-plan")
public class LearningPlanAgentController {

    private final LearningPlanAgentService service;

    public LearningPlanAgentController(LearningPlanAgentService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public ApiResponse<LearningPlanResponseDto> generate(@Valid @RequestBody GenerateLearningPlanRequest request) {
        return ApiResponse.success(service.generate(request));
    }
}
